package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.SIGMETAIRMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

/**
 * Created by rinne on 10/02/17.
 */
public class AirmetSigmetObsOrForecast extends RegexMatchingLexemeVisitor {

    public AirmetSigmetObsOrForecast(final OccurrenceFrequency prio) {
        super("^(OBS|FCST)(\\s+AT\\s+([0-9]{2})([0-9]{2})Z)?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        //OBS_OR_FORECAST should come after SIGMET_PHENOMENON but before SIGMET_LEVEL, SIGMET_MOVEMENT, SIGMET_INTENSITIY_CHANGE
        if ((SIGMET_START.equals(token.getFirst().getIdentity()) &&
                SIGMET_PHENOMENON.equals(token.getPrevious().getIdentity())) ||
                (AIRMET_START.equals(token.getFirst().getIdentity()) &&
                        AIRMET_PHENOMENON.equals(token.getPrevious().getIdentity()))) {
            token.identify(LexemeIdentity.OBS_OR_FORECAST);
            token.setParsedValue(IS_FORECAST, !"OBS".equals(match.group(1)));
            if (match.group(3) != null && !match.group(3).isEmpty() &&
                    (match.group(4) != null) && !match.group(4).isEmpty()) {
                token.setParsedValue(HOUR1, Integer.valueOf(match.group(3)));
                token.setParsedValue(MINUTE1, Integer.valueOf(match.group(4)));
            }
        }
    }

    public static class Reconstructor extends SigmetForecastAt.Reconstructor {

        private static final Map<SigmetAnalysisType, String> SIGMET_ANALYSIS_TYPE_CODES = initSigmetAnalysisTypeCodes();

        private static Map<SigmetAnalysisType, String> initSigmetAnalysisTypeCodes() {
            final EnumMap<SigmetAnalysisType, String> codes = new EnumMap<>(SigmetAnalysisType.class);
            codes.put(SigmetAnalysisType.OBSERVATION, "OBS");
            codes.put(SigmetAnalysisType.FORECAST, "FCST");
            return Collections.unmodifiableMap(codes);
        }

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(
                final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) throws SerializingException {
            if (SIGMETAIRMET.class.isAssignableFrom(clz)) {
                final SIGMETAIRMET sigmetAirmet = (SIGMETAIRMET) msg;
                final Optional<Lexeme> lexeme = ctx.getParameter("analysisIndex", Integer.class)
                        .flatMap(analysisIndex -> sigmetAirmet.getAnalysisGeometries()
                                .map(geometries -> geometries.get(analysisIndex)))
                        .flatMap(geometry -> geometry.getAnalysisType()
                                .map(SIGMET_ANALYSIS_TYPE_CODES::get)
                                .map(analysisTypeString -> this.createLexeme(
                                        analysisTypeString + getAnalysisTimeString(geometry).orElse(""),
                                        OBS_OR_FORECAST)));
                if (lexeme.isPresent()) {
                    return lexeme;
                }

                return super.getAsLexeme(msg, clz, ctx);
            }

            return Optional.empty();
        }
    }
}
