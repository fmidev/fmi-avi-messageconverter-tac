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

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_APRX_LINE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.APRX_LINE_WIDTH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.APRX_LINE_WIDTH_UNIT;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetAprx extends RegexMatchingLexemeVisitor {

    public SigmetAprx(final OccurrenceFrequency prio) {
        super("^APRX\\s(\\d{2})(KM|NM)\\sWID\\sLINE\\sBTN", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_APRX_LINE);
        token.setParsedValue(APRX_LINE_WIDTH, match.group(1));
        token.setParsedValue(APRX_LINE_WIDTH_UNIT, match.group(2));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                return Optional.empty();
            }
            return Optional.empty();
        }
    }
}
