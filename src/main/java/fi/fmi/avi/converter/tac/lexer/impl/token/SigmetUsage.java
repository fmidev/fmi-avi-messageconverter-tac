package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TESTOREXERCISE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_USAGE;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetUsage extends RegexMatchingLexemeVisitor {

    public SigmetUsage(final OccurrenceFrequency prio) {
        super("^(TEST|EXER)$");
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_USAGE);
        token.setParsedValue(TESTOREXERCISE, match.group(1));
        return;
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (((AviationWeatherMessage)msg).getPermissibleUsageReason().isPresent()) {
                if (AviationCodeListUser.PermissibleUsageReason.EXERCISE == ((AviationWeatherMessage) msg).getPermissibleUsageReason().get()) {
                    return Optional.of(this.createLexeme("EXER", SIGMET_USAGE));
                } else if (AviationCodeListUser.PermissibleUsageReason.TEST == ((AviationWeatherMessage) msg).getPermissibleUsageReason().get()) {
                    return Optional.of(this.createLexeme("TEST", SIGMET_USAGE));
                }
            }
            return Optional.empty();
        }
    }
}
