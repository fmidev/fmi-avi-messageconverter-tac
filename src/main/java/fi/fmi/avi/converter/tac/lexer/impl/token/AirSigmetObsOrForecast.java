package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;
/**
 * Created by rinne on 10/02/17.
 */
public class AirSigmetObsOrForecast extends RegexMatchingLexemeVisitor {

    public AirSigmetObsOrForecast(final OccurrenceFrequency prio) {
        super("^(OBS|FCST)(\\sAT\\s([0-9]{2})([0-9]{2})Z)?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        //OBS_OR_FORECAST should come after SIGMET_PHENOMENON but before SIGMET_LEVEL, SIGMET_MOVEMENT, SIGMET_INTENSITIY_CHANGE
        if ((SIGMET_START.equals(token.getFirst().getIdentity())&&
            SIGMET_PHENOMENON.equals(token.getPrevious().getIdentity()))||
            (AIRMET_START.equals(token.getFirst().getIdentity())&&
            AIRMET_PHENOMENON.equals(token.getPrevious().getIdentity()))) {
            token.identify(LexemeIdentity.OBS_OR_FORECAST);
            token.setParsedValue(IS_FORECAST, !("OBS".equals(match.group(1))));
            if ((match.group(3)!=null)&&(match.group(3).length()>0)&&
                (match.group(4)!=null)&&(match.group(4).length()>0)) {
                token.setParsedValue(HOUR1, Integer.valueOf(match.group(3)));
                token.setParsedValue(MINUTE1, Integer.valueOf(match.group(4)));
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET m = (SIGMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    String tim = "";
                    if (m.getAnalysisGeometries().get().get(analysisIndex.get()).getTime().isPresent()) {
                        PartialOrCompleteTimeInstant t = m.getAnalysisGeometries().get().get(0).getTime().get();
                        tim = String.format(Locale.US, " AT %02d%02dZ", t.getHour().getAsInt(), t.getMinute().getAsInt());
                    }
                    if (SigmetAnalysisType.OBSERVATION.equals(m.getAnalysisGeometries().get().get(analysisIndex.get()).getAnalysisType().orElse(null))) {
                        return Optional.of(this.createLexeme("OBS" + tim, LexemeIdentity.OBS_OR_FORECAST));
                    } else if (SigmetAnalysisType.FORECAST.equals(m.getAnalysisGeometries().get().get(analysisIndex.get()).getAnalysisType().orElse(null))) {
                        return Optional.of(this.createLexeme("FCST" + tim, LexemeIdentity.OBS_OR_FORECAST));
                    }
                }
                final Optional<Integer> forecastIndex = ctx.getParameter("forecastIndex", Integer.class);
                if (forecastIndex.isPresent()) {
                    String tim="";
                    if (m.getForecastGeometries().get().get(forecastIndex.get()).getTime().isPresent()) {
                        PartialOrCompleteTimeInstant t = m.getForecastGeometries().get().get(0).getTime().get();
                        tim=String.format(Locale.US, " AT %02d%02dZ", t.getHour().getAsInt(), t.getMinute().getAsInt());
                    }
                    return Optional.of(this.createLexeme("FCST"+tim, LexemeIdentity.OBS_OR_FORECAST));
                }
            }
            if (AIRMET.class.isAssignableFrom(clz)) {
                AIRMET m = (AIRMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    String tim = "";
                    if (m.getAnalysisGeometries().get().get(analysisIndex.get()).getTime().isPresent()) {
                        PartialOrCompleteTimeInstant t = m.getAnalysisGeometries().get().get(0).getTime().get();
                        tim = String.format(Locale.US, " AT %02d%02dZ", t.getHour().getAsInt(), t.getMinute().getAsInt());
                    }
                    if (SigmetAnalysisType.OBSERVATION.equals(m.getAnalysisGeometries().get().get(analysisIndex.get()).getAnalysisType().orElse(null))) {
                        return Optional.of(this.createLexeme("OBS" + tim, LexemeIdentity.OBS_OR_FORECAST));
                    } else if (SigmetAnalysisType.FORECAST.equals(m.getAnalysisGeometries().get().get(analysisIndex.get()).getAnalysisType().orElse(null))) {
                        return Optional.of(this.createLexeme("FCST" + tim, LexemeIdentity.OBS_OR_FORECAST));
                    }
                }
            }
            return Optional.empty();
        }
    }
}
