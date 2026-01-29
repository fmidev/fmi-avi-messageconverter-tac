package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.regex.Matcher;

public class ReplaceAdvisoryNumberLabel extends RegexMatchingLexemeVisitor {
    public ReplaceAdvisoryNumberLabel(final OccurrenceFrequency prio) {
        super("^NR\\s+RPLC\\s*:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
    }

    public static class Reconstructor extends AbstractFixedContentReconstructor {
        public Reconstructor() {
            super("NR RPLC:", LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
        }

        @Override
        protected <T extends AviationWeatherMessageOrCollection> boolean isReconstructable(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            return SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz) && !((SpaceWeatherAdvisoryAmd82) msg).getReplaceAdvisoryNumbers().isEmpty()
                    || SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz) && ((SpaceWeatherAdvisoryAmd79) msg).getReplaceAdvisoryNumber().isPresent();
        }
    }

}
