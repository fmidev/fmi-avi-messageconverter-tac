package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;

/**
 * Created by rinne on 10/02/17.
 */
public class ObsOrForecast extends RegexMatchingLexemeVisitor {

    public ObsOrForecast(final OccurrenceFrequency prio) {
        super("^OBS|FCST$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious()&&LexemeIdentity.SIGMET_START.equals(token.getFirst().getIdentity())) {
            if (token.getTACToken().startsWith("OBS")) {
                token.identify(LexemeIdentity.OBS_OR_FORECAST);
                token.setParsedValue(VALUE, "OBS");
                return;
            }
            if (token.getTACToken().startsWith("FCST")) {
              token.identify(LexemeIdentity.OBS_OR_FORECAST);
              token.setParsedValue(VALUE, "FCST");
              return;
          }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET m = (SIGMET) msg;
                if (SigmetAnalysisType.OBSERVATION.equals(m.getAnalysisType())) {
                    return Optional.of(this.createLexeme("OBS", LexemeIdentity.OBS_OR_FORECAST));
                } else if (SigmetAnalysisType.FORECAST.equals(m.getAnalysisType())) {
                    return Optional.of(this.createLexeme("FCST", LexemeIdentity.OBS_OR_FORECAST));
                }
            }
            return Optional.empty();
        }
    }
}
