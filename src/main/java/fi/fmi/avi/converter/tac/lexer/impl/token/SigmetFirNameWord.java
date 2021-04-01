package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_FIR_NAME_WORD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.FIR_DESIGNATOR;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetFirNameWord extends RegexMatchingLexemeVisitor {
    public SigmetFirNameWord(final OccurrenceFrequency prio) {
        super("^(\\w*)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious()&&(FIR_DESIGNATOR.equals(token.getPrevious().getIdentity())||
                      SIGMET_FIR_NAME_WORD.equals(token.getPrevious().getIdentity()))) {
                          token.identify(SIGMET_FIR_NAME_WORD);
                      }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET m = (SIGMET) msg;
            }
            return Optional.empty();
        }
    }
}
