package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.Optional;
import java.util.regex.Matcher;

public class SWXIntensity extends RegexMatchingLexemeVisitor {
    public SWXIntensity(final OccurrenceFrequency prio) {
        super("^(?<intensity>MOD|SEV)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_INTENSITY);
        token.setParsedValue(Lexeme.ParsedValueName.INTENSITY, match.group("intensity"));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(
                final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) throws SerializingException {
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                return getAsLexeme((SpaceWeatherAdvisoryAmd82) msg, ctx);
            }
            return Optional.empty();
        }

        private <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(
                final SpaceWeatherAdvisoryAmd82 msg, final ReconstructorContext<T> ctx) throws SerializingException {
            final String intensityCode = msg.getAnalyses()
                    .get(ctx.getMandatoryParameter("analysisIndex", Integer.class))
                    .getIntensityAndRegions()
                    .get(ctx.getMandatoryParameter("intensityAndRegionIndex", Integer.class))
                    .getIntensity()
                    .getCode();
            return Optional.of(createLexeme(intensityCode, LexemeIdentity.SWX_INTENSITY));
        }
    }
}
