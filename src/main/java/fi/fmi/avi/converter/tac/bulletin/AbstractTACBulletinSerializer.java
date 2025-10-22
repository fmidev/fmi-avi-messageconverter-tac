package fi.fmi.avi.converter.tac.bulletin;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.*;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.List;

public abstract class AbstractTACBulletinSerializer<S extends AviationWeatherMessage, T extends MeteorologicalBulletin<S>> extends AbstractTACSerializer<T> {

    /**
     * Maximum number of characters per line (inclusive).
     */
    public static final int MAX_ROW_LENGTH = 59;

    @Override
    public ConversionResult<String> convertMessage(final T input, final ConversionHints hints) {
        final ConversionResult<String> result = new ConversionResult<>();
        try {
            final LexemeSequence seq = tokenizeMessage(input, hints);
            result.setConvertedMessage(seq.getTAC());
        } catch (final SerializingException se) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, se.getMessage()));
        }
        return result;
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    protected abstract T accepts(AviationWeatherMessageOrCollection message) throws SerializingException;

    protected abstract Class<T> getBulletinClass();

    protected abstract LexemeSequence tokenizeSingleMessage(S message, ConversionHints hints) throws SerializingException;

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        final T input = accepts(msg);
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<T> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        appendToken(retval, LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        appendToken(retval, LexemeIdentity.ISSUE_TIME, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        if (appendToken(retval, LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR, input, getBulletinClass(), baseCtx) == 0) {
            retval.removeLast();
        }

        final boolean whitespacePassthrough = hints != null && ConversionHints.VALUE_WHITESPACE_SERIALIZATION_MODE_PASSTHROUGH.equals(
                hints.getOrDefault(ConversionHints.KEY_WHITESPACE_SERIALIZATION_MODE, null));
        final boolean disableLineWrap = hints != null && ConversionHints.VALUE_DISABLE_LINEWRAP_SERIALIZATION_MODE.equals(
                hints.getOrDefault(ConversionHints.KEY_DISABLE_LINEWRAP_SERIALIZATION_MODE, null));

        final List<S> messages = input.getMessages();
        LexemeSequence messageSequence;
        if (messages.size() > 0) {
            for (final S message : messages) {
                final Layout layout;
                if (whitespacePassthrough) {
                    layout = Layout.WHITESPACE_PASSTHROUGH;
                } else if (isAdvisory(message)) {
                    layout = Layout.ADVISORY;
                } else {
                    layout = Layout.STANDARD;
                }
                final int lineWrapIndent = layout.lineWrapIndent(hints);
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN, 2);
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
                messageSequence = tokenizeSingleMessage(message, hints);

                int charsOnRow = 0;
                final List<Lexeme> lexemes = messageSequence.getLexemes();
                for (final Lexeme lexeme : lexemes) {
                    final int tokenLength = lexeme.getTACToken().length();
                    if (layout == Layout.WHITESPACE_PASSTHROUGH || layout == Layout.ADVISORY) {
                        if (!LexemeIdentity.END_TOKEN.equals(lexeme.getIdentity())) {
                            //Append CR before an LF if the CR was not already added:
                            if (isSpecialCharacterLexeme(lexeme, MeteorologicalBulletinSpecialCharacter.LINE_FEED)) {
                                if (retval.getLast()//
                                        .map(last -> !isSpecialCharacterLexeme(last, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN))//
                                        .orElse(false)) {
                                    appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
                                }
                                charsOnRow = 0;
                            } else if (!disableLineWrap && charsOnRow + tokenLength > MAX_ROW_LENGTH) {
                                while (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                                    retval.removeLast();
                                }
                                appendLineWrap(retval, lineWrapIndent);
                                charsOnRow = lineWrapIndent + tokenLength;
                            } else {
                                charsOnRow += tokenLength;
                            }
                            retval.append(lexeme);
                        }
                    } else {
                        if (!LexemeIdentity.WHITE_SPACE.equals(lexeme.getIdentity()) && !LexemeIdentity.END_TOKEN.equals(lexeme.getIdentity())) {
                            if (!disableLineWrap && charsOnRow + tokenLength > MAX_ROW_LENGTH) {
                                if (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                                    retval.removeLast();
                                }
                                appendLineWrap(retval, lineWrapIndent);
                                charsOnRow = lineWrapIndent;
                            }
                            retval.append(lexeme);
                            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                            charsOnRow += tokenLength + 1;
                        }
                    }
                }
                //Remove any trailing whitespaces:
                while (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                    retval.removeLast();
                }
                //..and make sure '=' is the last character:
                retval.append(this.getLexingFactory().createLexeme("=", LexemeIdentity.END_TOKEN));

            }
        }
        return retval.build();
    }

    private boolean isAdvisory(final S message) {
        return SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(message.getClass())
                || SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(message.getClass());
    }

    private boolean isSpecialCharacterLexeme(final Lexeme lexeme, final MeteorologicalBulletinSpecialCharacter specialCharacter) {
        return LexemeIdentity.WHITE_SPACE.equals(lexeme.getIdentity()) //
                && lexeme.getParsedValues().containsKey(Lexeme.ParsedValueName.TYPE) //
                && lexeme.getParsedValue(Lexeme.ParsedValueName.TYPE, MeteorologicalBulletinSpecialCharacter.class).equals(specialCharacter);
    }

    private void appendLineWrap(final LexemeSequenceBuilder builder, final int indentLength) {
        appendWhitespace(builder, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
        appendWhitespace(builder, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        appendWhitespace(builder, MeteorologicalBulletinSpecialCharacter.SPACE, indentLength);
    }

    private enum Layout {
        STANDARD(5), //
        WHITESPACE_PASSTHROUGH(0), //
        ADVISORY(0);

        private final int defaultIndentation;

        Layout(final int defaultIndentation) {
            this.defaultIndentation = defaultIndentation;
        }

        int lineWrapIndent(final ConversionHints conversionHints) {
            if (conversionHints != null) {
                final Integer indent = conversionHints.tryGet(ConversionHints.KEY_INDENT_ON_LINE_WRAP, Integer.class);
                if (indent != null) {
                    return indent;
                }
            }
            return getDefaultIndentation();
        }

        int getDefaultIndentation() {
            return defaultIndentation;
        }
    }
}
