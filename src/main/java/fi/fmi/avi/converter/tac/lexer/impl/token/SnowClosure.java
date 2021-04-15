package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SNOW_CLOSURE;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;

/**
 * Created by rinne on 10/02/17.
 */
public class SnowClosure extends PrioritizedLexemeVisitor {
    public SnowClosure(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("SNOCLO".equalsIgnoreCase(token.getTACToken()) || "R/SNOCLO".equals(token.getTACToken())) {
            token.identify(SNOW_CLOSURE);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                if (((MeteorologicalTerminalAirReport) msg).isSnowClosure()) {
                    return Optional.of(createLexeme("R/SNOCLO", LexemeIdentity.SNOW_CLOSURE));
                }
            }
            return Optional.empty();
        }
    }
}
