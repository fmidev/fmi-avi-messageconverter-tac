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

public class SpaceWeatherCenter extends RegexMatchingLexemeVisitor {
    public SpaceWeatherCenter(final OccurrenceFrequency prio) {
        super("^SWXC\\:\\s{1}(?<issuer>[A-Z a-z 0-9]*)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SPACE_WEATHER_CENTRE);
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, match.group("issuer"));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if(SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                IssuingCenter center = ((SpaceWeatherAdvisory)msg).getIssuingCenter();

                if(!center.getType().isPresent()) {
                    throw new SerializingException("Issuing center name is missing");
                }

                if(!center.getDesignator().isPresent()) {
                    throw new SerializingException("Issuing center designator is missing");
                }
                //TODO: add handling for removing unwaned stuff from type (OTHER:SWXC should be SWXC)
                StringBuilder builder = new StringBuilder();
                builder.append(center.getType().get());
                builder.append(": ");
                builder.append(center.getDesignator().get());
                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.SPACE_WEATHER_CENTRE));
            }
            return retval;
        }
    }
}
