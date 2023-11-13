package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_VA_NAME;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetVaName extends RegexMatchingLexemeVisitor {
    public SigmetVaName(final OccurrenceFrequency prio) {
        super("^MT (.*)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (SIGMET_START.equals(token.getFirst().getIdentity())){
            token.identify(SIGMET_VA_NAME);
            token.setParsedValue(VALUE, match.group(1));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET) msg;
                if (AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA.equals(sigmet.getPhenomenon().orElse(null))) {
                    if (sigmet.getVAInfo().isPresent()) {
                        if (sigmet.getVAInfo().isPresent() && sigmet.getVAInfo().get().getVolcano().isPresent()
                                && sigmet.getVAInfo().get().getVolcano().get().getVolcanoName().isPresent()) {
                            String volcanoName = sigmet.getVAInfo().get().getVolcano().get().getVolcanoName().get();
                            if (volcanoName.length() > 0) {
                                return Optional.of(this.createLexeme("MT " + volcanoName, LexemeIdentity.SIGMET_VA_NAME));
                            }
                        }
                    }
                    return Optional.empty();
                }

            }
            return Optional.empty();
        }

    }
}
