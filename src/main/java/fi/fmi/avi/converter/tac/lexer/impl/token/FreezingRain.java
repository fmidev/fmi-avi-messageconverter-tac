package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.SEV_ICE_FZRA;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TS_ADJECTIVE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

/**
 * Created by rinne on 10/02/17.
 */
public class FreezingRain extends PrioritizedLexemeVisitor {

    public FreezingRain(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {

        String tok=token.getTACToken();
        if ("(FZRA)".equals(tok)&&token.hasPrevious()&&token.getPrevious().getTACToken().startsWith("SEV ICE")) {
            token.identify(PHENOMENON_SIGMET_FZRA);
            token.getPrevious().setParsedValue(SEV_ICE_FZRA, true);
        }
        return;
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            return Optional.empty();
        }
    }
}
