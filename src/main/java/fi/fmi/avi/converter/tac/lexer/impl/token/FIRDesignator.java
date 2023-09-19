package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.SIGMETAIRMET;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;



/**
 * Created by rinne on 10/02/17.
 */
public class FIRDesignator extends RegexMatchingLexemeVisitor {
    public FIRDesignator(final OccurrenceFrequency prio) {
        super("^[A-Z]{4,}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious()&& LexemeIdentity.MWO_DESIGNATOR.equals(token.getPrevious().getIdentity())) {
            token.identify(LexemeIdentity.FIR_DESIGNATOR);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE,token.getTACToken().substring(0,4));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMETAIRMET.class.isAssignableFrom(clz)) {
                SIGMETAIRMET m = (SIGMETAIRMET) msg;
                if (m.getAirspace().getName() != null) {
                    return Optional.of(this.createLexeme(m.getAirspace().getDesignator(), LexemeIdentity.FIR_DESIGNATOR));
                }
            }
            return Optional.empty();
        }
    }
}
