package fi.fmi.avi.converter.tac.taf;

import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;

public class TAFBulletinTACSerializer extends AbstractTACSerializer<TAFBulletin> {

    public static final int MAX_ROW_LENGTH = 60;
    public static final int WRAPPED_LINE_INDENT = 5;
    public static final CharSequence NEW_LINE = "\r\n";

    private TAFTACSerializer tafSerializer;

    public void setTafSerializer(final TAFTACSerializer serializer) {
        this.tafSerializer = serializer;
    }

    @Override
    public ConversionResult<String> convertMessage(final TAFBulletin input, final ConversionHints hints) {
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

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        if (!(msg instanceof TAFBulletin)) {
            throw new SerializingException("I can only tokenize TAFBulletins!");
        }
        if (this.tafSerializer == null) {
            throw new IllegalStateException("No TafSerializer set");
        }
        TAFBulletin input = (TAFBulletin) msg;
        LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        ReconstructorContext<TAFBulletin> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, input, TAFBulletin.class, baseCtx);
        appendWhitespace(retval, ' ');
        appendToken(retval, Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, input, TAFBulletin.class, baseCtx);
        appendWhitespace(retval, ' ');
        appendToken(retval, Lexeme.Identity.ISSUE_TIME, input, TAFBulletin.class, baseCtx);
        appendWhitespace(retval, ' ');
        if (appendToken(retval, Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR, input, TAFBulletin.class, baseCtx) == 0) {
            retval.removeLast();
        }
        List<TAF> messages = input.getMessages();
        LexemeSequence tafSequence;
        for (TAF message : messages) {
            appendWhitespace(retval, NEW_LINE);
            tafSequence = tafSerializer.tokenizeMessage(message, hints);
            int charsOnRow = 0;
            List<Lexeme> lexemes = tafSequence.getLexemes();
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
            Lexeme endToken = tafSequence.getLastLexeme();
            if (endToken.getIdentity() != Lexeme.Identity.END_TOKEN) {
                throw new SerializingException("TAF does not end in end token '='");
            }
            retval.append(endToken);
        }
        return retval.build();
    }

}
