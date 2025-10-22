package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.regex.Matcher;

public class SWXNotAvailable extends RegexMatchingLexemeVisitor {
    public SWXNotAvailable(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^NOT\\sAVBL", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_NOT_AVAILABLE);

    }

    public static class Reconstructor extends AbstractFixedContentReconstructor {
        public Reconstructor() {
            super("NOT AVBL", LexemeIdentity.SWX_NOT_AVAILABLE);
        }

        @Override
        protected <T extends AviationWeatherMessageOrCollection> boolean isReconstructable(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            return SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz) && ctx.getParameter("analysisIndex", Integer.class)
                    .flatMap(analysisIndex -> ((SpaceWeatherAdvisoryAmd82) msg).getAnalyses().get(analysisIndex).getNilPhenomenonReason())
                    .filter(nilPhenomenonReason -> nilPhenomenonReason
                            .equals(fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE)).isPresent()
                    || SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz) && ctx.getParameter("analysisIndex", Integer.class)
                    .flatMap(analysisIndex -> ((SpaceWeatherAdvisoryAmd79) msg).getAnalyses().get(analysisIndex).getNilPhenomenonReason())
                    .filter(nilPhenomenonReason -> nilPhenomenonReason
                            .equals(fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE)).isPresent();
        }
    }
}
