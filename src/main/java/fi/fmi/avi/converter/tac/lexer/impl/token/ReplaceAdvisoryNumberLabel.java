package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class ReplaceAdvisoryNumberLabel extends RegexMatchingLexemeVisitor {
    public ReplaceAdvisoryNumberLabel(final OccurrenceFrequency prio) {
        super("^NR\\sRPLC\\s:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                if (((SpaceWeatherAdvisory) msg).getReplaceAdvisoryNumber().isPresent()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("NR RPLC :");

                    retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.REPLACE_ADVISORY_NUMBER));
                }
            }
            return retval;
        }
    }

}
