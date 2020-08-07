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
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;

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
        Integer labelSize = hints.get(ConversionHints.KEY_SWX_LABEL_END_LENGTH) == null ? 20 : (Integer) hints.get(ConversionHints.KEY_SWX_LABEL_END_LENGTH);
        final SpaceWeatherAdvisory input = (SpaceWeatherAdvisory) msg;
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<SpaceWeatherAdvisory> baseCtx = new ReconstructorContext<>(input, hints);

        if (appendToken(retval, LexemeIdentity.SPACE_WEATHER_ADVISORY_START, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.TEST_OR_EXCERCISE_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhiteSpace(retval, labelSize);
        }
        if (appendToken(retval, LexemeIdentity.TEST_OR_EXCERCISE, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.SWX_ISSUE_TIME_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhiteSpace(retval, labelSize);
        }
        if (appendToken(retval, LexemeIdentity.ISSUE_TIME, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.SWX_CENTRE_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhiteSpace(retval, labelSize);
        }
        if (appendToken(retval, LexemeIdentity.SWX_CENTRE, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_NUMBER_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhiteSpace(retval, labelSize);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_NUMBER, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.SWX_EFFECT_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhiteSpace(retval, labelSize);
        }
        if (appendToken(retval, LexemeIdentity.SWX_EFFECT, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhiteSpace(retval, labelSize);
        }
        if (appendToken(retval, LexemeIdentity.REPLACE_ADVISORY_NUMBER, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        for (int i = 0; i < input.getAnalyses().size(); i++) {
            baseCtx.setHint(ConversionHints.KEY_SWX_ANALYSIS_INDEX, i);

            if (appendToken(retval, LexemeIdentity.ADVISORY_PHENOMENA_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
                appendWhiteSpace(retval, labelSize);
            }

            if (appendToken(retval, LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            SpaceWeatherAdvisoryAnalysis analysis = input.getAnalyses().get(i);

            if (analysis.isNoPhenomenaExpected()) {
                appendToken(retval, LexemeIdentity.SWX_NOT_EXPECTED, input, SpaceWeatherAdvisory.class, baseCtx);
            } else if (analysis.isNoInformationAvailable()) {
                if (appendToken(retval, LexemeIdentity.SWX_NOT_AVAILABLE, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
            } else {
                if (appendToken(retval, LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    appendToken(retval, LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT, input, SpaceWeatherAdvisory.class, baseCtx);
                } else {
                    appendToken(retval, LexemeIdentity.SWX_PHENOMENON_POLYGON_LIMIT, input, SpaceWeatherAdvisory.class, baseCtx);
                }
                appendToken(retval, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT, input, SpaceWeatherAdvisory.class, baseCtx);
            }
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (input.getRemarks().isPresent() && input.getRemarks().get().size() > 0) {
            baseCtx.setParameter("remark", input.getRemarks().get().get(0));
            if (appendToken(retval, LexemeIdentity.REMARKS_START, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
                appendWhiteSpace(retval, labelSize);
            }
            if (appendToken(retval, LexemeIdentity.REMARK, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.NEXT_ADVISORY_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhiteSpace(retval, labelSize);
        }
        if (appendToken(retval, LexemeIdentity.NEXT_ADVISORY, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }

        return retval.build();
    }
    private void appendWhiteSpace(final LexemeSequenceBuilder builder, final int labelSize) {
        int whiteSpace = labelSize - builder.getLast().get().getTACToken().length();
        if (labelSize > 0 && whiteSpace > 0) {
            appendWhitespace(builder, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE, whiteSpace);
        } else {
            appendWhitespace(builder, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

    }
}
