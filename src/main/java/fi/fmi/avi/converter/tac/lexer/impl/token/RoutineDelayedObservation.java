package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ROUTINE_DELAYED_OBSERVATION;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.METAR;

/**
 * Token parser for delayed observation indicator (RTD)
 */
public class RoutineDelayedObservation extends PrioritizedLexemeVisitor {

    public RoutineDelayedObservation(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getPrevious() != null && token.getPrevious().getIdentity() == ISSUE_TIME && "RTD".equalsIgnoreCase(token.getTACToken())) {
            token.identify(ROUTINE_DELAYED_OBSERVATION);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (METAR.class.isAssignableFrom(clz)) {
                if (((METAR) msg).isRoutineDelayed()) {
                    return Optional.of(this.createLexeme("RTD", ROUTINE_DELAYED_OBSERVATION));
                }
            }
            return Optional.empty();
        }
    }
}
