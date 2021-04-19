package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetForecastAt extends RegexMatchingLexemeVisitor {

    public SigmetForecastAt(final OccurrenceFrequency prio) {
        super("^xFCST AT ([0-9]{4})Z$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (LexemeIdentity.SIGMET_START.equals(token.getFirst().getIdentity())) {        boolean afterLMC=false;
            Lexeme l = token.getPrevious();
            while (l!=null) {
                if (LexemeIdentity.SIGMET_LEVEL.equals(l.getIdentity())||
                LexemeIdentity.SIGMET_MOVING.equals(l.getIdentity())||
                LexemeIdentity.SIGMET_INTENSITY.equals(l.getIdentity())) {
                    afterLMC = true;
                    break;
                }
                l=l.getPrevious();
            }
            if (afterLMC) {
                token.identify(LexemeIdentity.SIGMET_FCST_AT);
                token.setParsedValue(VALUE, match.group(1));
            }
        }
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                return Optional.of(createLexeme("AND", LexemeIdentity.SIGMET_FCST_AT));
            }
            return Optional.empty();
        }
    }
}
