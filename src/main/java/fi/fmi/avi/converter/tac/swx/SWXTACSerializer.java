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
        int labelColumnWidth = 20;
        if (hints != null) {
            if (hints.containsKey(ConversionHints.KEY_ADVISORY_LABEL_WIDTH)) {
                labelColumnWidth = (Integer) hints.get(ConversionHints.KEY_ADVISORY_LABEL_WIDTH);
            }
        }
        final SpaceWeatherAdvisory input = (SpaceWeatherAdvisory) msg;
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<SpaceWeatherAdvisory> baseCtx = new ReconstructorContext<>(input, hints);

        if (appendToken(retval, LexemeIdentity.SPACE_WEATHER_ADVISORY_START, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_STATUS_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_STATUS, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.DTG_ISSUE_TIME_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.ISSUE_TIME, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.SWX_CENTRE_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.SWX_CENTRE, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_NUMBER_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_NUMBER, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.REPLACE_ADVISORY_NUMBER, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.SWX_EFFECT_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.SWX_EFFECT, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        for (int i = 0; i < input.getAnalyses().size(); i++) {
            final ReconstructorContext<SpaceWeatherAdvisory> analysisContext = baseCtx.copyWithParameter("analysisIndex", i);

            if (appendToken(retval, LexemeIdentity.ADVISORY_PHENOMENA_LABEL, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendSpacePadding(retval, labelColumnWidth);
            }

            if (appendToken(retval, LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.SWX_NOT_EXPECTED, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (appendToken(retval, LexemeIdentity.SWX_NOT_AVAILABLE, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (appendToken(retval, LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.POLYGON_COORDINATE_PAIR, input, SpaceWeatherAdvisory.class, analysisContext) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                retval.removeLast(); // last whitespace removed
            }
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (input.getRemarks().isPresent()) {
            appendToken(retval, LexemeIdentity.REMARKS_START, input, SpaceWeatherAdvisory.class, baseCtx);
            appendSpacePadding(retval, labelColumnWidth);
            for (final String remark : input.getRemarks().get()) {
                this.appendToken(retval, LexemeIdentity.REMARK, input, SpaceWeatherAdvisory.class, baseCtx.copyWithParameter("remark", remark));
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                retval.removeLast(); // last whitespace removed
            }
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.NEXT_ADVISORY_LABEL, input, SpaceWeatherAdvisory.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        appendToken(retval, LexemeIdentity.NEXT_ADVISORY, input, SpaceWeatherAdvisory.class, baseCtx);
        retval.append(this.getLexingFactory().createLexeme("=", LexemeIdentity.END_TOKEN));
        return retval.build();
    }

    private void appendSpacePadding(final LexemeSequenceBuilder builder, final int labelSize) {
        if (labelSize > 0) {
            if (builder.getLast().isPresent()) {
                final int whiteSpace = labelSize - builder.getLast().get().getTACToken().length();
                appendWhitespace(builder, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE, whiteSpace);
            }
        }
    }
}
