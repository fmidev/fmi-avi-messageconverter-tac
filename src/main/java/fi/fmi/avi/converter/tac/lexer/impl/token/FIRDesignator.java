package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;



/**
 * Created by rinne on 10/02/17.
 */
public class FIRDesignator extends RegexMatchingLexemeVisitor {
    /*
        The code-to-country mapping is not really needed here in the lexer, but these could be useful in other classes.
        Updating the list is also less error-prone with the name of the country attached to the code.

        The list copied from https://en.wikipedia.org/wiki/International_Civil_Aviation_Organization_airport_code
        on 11th Jan 2017.
     */


    private final static Map<String, ICAOCode.ICAOCodeCountryPrefix> codeToCountryMap = ICAOCode.ICAOCodeCountryPrefix.getCodeToCountryMap();

    public FIRDesignator(final OccurrenceFrequency prio) {
        super("^[A-Z]{4,}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious()&& LexemeIdentity.MWO_DESIGNATOR.equals(token.getPrevious().getIdentity())) {
            for (String s : codeToCountryMap.keySet()) {
                if (token.getTACToken().startsWith(s)) {
                	token.identify(LexemeIdentity.FIR_DESIGNATOR);
                	token.setParsedValue(Lexeme.ParsedValueName.COUNTRY, codeToCountryMap.get(s));
                    token.setParsedValue(Lexeme.ParsedValueName.VALUE,token.getTACToken().substring(0,4));
                    return;
                }
            }
            token.identify(LexemeIdentity.MWO_DESIGNATOR, Lexeme.Status.SYNTAX_ERROR, "Invalid ICAO code country prefix");
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET m = (SIGMET) msg;
                if (m.getMeteorologicalWatchOffice().getDesignator() != null) {
                    return Optional.of(this.createLexeme(m.getAirspace().getDesignator(), LexemeIdentity.FIR_DESIGNATOR));
                }
            }
            return Optional.empty();
        }
    }
}
