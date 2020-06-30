package fi.fmi.avi.converter.tac.lexer.impl.token;




import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class AdvisoryPhenomenaTimeGroup extends TimeHandlingRegex {

    public AdvisoryPhenomenaTimeGroup(final OccurrenceFrequency prio) {
        super("^(?<day>[0-9]{2})/(?<hour>[0-9]{2})(?<minute>[0-9]{2})Z$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() &&  LexemeIdentity.ADVISORY_PHENOMENA_LABEL.equals(token.getPrevious().getIdentity())) {
            final int day = Integer.parseInt(match.group("day"));
            final int hour = Integer.parseInt(match.group("hour"));
            final int minute = Integer.parseInt(match.group("minute"));
            if (timeOkDayHourMinute(day, hour,minute)) {
                token.identify(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
                token.setParsedValue(Lexeme.ParsedValueName.DAY1, day);
                token.setParsedValue(Lexeme.ParsedValueName.HOUR1, hour);
                token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, minute);
            }


        }
    }
    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if(SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {

            }
            return retval;
        }
    }

}
