package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis;

import java.util.Optional;
import java.util.regex.Matcher;

public class SWXNotAvailable extends RegexMatchingLexemeVisitor {
    public SWXNotAvailable(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^NOT\\sAVBL", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_NOT_AVAILABLE);

    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisoryAmd79) msg).getAnalyses().get(analysisIndex.get());
                    if (analysis.getNilPhenomenonReason().isPresent()) {
                        if (analysis.getNilPhenomenonReason().get().equals(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE)) {
                            retval = Optional.of(this.createLexeme("NOT AVBL", LexemeIdentity.SWX_NOT_AVAILABLE));
                        }
                    }
                }
            }
            return retval;
        }
    }
}
