package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_INTENSITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetIntensity extends RegexMatchingLexemeVisitor {

    public SigmetIntensity(final OccurrenceFrequency prio) {
        super("^(INTSF|WKN|NC)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_INTENSITY);
        token.setParsedValue(INTENSITY, match.group(0));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sm = (SIGMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    switch (sm.getAnalysisGeometries().get().get(analysisIndex.get()).getIntensityChange().get()) {
                    case NO_CHANGE:
                        return Optional.of(createLexeme("NC", SIGMET_INTENSITY));
                    case WEAKENING:
                        return Optional.of(createLexeme("WKN", SIGMET_INTENSITY));
                    case INTENSIFYING:
                        return Optional.of(createLexeme("INTSF", SIGMET_INTENSITY));
                    }
                }
            }
            if (AIRMET.class.isAssignableFrom(clz)) {
                AIRMET am = (AIRMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    switch (am.getAnalysisGeometries().get().get(analysisIndex.get()).getIntensityChange().get()) {
                    case NO_CHANGE:
                        return Optional.of(createLexeme("NC", SIGMET_INTENSITY));
                    case WEAKENING:
                        return Optional.of(createLexeme("WKN", SIGMET_INTENSITY));
                    case INTENSIFYING:
                        return Optional.of(createLexeme("INTSF", SIGMET_INTENSITY));
                    }
                }
            }
            return Optional.empty();
        }
    }
}
