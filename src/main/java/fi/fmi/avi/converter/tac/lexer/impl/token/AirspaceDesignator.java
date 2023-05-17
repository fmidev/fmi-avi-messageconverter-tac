package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COUNTRY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIRSPACE_DESIGNATOR;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

/**
 * Created by rinne on 10/02/17.
 */
public class AirspaceDesignator extends RegexMatchingLexemeVisitor {
    public AirspaceDesignator(final OccurrenceFrequency prio) {
        super("^[A-Z]{4}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        //Must be first token:
        if (token == token.getFirst()) {
            token.identify(AIRSPACE_DESIGNATOR);
            token.setParsedValue(VALUE,token.getTACToken());
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET m = (SIGMET) msg;
                if (m.getAirspace().getDesignator() != null) {
                    return Optional.of(this.createLexeme(m.getAirspace().getDesignator(), AIRSPACE_DESIGNATOR));
                }
            }
            return Optional.empty();
        }
    }
}
