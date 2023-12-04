package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.model.sigmet.SIGMET;

public class SIGMETTACSerializer extends AbstractTACSerializer<SIGMET> {
    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    @Override
    public ConversionResult<String> convertMessage(final SIGMET input, final ConversionHints hints) {
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
        if (!(msg instanceof SIGMET)) {
            throw new SerializingException("I can only tokenize SIGMETs!");
        }
        final SIGMET input = (SIGMET) msg;
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<SIGMET> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, LexemeIdentity.SIGMET_START, input, SIGMET.class, baseCtx);
        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);

        if (appendToken(retval,LexemeIdentity.SEQUENCE_DESCRIPTOR, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (appendToken(retval,LexemeIdentity.VALID_TIME, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.MWO_DESIGNATOR, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval,LexemeIdentity.FIR_DESIGNATOR, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.FIR_NAME, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.SIGMET_USAGE, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }

        if (appendToken(retval,LexemeIdentity.SIGMET_CANCEL, input, SIGMET.class, baseCtx)>0) {
            appendToken(retval, LexemeIdentity.END_TOKEN, input, SIGMET.class, baseCtx);
            return retval.build();
        }

        if (appendToken(retval,LexemeIdentity.SIGMET_VA_ERUPTION, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.SIGMET_VA_NAME, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.SIGMET_VA_POSITION, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (appendToken(retval,LexemeIdentity.SIGMET_PHENOMENON, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        input.getAnalysisGeometries().ifPresent(g-> {
            for (int i=0; i<g.size();i++) {
                final ReconstructorContext<SIGMET> analysisCtx = baseCtx.copyWithParameter("analysisIndex", i);
                try {
                    if (appendToken(retval,LexemeIdentity.OBS_OR_FORECAST, input, SIGMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                    if (appendToken(retval,LexemeIdentity.SIGMET_TAC_ELEMENT, input, SIGMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                    } else {
                        if (appendToken(retval,LexemeIdentity.SIGMET_WITHIN, input, SIGMET.class, analysisCtx)>0) {
                            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval,LexemeIdentity.POLYGON_COORDINATE_PAIR, input, SIGMET.class, analysisCtx)>0) {
                            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval,LexemeIdentity.SIGMET_ENTIRE_AREA, input, SIGMET.class, analysisCtx)>0) {
                            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval,LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT, input, SIGMET.class, analysisCtx)>0) {
                            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                    }
                    if (appendToken(retval,LexemeIdentity.SIGMET_LEVEL, input, SIGMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                    if (appendToken(retval,LexemeIdentity.SIGMET_MOVING, input, SIGMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                    if (appendToken(retval, LexemeIdentity.SIGMET_INTENSITY, input, SIGMET.class, analysisCtx) > 0) {
                        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                } catch (SerializingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        input.getForecastGeometries().ifPresent(g-> {
            for (int i=0; i<g.size(); i++) {
                final ReconstructorContext<SIGMET> forecastCtx = baseCtx.copyWithParameter("forecastIndex", i);
                try {
                    if (appendToken(retval,LexemeIdentity.SIGMET_FCST_AT, input, SIGMET.class, forecastCtx)>0) {
                        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                    if (appendToken(retval,LexemeIdentity.SIGMET_NO_VA_EXP, input, SIGMET.class, forecastCtx)>0) {
                        appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                    } else {
                        if (appendToken(retval,LexemeIdentity.SIGMET_TAC_ELEMENT, input, SIGMET.class, forecastCtx)>0) {
                            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                        } else {
                            if (appendToken(retval,LexemeIdentity.SIGMET_WITHIN, input, SIGMET.class, forecastCtx)>0) {
                                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                            }
                            if (appendToken(retval,LexemeIdentity.POLYGON_COORDINATE_PAIR, input, SIGMET.class, forecastCtx)>0) {
                                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                            }
                            if (appendToken(retval,LexemeIdentity.SIGMET_ENTIRE_AREA, input, SIGMET.class, forecastCtx)>0) {
                                appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
                            }
                            if (i==0) { //TODO currently only for the first forecast
                            }
                        }
                }
                } catch (SerializingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        if (appendToken(retval, LexemeIdentity.AMENDMENT, input, SIGMET.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.CORRECTION, input, SIGMET.class, baseCtx) > 0) {
            appendWhitespace(retval, MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        while (retval.getLast().isPresent() && LexemeIdentity.WHITE_SPACE.equals(retval.getLast().get().getIdentity())) {
            retval.removeLast();
        }
        appendToken(retval, LexemeIdentity.END_TOKEN, input, SIGMET.class, baseCtx);
        return retval.build();
    }
}
