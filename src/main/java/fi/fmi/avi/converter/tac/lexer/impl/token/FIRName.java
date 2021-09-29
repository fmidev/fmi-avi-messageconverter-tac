package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;


/**
 * Created by rinne on 10/02/17.
 */
public class FIRName extends RegexMatchingLexemeVisitor {

    public FIRName(final OccurrenceFrequency prio) {
        super("^(FIR|UIR|FIR/UIR|CTA)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() && LexemeIdentity.SIGMET_FIR_NAME_WORD.equals(token.getPrevious().getIdentity()) && !match.group(1).equals("ENTIRE")) {
            token.identify(LexemeIdentity.FIR_NAME);
            String firName = token.getPrevious().getTACToken();
            if (token.getPrevious().hasPrevious()&&LexemeIdentity.SIGMET_FIR_NAME_WORD.equals(token.getPrevious().getPrevious().getIdentity())) {
                firName = token.getPrevious().getPrevious().getTACToken() + " " + firName;
            }
            firName = firName + " "+ match.group(1);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, firName);
            token.setParsedValue(Lexeme.ParsedValueName.FIR_TYPE, match.group(1));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET m = (SIGMET) msg;
                String firName = m.getAirspace().getName();
                return Optional.of(this.createLexeme(firName, LexemeIdentity.FIR_NAME));
            }
            if (AIRMET.class.isAssignableFrom(clz)) {
                AIRMET m = (AIRMET) msg;
                String firName = m.getAirspace().getName();
                return Optional.of(this.createLexeme(firName, LexemeIdentity.FIR_NAME));
            }
            return Optional.empty();
        }
    }
}
