package fi.fmi.avi.converter.tac.swx.amd79;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;

public class SWXAmd79TACSerializer extends AbstractTACSerializer<SpaceWeatherAdvisoryAmd79> {

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        if (!(msg instanceof SpaceWeatherAdvisoryAmd79)) {
            throw new SerializingException("I can only tokenize Space weather advisories!");
        }
        int labelColumnWidth = 20;
        if (hints != null) {
            if (hints.containsKey(ConversionHints.KEY_ADVISORY_LABEL_WIDTH)) {
                labelColumnWidth = (Integer) hints.get(ConversionHints.KEY_ADVISORY_LABEL_WIDTH);
            }
        }
        final SpaceWeatherAdvisoryAmd79 input = (SpaceWeatherAdvisoryAmd79) msg;
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<SpaceWeatherAdvisoryAmd79> baseCtx = new ReconstructorContext<>(input, hints);

        if (appendToken(retval, LexemeIdentity.SPACE_WEATHER_ADVISORY_START, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_STATUS_LABEL, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_STATUS, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.DTG_ISSUE_TIME_LABEL, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.ISSUE_TIME, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.SWX_CENTRE_LABEL, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.SWX_CENTRE, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_NUMBER_LABEL, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.ADVISORY_NUMBER, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.REPLACE_ADVISORY_NUMBER, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval, LexemeIdentity.SWX_EFFECT_LABEL, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        if (appendToken(retval, LexemeIdentity.SWX_EFFECT_AND_INTENSITY, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        for (int i = 0; i < input.getAnalyses().size(); i++) {
            final ReconstructorContext<SpaceWeatherAdvisoryAmd79> analysisContext = baseCtx.copyWithParameter("analysisIndex", i);

            if (appendToken(retval, LexemeIdentity.ADVISORY_PHENOMENA_LABEL, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendSpacePadding(retval, labelColumnWidth);
            }

            if (appendToken(retval, LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.SWX_NOT_EXPECTED, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (appendToken(retval, LexemeIdentity.SWX_NOT_AVAILABLE, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (appendToken(retval, LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.POLYGON_COORDINATE_PAIR, input, SpaceWeatherAdvisoryAmd79.class, analysisContext) > 0) {
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                retval.removeLast(); // last whitespace removed
            }
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        appendToken(retval, LexemeIdentity.REMARKS_START, input, SpaceWeatherAdvisoryAmd79.class, baseCtx);
        appendSpacePadding(retval, labelColumnWidth);
        if (input.getRemarks().isPresent()) {
            for (final String remark : input.getRemarks().get()) {
                this.appendToken(retval, LexemeIdentity.REMARK, input, SpaceWeatherAdvisoryAmd79.class, baseCtx.copyWithParameter("remark", remark));
                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
                retval.removeLast(); // last whitespace removed
            }
        } else {
            this.appendToken(retval, LexemeIdentity.REMARK, input, SpaceWeatherAdvisoryAmd79.class, baseCtx.copyWithParameter("remark", "NIL"));
        }
        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        if (appendToken(retval, LexemeIdentity.NEXT_ADVISORY_LABEL, input, SpaceWeatherAdvisoryAmd79.class, baseCtx) > 0) {
            appendSpacePadding(retval, labelColumnWidth);
        }
        appendToken(retval, LexemeIdentity.NEXT_ADVISORY, input, SpaceWeatherAdvisoryAmd79.class, baseCtx);
        retval.append(this.getLexingFactory().createLexeme("=", LexemeIdentity.END_TOKEN));
        return retval.build();
    }

    private void appendSpacePadding(final LexemeSequenceBuilder builder, final int labelSize) {
        if (labelSize > 0) {
            if (builder.getLast().isPresent()) {
                final int whiteSpace = labelSize - builder.getLast().get().getTACToken().length();
                appendWhitespace(builder, MeteorologicalBulletinSpecialCharacter.SPACE, whiteSpace);
            }
        }
    }
}
