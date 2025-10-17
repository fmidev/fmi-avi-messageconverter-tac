package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;

import java.util.Optional;
import java.util.regex.Matcher;

public class ReplaceAdvisoryNumberLabel extends RegexMatchingLexemeVisitor {
    public ReplaceAdvisoryNumberLabel(final OccurrenceFrequency prio) {
        super("^NR\\sRPLC\\s?:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                if (((SpaceWeatherAdvisoryAmd79) msg).getReplaceAdvisoryNumber().isPresent()) {
                    retval = Optional.of(this.createLexeme("NR RPLC:", LexemeIdentity.REPLACE_ADVISORY_NUMBER));
                }
            }
            return retval;
        }
    }

}
