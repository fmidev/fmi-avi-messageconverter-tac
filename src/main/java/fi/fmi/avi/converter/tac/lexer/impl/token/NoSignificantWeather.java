package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.NO_SIGNIFICANT_WEATHER;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
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
    public NoSignificantWeather(final Priority prio) {
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
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {

    	    Optional<TAFChangeForecast> forecast = ctx.getParameter("forecast", TAFChangeForecast.class);
            if (forecast.isPresent() && forecast.get().isNoSignificantWeather()) {
                return Optional.of(this.createLexeme("NSW", NO_SIGNIFICANT_WEATHER));
            }

            Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
            if (trend.isPresent() && trend.get().isNoSignificantWeather()) {
                return Optional.of(this.createLexeme("NSW", NO_SIGNIFICANT_WEATHER));
            }
            return Optional.empty();
        }
    }
}
