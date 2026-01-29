package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_LINE;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetLine extends RegexMatchingLexemeVisitor {

    public SigmetLine(final OccurrenceFrequency prio) {
        super("^(N|NE|E|SE|S|SW|W|NW)\\s+OF\\s+LINE\\s+([NS]\\d{2,4}\\s+[EW]\\d{3,5})\\s+-\\s+([NS]\\d{2,4}\\s+[EW]\\d{3,5})(\\s+-\\s+([NS]\\d{2,4}\\s+[EW]\\d{3,5}))?(\\s+-\\s+([NS]\\d{2,4}\\s+[EW]\\d{3,5}))?$", prio);
        //       1                                   2                                   3                            4       5                              6       7
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_LINE);
        token.setParsedValue(ParsedValueName.RELATIONTYPE, match.group(1));
        token.setParsedValue(ParsedValueName.LINE_POINT1, match.group(2));
        token.setParsedValue(ParsedValueName.LINE_POINT2, match.group(3));
        token.setParsedValue(ParsedValueName.LINE_POINT3, match.group(5));
        token.setParsedValue(ParsedValueName.LINE_POINT4, match.group(7));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                return Optional.of(createLexeme("N/S/TODO: OF LINE", SIGMET_LINE));
            }
            return Optional.empty();
        }
    }
}
