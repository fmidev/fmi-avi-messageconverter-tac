package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.NO_SIGNIFICANT_WEATHER;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Created by rinne on 10/02/17.
 */
public class NoSignificantWeather extends PrioritizedLexemeVisitor {
    public NoSignificantWeather(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("NSW".equalsIgnoreCase(token.getTACToken())) {
            token.identify(NO_SIGNIFICANT_WEATHER);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {

            final Optional<TAFChangeForecast> forecast = ctx.getParameter("forecast", TAFChangeForecast.class);
            if (forecast.isPresent() && forecast.get().isNoSignificantWeather()) {
                return Optional.of(this.createLexeme("NSW", NO_SIGNIFICANT_WEATHER));
            }

            final Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
            if (trend.isPresent() && trend.get().isNoSignificantWeather()) {
                return Optional.of(this.createLexeme("NSW", NO_SIGNIFICANT_WEATHER));
            }
            return Optional.empty();
        }
    }
}
