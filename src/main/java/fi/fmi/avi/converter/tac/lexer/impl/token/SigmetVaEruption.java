package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_VA_ERUPTION;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetVaEruption extends RegexMatchingLexemeVisitor {
    public SigmetVaEruption(final OccurrenceFrequency prio) {
        super("^(VA ERUPTION)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_VA_ERUPTION);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET) msg;
                if (AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA.equals(sigmet.getPhenomenon().orElse(null))) {
                    return Optional.of(this.createLexeme("VA ERUPTION", LexemeIdentity.SIGMET_VA_ERUPTION));
                } else {
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
    }
}
