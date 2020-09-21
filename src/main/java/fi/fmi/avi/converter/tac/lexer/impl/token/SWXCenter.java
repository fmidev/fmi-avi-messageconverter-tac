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
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.IssuingCenter;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

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

            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                IssuingCenter center = ((SpaceWeatherAdvisory) msg).getIssuingCenter();

                if (!center.getName().isPresent()) {
                    throw new SerializingException("Issuing center name is missing");
                }
                StringBuilder builder = new StringBuilder();

                builder.append(center.getName().get());
                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.SWX_CENTRE));
            }
            return retval;
        }
    }
}
