package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AUTOMATED;

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
public class AutoMetar extends PrioritizedLexemeVisitor {
    public AutoMetar(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("AUTO".equalsIgnoreCase(token.getTACToken())) {
            token.identify(AUTOMATED);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                final MeteorologicalTerminalAirReport m = (MeteorologicalTerminalAirReport) msg;
                if (m.isAutomatedStation()) {
                    return Optional.of(this.createLexeme("AUTO", LexemeIdentity.AUTOMATED));
                }
            }
            return Optional.empty();
        }
    }
}
