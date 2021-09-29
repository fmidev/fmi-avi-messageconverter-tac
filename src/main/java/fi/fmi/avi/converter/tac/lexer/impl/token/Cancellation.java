package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CANCELLATION;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VALID_TIME;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
public class Cancellation extends PrioritizedLexemeVisitor {

    public Cancellation(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getPrevious() != null && VALID_TIME.equals(token.getPrevious().getIdentity()) && "CNL".equalsIgnoreCase(token.getTACToken())) {
            token.identify(CANCELLATION);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            if (TAF.class.isAssignableFrom(clz)) {
                if (((TAF) msg).isCancelMessage()) {
                    return Optional.of(this.createLexeme("CNL", CANCELLATION));
                }
            }
            return Optional.empty();
        }
    }
}
