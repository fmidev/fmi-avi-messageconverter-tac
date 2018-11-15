package fi.fmi.avi.converter.tac;

import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.MeteorologicalBulletin;

public abstract class AbstractTACBulletinSerializer<U extends BulletinHeading, S extends AviationWeatherMessage, T extends MeteorologicalBulletin<S, U>>
        extends AbstractTACSerializer<T> {

    public static final int MAX_ROW_LENGTH = 60;
    public static final int WRAPPED_LINE_INDENT = 5;
    public static final CharSequence NEW_LINE = "\r\n";

    @Override
    public ConversionResult<String> convertMessage(final T input, final ConversionHints hints) {
        ConversionResult<String> result = new ConversionResult<>();
        try {
            LexemeSequence seq = tokenizeMessage(input, hints);
            result.setConvertedMessage(seq.getTAC());
        } catch (SerializingException se) {
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
        T input = accepts(msg);
        LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        ReconstructorContext<T> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, ' ');
        appendToken(retval, Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, ' ');
        appendToken(retval, Lexeme.Identity.ISSUE_TIME, input, getBulletinClass(), baseCtx);
        appendWhitespace(retval, ' ');
        if (appendToken(retval, Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR, input, getBulletinClass(), baseCtx) == 0) {
            retval.removeLast();
        }
        List<S> messages = input.getMessages();
        LexemeSequence messageSequence;
        for (S message : messages) {
            appendWhitespace(retval, NEW_LINE);
            messageSequence = tokenizeSingleMessage(message, hints);
            int charsOnRow = 0;
            List<Lexeme> lexemes = messageSequence.getLexemes();
            for (Lexeme l : lexemes) {
                if (l.getIdentity() != Lexeme.Identity.WHITE_SPACE && l.getIdentity() != Lexeme.Identity.END_TOKEN) {
                    int length = l.getTACToken().length();
                    if (charsOnRow + length >= MAX_ROW_LENGTH) {
                        if (retval.getLast().isPresent() && retval.getLast().get().getIdentity() == Lexeme.Identity.WHITE_SPACE) {
                            retval.removeLast();
                        }
                        appendWhitespace(retval, NEW_LINE);
                        appendWhitespace(retval, ' ', WRAPPED_LINE_INDENT);
                        charsOnRow = WRAPPED_LINE_INDENT;
                    }
                    retval.append(l);
                    appendWhitespace(retval, ' ');
                    charsOnRow += length + 1;
                }
            }
            while (retval.getLast().isPresent() && retval.getLast().get().getIdentity() == Lexeme.Identity.WHITE_SPACE) {
                retval.removeLast();
            }
            Lexeme endToken = messageSequence.getLastLexeme();
            if (endToken.getIdentity() != Lexeme.Identity.END_TOKEN) {
                throw new SerializingException("Contained message does not end in end token '='");
            }
            retval.append(endToken);
        }
        return retval.build();
    }

}
