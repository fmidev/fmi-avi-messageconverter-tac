package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AMENDMENT;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
public class Amendment extends PrioritizedLexemeVisitor {

    public Amendment(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getPrevious() == token.getFirst() && "AMD".equalsIgnoreCase(token.getTACToken())) {
            token.identify(AMENDMENT);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            if (TAF.class.isAssignableFrom(clz)) {
                // Note: cancellation messages are also amendments
                if (AviationCodeListUser.TAFStatus.AMENDMENT == ((TAF) msg).getStatus() ||
                    AviationCodeListUser.TAFStatus.CANCELLATION == ((TAF) msg).getStatus()) {
                    return Optional.of(this.createLexeme("AMD", AMENDMENT));
                }

            }
            return Optional.empty();
        }
    }
}
