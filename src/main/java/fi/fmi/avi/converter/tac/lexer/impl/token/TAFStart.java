package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_START;

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
public class TAFStart extends PrioritizedLexemeVisitor {
    public TAFStart(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("TAF".equalsIgnoreCase(token.getTACToken())) {
            if (token.getFirst() != null && (token.getFirst().equals(token) || Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS == token.getFirst()
                    .getIdentity())) {
                token.identify(TAF_START);
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (TAF.class.isAssignableFrom(clz)) {
                return Optional.of(this.createLexeme("TAF", Lexeme.Identity.TAF_START));
            } else {
                return Optional.empty();
            }
        }
    }

}
