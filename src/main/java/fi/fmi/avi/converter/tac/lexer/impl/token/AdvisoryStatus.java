package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.Optional;
import java.util.regex.Matcher;

public class AdvisoryStatus extends RegexMatchingLexemeVisitor {
    public AdvisoryStatus(final OccurrenceFrequency prio) {
        super("^(?<status>TEST|EXER){1}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && token.getPrevious().getIdentity().equals(LexemeIdentity.ADVISORY_STATUS_LABEL)) {
                token.identify(LexemeIdentity.ADVISORY_STATUS);
                final String status = match.group("status");

                if (status.equals(AviationCodeListUser.PermissibleUsageReason.TEST.toString())) {
                    token.setParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.TEST);
                } else {
                    token.setParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.EXERCISE);
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        private static String getLexemeValue(final AviationCodeListUser.PermissibleUsageReason permissibleUsageReason) {
            final StringBuilder builder = new StringBuilder();
            if (permissibleUsageReason == AviationCodeListUser.PermissibleUsageReason.EXERCISE) {
                builder.append("EXER");
            } else {
                builder.append(permissibleUsageReason);
            }
            return builder.toString();
        }

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                retval = ((SpaceWeatherAdvisoryAmd82) msg).getPermissibleUsageReason()
                        .map(permissibleUsageReason -> createLexeme(getLexemeValue(permissibleUsageReason), LexemeIdentity.ADVISORY_STATUS));
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                retval = ((SpaceWeatherAdvisoryAmd79) msg).getPermissibleUsageReason()
                        .map(permissibleUsageReason -> createLexeme(getLexemeValue(permissibleUsageReason), LexemeIdentity.ADVISORY_STATUS));
            }
            return retval;
        }
    }
}
