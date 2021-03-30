package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_WITHIN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SPECI_START;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetWithin extends PrioritizedLexemeVisitor {
    public SigmetWithin(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("WI".equalsIgnoreCase(token.getTACToken())) {
            token.identify(SIGMET_WITHIN);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                return Optional.of(this.createLexeme("WI", SIGMET_WITHIN));
            }
            return Optional.empty();
        }
    }

}
