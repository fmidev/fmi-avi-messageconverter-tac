package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.EXER;

/**
 * Created by rinne on 10/02/17.
 */
public class Exer extends PrioritizedLexemeVisitor {

    public Exer(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("EXER".equalsIgnoreCase(token.getTACToken())) {
            token.identify(EXER);
            return;
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                if (AviationCodeListUser.PermissibleUsageReason.EXERCISE == ((SIGMET) msg).getPermissibleUsageReason().get()) {
                    return Optional.of(this.createLexeme("EXER", EXER));
                }
            } else if (AIRMET.class.isAssignableFrom(clz)) {
                if (AviationCodeListUser.PermissibleUsageReason.EXERCISE == ((SIGMET) msg).getPermissibleUsageReason().get()) {
                    return Optional.of(this.createLexeme("EXER", EXER));
                }
            }
            return Optional.empty();
        }
    }
}
