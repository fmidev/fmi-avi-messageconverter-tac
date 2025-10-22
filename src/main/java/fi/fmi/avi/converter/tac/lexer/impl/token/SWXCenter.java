package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.IssuingCenter;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.regex.Matcher;

public class SWXCenter extends RegexMatchingLexemeVisitor {
    public SWXCenter(final OccurrenceFrequency prio) {
        super("^(?<issuer>.{3,12})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {

        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && LexemeIdentity.SWX_CENTRE_LABEL.equals(token.getPrevious().getIdentity())) {
                token.identify(LexemeIdentity.SWX_CENTRE);
                token.setParsedValue(Lexeme.ParsedValueName.VALUE, match.group("issuer"));
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd82.IssuingCenter center = ((SpaceWeatherAdvisoryAmd82) msg).getIssuingCenter();
                retval = Optional.of(createCenterLexeme(center.getName().orElse(null)));
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final IssuingCenter center = ((SpaceWeatherAdvisoryAmd79) msg).getIssuingCenter();
                retval = Optional.of(createCenterLexeme(center.getName().orElse(null)));
            }
            return retval;
        }

        private Lexeme createCenterLexeme(@Nullable final String centerName) throws SerializingException {
            if (centerName == null) {
                throw new SerializingException("Issuing center name is missing");
            }
            return this.createLexeme(centerName, LexemeIdentity.SWX_CENTRE);
        }
    }
}
