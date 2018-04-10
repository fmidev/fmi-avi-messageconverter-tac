package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SPECI_START;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessage;

/**
 * Created by rinne on 10/02/17.
 */
public class SpeciStart extends PrioritizedLexemeVisitor {
    public SpeciStart(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getFirst().equals(token) && "SPECI".equalsIgnoreCase(token.getTACToken())) {
            token.identify(SPECI_START);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessage> Lexeme getAsLexeme(final T msg, Class<T> clz, final ConversionHints hints, final Object... specifier) {
            if (SPECI.class.isAssignableFrom(clz)) {
                return this.createLexeme("SPECI", SPECI_START);
            } else {
                return null;
            }

        }
    }

}
