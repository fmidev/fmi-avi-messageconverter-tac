package fi.fmi.avi.converter.tac.swx;

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
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;


public class SWXTACSerializer extends AbstractTACSerializer<SpaceWeatherAdvisory> {

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    @Override
    public ConversionResult<String> convertMessage(final SpaceWeatherAdvisory input, final ConversionHints hints) {
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
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        if (!(msg instanceof SpaceWeatherAdvisory)) {
            throw new SerializingException("I can only tokenize Space weather advisories!");
        }
        final SpaceWeatherAdvisory input = (SpaceWeatherAdvisory) msg;
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<SpaceWeatherAdvisory> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, LexemeIdentity.SPACE_WEATHER_ADVISORY_START, input, SpaceWeatherAdvisory.class, baseCtx);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        if (appendToken(retval, LexemeIdentity.TEST_OR_EXCERCISE, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.ISSUE_TIME, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        return null;
    }
}
