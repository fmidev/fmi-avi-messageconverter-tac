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
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class AdvisoryStatus extends RegexMatchingLexemeVisitor {
    public AdvisoryStatus(final OccurrenceFrequency prio) {
        super("^(?<status>TEST|EXERCISE){1}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.getPrevious().getIdentity().equals(LexemeIdentity.TEST_OR_EXCERCISE_LABEL)) {
            token.identify(LexemeIdentity.TEST_OR_EXCERCISE);
            String status = match.group("status");

            if (status.equals(AviationCodeListUser.PermissibleUsageReason.TEST.toString())) {
                token.setParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.TEST);
            } else {
                token.setParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.EXERCISE);
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                SpaceWeatherAdvisory advisory = (SpaceWeatherAdvisory) msg;

                if (advisory.getPermissibleUsageReason().isPresent()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append(advisory.getPermissibleUsageReason().get().toString());

                    retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.TEST_OR_EXCERCISE));
                }
            }
            return retval;
        }
    }
}
