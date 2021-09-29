package fi.fmi.avi.converter.tac.airmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.*;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.AIRMET;

public class AIRMETTACSerializer extends AbstractTACSerializer<AIRMET> {
    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    @Override
    public ConversionResult<String> convertMessage(final AIRMET input, final ConversionHints hints) {
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
        if (!(msg instanceof AIRMET)) {
            throw new SerializingException("I can only tokenize AIRMETs!");
        }
        final AIRMET input = (AIRMET) msg;
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<AIRMET> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, LexemeIdentity.AIRMET_START, input, AIRMET.class, baseCtx);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);

        if (appendToken(retval,LexemeIdentity.SEQUENCE_DESCRIPTOR, input, AIRMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (appendToken(retval,LexemeIdentity.VALID_TIME, input, AIRMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.MWO_DESIGNATOR, input, AIRMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
        }
        if (appendToken(retval,LexemeIdentity.FIR_DESIGNATOR, input, AIRMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.FIR_NAME, input, AIRMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.SIGMET_USAGE, input, AIRMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (appendToken(retval,LexemeIdentity.AIRMET_CANCEL, input, AIRMET.class, baseCtx)>0) {
            appendToken(retval, LexemeIdentity.END_TOKEN, input, AIRMET.class, baseCtx);
            return retval.build();
        }

        if (appendToken(retval,LexemeIdentity.AIRMET_PHENOMENON, input, AIRMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        input.getAnalysisGeometries().ifPresent(g-> {
            for (int i=0; i<g.size();i++) {
                final ReconstructorContext<AIRMET> analysisCtx = baseCtx.copyWithParameter("analysisIndex", i);
                try {
                    if (appendToken(retval,LexemeIdentity.OBS_OR_FORECAST, input, AIRMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                    if (appendToken(retval,LexemeIdentity.SIGMET_TAC_ELEMENT, input, AIRMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    } else {
                        if (appendToken(retval,LexemeIdentity.SIGMET_WITHIN, input, AIRMET.class, analysisCtx)>0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval,LexemeIdentity.POLYGON_COORDINATE_PAIR, input, AIRMET.class, analysisCtx)>0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval,LexemeIdentity.SIGMET_ENTIRE_AREA, input, AIRMET.class, analysisCtx)>0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                    }

                    if (appendToken(retval,LexemeIdentity.SIGMET_LEVEL, input, AIRMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }

                    if (appendToken(retval,LexemeIdentity.SIGMET_MOVING, input, AIRMET.class, analysisCtx)>0) {
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }

                    if (appendToken(retval, LexemeIdentity.SIGMET_INTENSITY, input, AIRMET.class, analysisCtx) > 0) {
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                } catch (SerializingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });

        if (appendToken(retval, LexemeIdentity.AMENDMENT, input, AIRMET.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.CORRECTION, input, AIRMET.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        retval.removeLast();
        appendToken(retval, LexemeIdentity.END_TOKEN, input, AIRMET.class, baseCtx);
        return retval.build();
    }
}
