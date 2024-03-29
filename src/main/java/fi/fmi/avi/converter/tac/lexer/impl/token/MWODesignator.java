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

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COUNTRY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.MWO_DESIGNATOR;

/**
 * Created by rinne on 10/02/17.
 */
public class MWODesignator extends RegexMatchingLexemeVisitor {
    public MWODesignator(final OccurrenceFrequency prio) {
        super("^[A-Z]{4,}-$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious()&&LexemeIdentity.VALID_TIME.equals(token.getPrevious().getIdentity())) {
            token.identify(MWO_DESIGNATOR);
            token.setParsedValue(VALUE,token.getTACToken().substring(0,4));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMETAIRMET.class.isAssignableFrom(clz)) {
                SIGMETAIRMET m = (SIGMETAIRMET) msg;
                if (m.getMeteorologicalWatchOffice().getDesignator() != null) {
                    return Optional.of(this.createLexeme(m.getMeteorologicalWatchOffice().getDesignator()+"-", MWO_DESIGNATOR));
                }
            }
            return Optional.empty();
        }
    }
}
