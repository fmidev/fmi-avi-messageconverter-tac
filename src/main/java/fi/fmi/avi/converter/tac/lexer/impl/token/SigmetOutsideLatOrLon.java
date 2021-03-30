package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_OUTSIDE_LATLON;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetOutsideLatOrLon extends RegexMatchingLexemeVisitor {

    //"^(N|E|S|W)\\sOF\\s([NESW])(\\d+)$"
    public SigmetOutsideLatOrLon(final OccurrenceFrequency prio) {
        super("^(N|E|S|W)\\sOF\\s([NESW])(\\d+)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_OUTSIDE_LATLON);
        token.setParsedValue(VALUE, match.group(3));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                return Optional.of(createLexeme("SIGMET_OUTSIDE_LATLON", SIGMET_OUTSIDE_LATLON));
            }
            return Optional.empty();
        }
    }
}
