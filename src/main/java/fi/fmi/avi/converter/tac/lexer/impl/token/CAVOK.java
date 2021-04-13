package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CAVOK;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Created by rinne on 10/02/17.
 */
public class CAVOK extends PrioritizedLexemeVisitor {
    public CAVOK(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("CAVOK".equalsIgnoreCase(token.getTACToken())) {
            token.identify(CAVOK);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();
            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                final MeteorologicalTerminalAirReport m = (MeteorologicalTerminalAirReport) msg;
                final Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
                if (trend.isPresent()) {
                    if (trend.get().isCeilingAndVisibilityOk()) {
                        retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                    }
                } else {
                    if (m.isCeilingAndVisibilityOk()) {
                        retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                    }
                }
            } else if (TAF.class.isAssignableFrom(clz)) {
                final Optional<TAFBaseForecast> base = ctx.getParameter("forecast", TAFBaseForecast.class);
                if (base.isPresent() && base.get().isCeilingAndVisibilityOk()) {
                    retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                }
                final Optional<TAFChangeForecast> change = ctx.getParameter("forecast", TAFChangeForecast.class);
                if (change.isPresent() && change.get().isCeilingAndVisibilityOk()) {
                    retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                }
            }
            return retval;
        }
    }
}
