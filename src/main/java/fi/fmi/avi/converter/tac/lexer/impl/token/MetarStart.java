package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.METAR_START;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;

/**
 * Created by rinne on 10/02/17.
 */
public class MetarStart extends PrioritizedLexemeVisitor {
    public MetarStart(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getFirst().equals(token) && "METAR".equalsIgnoreCase(token.getTACToken())) {
            token.identify(METAR_START);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ConversionHints hints,
                final Object... specifier) {
            if (METAR.class.isAssignableFrom(clz)) {
                return Optional.of(this.createLexeme("METAR", METAR_START));
            } else {
                return Optional.empty();
            }

        }
    }

}
