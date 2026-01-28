package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_OUTSIDE_LATLON;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetOutsideLatOrLon extends RegexMatchingLexemeVisitor {
    private static final String re1="((N|S)\\s+OF\\s+([NS]\\d{2,4})(\\s+AND\\s+(W|E)\\s+OF\\s+([WE](\\d{3,5})))?)";
    private static final String re2="((W|E)\\s+OF\\s+([WE]\\d{3,5})(\\s+AND\\s+(N|S)\\s+OF\\s+([NS](\\d{2,4})))?)";

    public SigmetOutsideLatOrLon(final OccurrenceFrequency prio) {
        super("^("+re1+")|("+re2+")$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_OUTSIDE_LATLON);
        token.setParsedValue(ParsedValueName.TACGEOMETRY, match.group(0));
        token.setParsedValue(ParsedValueName.RELATIONTYPE, match.group(3));
        token.setParsedValue(ParsedValueName.RELATEDLINE, match.group(4));
        token.setParsedValue(ParsedValueName.RELATIONTYPE2, match.group(5));
        token.setParsedValue(ParsedValueName.RELATEDLINE2, match.group(6));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            return Optional.empty();
        }
    }
}
