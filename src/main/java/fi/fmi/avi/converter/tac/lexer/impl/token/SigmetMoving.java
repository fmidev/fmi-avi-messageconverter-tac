package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_MOVING;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetMoving extends RegexMatchingLexemeVisitor {

    static String[] windDirs={"N", "NNE", "NE", "NNE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW",
    "WSW", "W", "WNW", "NW", "NNW"};

    public SigmetMoving(final OccurrenceFrequency prio) {
        super("^STNR|(MOV)\\s(N|NNE|NE|ENE|E|ESE|SE|SSE|S|SSW|SW|WSW|W|WNW|NW|NNW)\\s([0-9]{2})(KT|KMH)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if ("STNR".equals(match.group(0))) {
            token.identify(SIGMET_MOVING);
            token.setParsedValue(STATIONARY, true);
        } else {
            token.identify(SIGMET_MOVING);
            token.setParsedValue(STATIONARY, false);
            token.setParsedValue(DIRECTION, match.group(2));
            token.setParsedValue(VALUE, match.group(3));
            token.setParsedValue(UNIT, match.group(4));
        }
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET)msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    if (sigmet.getForecastGeometries().isPresent() && sigmet.getForecastGeometries().get().size()>0) {
                        return Optional.empty();
                    } else if (!sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingDirection().isPresent()) {
                        return Optional.of(createLexeme("STNR", SIGMET_MOVING));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("MOV");
                        sb.append(MeteorologicalBulletinSpecialCharacter.SPACE.getContent());
                        int index = (int) (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingDirection().get().getValue()/22.5);
                        if ((index>=0)&&(index<16)){
                            sb.append(windDirs[index]);
                        }
                        sb.append(MeteorologicalBulletinSpecialCharacter.SPACE.getContent());
                        NumericMeasure spd = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingSpeed().get();
                        sb.append(String.format("%02.0f", spd.getValue()));
                        sb.append(spd.getUom());
                        return Optional.of(createLexeme(sb.toString(), SIGMET_MOVING));
                    }
                }
            }
            if (AIRMET.class.isAssignableFrom(clz)) {
                AIRMET airmet = (AIRMET)msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    if (!airmet.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingDirection().isPresent()) {
                        return Optional.of(createLexeme("STNR", SIGMET_MOVING));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("MOV");
                        sb.append(MeteorologicalBulletinSpecialCharacter.SPACE.getContent());
                        int index = (int) (airmet.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingDirection().get().getValue()/22.5);
                        if ((index>=0)&&(index<16)){
                            sb.append(windDirs[index]);
                        }
                        sb.append(MeteorologicalBulletinSpecialCharacter.SPACE.getContent());
                        NumericMeasure spd = airmet.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingSpeed().get();
                        sb.append(String.format("%02.0f", spd.getValue()));
                        sb.append(spd.getUom());
                        return Optional.of(createLexeme(sb.toString(), SIGMET_MOVING));
                    }
                }
            }
            return Optional.empty();
        }
    }
}
