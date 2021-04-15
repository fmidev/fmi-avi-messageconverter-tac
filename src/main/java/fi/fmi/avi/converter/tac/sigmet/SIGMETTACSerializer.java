package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.*;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
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
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);

        if (appendToken(retval,LexemeIdentity.SEQUENCE_DESCRIPTOR, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (appendToken(retval,LexemeIdentity.VALID_TIME, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.MWO_DESIGNATOR, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.FIR_DESIGNATOR, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.FIR_NAME, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.PHENOMENON_SIGMET, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval,LexemeIdentity.OBS_OR_FORECAST, input, SIGMET.class, baseCtx)>0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        // if ("ENTIRE FIR".equals(input.getAnalysisGeometries().get().get(0).getGeometry().get().getTacGeometry().get())){
        //     if (appendToken(retval,LexemeIdentity.SIGMET_ENTIRE_AREA, input, SIGMET.class, baseCtx)>0) {
        //         appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        //     }
        // }
        input.getAnalysisGeometries().ifPresent(l-> l.get(0).getGeometry()
            .ifPresent(g->g.getTacGeometry().ifPresent(t-> {
                System.err.println("found tacGeometry");
                try {
                    if (appendToken(retval,LexemeIdentity.SIGMET_ENTIRE_AREA, input, SIGMET.class, baseCtx)>0) {
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }

                } catch (SerializingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            })));

        input.getForecastGeometries().ifPresent(l-> l.get(0).getGeometry()
        .ifPresent(g->g.getTacGeometry().ifPresent(t-> {
            System.err.println("found forecast tacGeometry");
            try {
                if (appendToken(retval,LexemeIdentity.SIGMET_ENTIRE_AREA, input, SIGMET.class, baseCtx)>0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }

            } catch (SerializingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        })));

        input.getIntensityChange().ifPresent(l -> {
            try {
                if (appendToken(retval, LexemeIdentity.SIGMET_INTENSITY, input, SIGMET.class, baseCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
            } catch (SerializingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        });
        if (appendToken(retval, LexemeIdentity.AMENDMENT, input, SIGMET.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.CORRECTION, input, SIGMET.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        retval.removeLast();
        appendToken(retval, LexemeIdentity.END_TOKEN, input, SIGMET.class, baseCtx);
        return retval.build();
    }
}
