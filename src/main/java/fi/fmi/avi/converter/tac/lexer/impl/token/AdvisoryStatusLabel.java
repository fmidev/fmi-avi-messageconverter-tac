package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class AdvisoryStatusLabel extends RegexMatchingLexemeVisitor {
    public AdvisoryStatusLabel(final OccurrenceFrequency prio) {
        super("^(?<label>STATUS\\:)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_STATUS_LABEL);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final SpaceWeatherAdvisory advisory = (SpaceWeatherAdvisory) msg;

                if (advisory.getPermissibleUsageReason().isPresent()) {
                    if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                        retval = Optional.of(this.createLexeme("STATUS:", LexemeIdentity.ADVISORY_STATUS_LABEL));
                    }
                }
            }
            return retval;
        }
    }
}
