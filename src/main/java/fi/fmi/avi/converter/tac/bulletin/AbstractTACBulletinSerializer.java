package fi.fmi.avi.converter.tac.bulletin;

import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;

public abstract class AbstractTACBulletinSerializer<S extends AviationWeatherMessage, T extends MeteorologicalBulletin<S>> extends AbstractTACSerializer<T> {

    public static final int MAX_ROW_LENGTH = 60;

    private static final int LINE_INDENTATION = 5;

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

    protected abstract T accepts(final AviationWeatherMessageOrCollection message) throws SerializingException;

    protected abstract Class<T> getBulletinClass();

    protected abstract LexemeSequence tokenizeSingleMessage(final S message, final ConversionHints hints) throws SerializingException;

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        final T input = accepts(msg);
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<T> baseCtx = new ReconstructorContext<>(input, hints);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN, 2);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        appendToken(retval, LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        appendToken(retval, LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        appendToken(retval, LexemeIdentity.ISSUE_TIME, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        if (appendToken(retval, LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR, input, getBulletinClass(), baseCtx) == 0) {
            retval.removeLast();
        }
        final boolean whitespacePassthrough = hints != null && ConversionHints.VALUE_WHITESPACE_SERIALIZATION_MODE_PASSTHROUGH.equals(
                hints.getOrDefault(ConversionHints.KEY_WHITESPACE_SERIALIZATION_MODE, null));
        final List<S> messages = input.getMessages();
        LexemeSequence messageSequence;
        if (messages.size() > 0) {
            for (final S message : messages) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN, 2);
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
                messageSequence = tokenizeSingleMessage(message, hints);
                int charsOnRow = 0;
                final List<Lexeme> lexemes = messageSequence.getLexemes();
                for (final Lexeme l : lexemes) {
                    final int tokenLength = l.getTACToken().length();
                    if (whitespacePassthrough) {
                        if (!LexemeIdentity.END_TOKEN.equals(l.getIdentity())) {
                            if (LexemeIdentity.WHITE_SPACE.equals(l.getIdentity()) && l.getParsedValues().containsKey(Lexeme.ParsedValueName.TYPE)
                                    && l.getParsedValue(Lexeme.ParsedValueName.TYPE, Lexeme.MeteorologicalBulletinSpecialCharacter.class)
                                    .equals(Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED)) {
                                charsOnRow = 0;
                            } else if (charsOnRow + tokenLength >= MAX_ROW_LENGTH) {
                                if (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                                    retval.removeLast();
                                }
                                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
                                charsOnRow = 0;
                            } else {
                                charsOnRow += tokenLength;
                            }
                            retval.append(l);
                        }
                    } else {
                        if (!LexemeIdentity.WHITE_SPACE.equals(l.getIdentity()) && !LexemeIdentity.END_TOKEN.equals(l.getIdentity())) {
                            if (charsOnRow + tokenLength >= MAX_ROW_LENGTH) {
                                if (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                                    retval.removeLast();
                                }
                                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
                                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE, LINE_INDENTATION);
                                charsOnRow = 1;
                            }
                            retval.append(l);
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                            charsOnRow += tokenLength + 1;
                        }
                    }
                }
                //Remove trailing whitespace:
                while (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                    retval.removeLast();
                }
                //..and make sure '=' is the last character:
                retval.append(this.getLexingFactory().createLexeme("=", LexemeIdentity.END_TOKEN));
            }
        }
        return retval.build();
    }

}
