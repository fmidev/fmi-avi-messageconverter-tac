package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REMARKS_START;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

/**
 * Created by rinne on 10/02/17.
 */
public class RemarkStart extends PrioritizedLexemeVisitor {
    public RemarkStart(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("RMK".equalsIgnoreCase(token.getTACToken())) {
            token.identify(REMARKS_START);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            if (msg instanceof AviationWeatherMessage) {
                final AviationWeatherMessage aviMsg = (AviationWeatherMessage) msg;
                if (aviMsg.getRemarks().isPresent() && !aviMsg.getRemarks().get().isEmpty()) {
                    return Optional.of(this.createLexeme("RMK", REMARKS_START));
                }
            }
            return Optional.empty();
        }
    }
}
