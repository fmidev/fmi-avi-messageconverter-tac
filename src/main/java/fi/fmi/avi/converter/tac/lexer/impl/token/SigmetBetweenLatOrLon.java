package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_BETWEEN_LATLON;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetBetweenLatOrLon extends RegexMatchingLexemeVisitor {
    private static String re1="((N|S)\\sOF\\s([NS]\\d{2,4})\\sAND\\s(N|S)\\sOF\\s([NS](\\d{2,4})))";
    private static String re2="((W|E)\\sOF\\s([WE]\\d{3,5})\\sAND\\s(W|E)\\sOF\\s([WE](\\d{3,5})))";

    public SigmetBetweenLatOrLon(final OccurrenceFrequency prio) {
        super("^("+re1+")|("+re2+")$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        for (int i=0; i<10; i++) {
            System.err.println(i+":"+match.group(i));
        }
        token.identify(SIGMET_BETWEEN_LATLON);
        token.setParsedValue(ParsedValueName.RELATIONTYPE, match.group(3));
        token.setParsedValue(ParsedValueName.RELATEDLINE, match.group(4));
        token.setParsedValue(ParsedValueName.RELATIONTYPE2, match.group(5));
        token.setParsedValue(ParsedValueName.RELATEDLINE2, match.group(6));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                return Optional.of(createLexeme("SIGMET_BETWEEN_LATLON", SIGMET_BETWEEN_LATLON));
            }
            return Optional.empty();
        }
    }
}
