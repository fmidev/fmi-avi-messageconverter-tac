package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;

public class NoSWXAvailable extends RegexMatchingLexemeVisitor {
    public NoSWXAvailable(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^NOT AVBL$", prio);
    }

    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.NO_SWX_AVAILABLE);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                Integer index = (Integer) ctx.getHints().get(ConversionHints.KEY_SWX_ANALYSIS_INDEX);
                if (index == null) {
                    throw new SerializingException("Conversion hint KEY_SWX_ANALYSIS_INDEX has not been set");
                }

                SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index);

                if (analysis.isNoInformationAvailable()) {
                    retval = Optional.of(this.createLexeme("NOT AVBL", LexemeIdentity.NO_SWX_AVAILABLE));
                }
            }
            return retval;
        }
    }
}
