package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AerodromeWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.MeteorologicalBulletin;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;

/**
 * Created by rinne on 10/02/17.
 */
public class IssueTime extends TimeHandlingRegex {

    public IssueTime(final Priority prio) {
        super("^([0-9]{2})([0-9]{2})([0-9]{2})Z?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() && (token.getPrevious().getIdentity() == AERODROME_DESIGNATOR
                || token.getPrevious().getIdentity() == BULLETIN_HEADING_LOCATION_INDICATOR)) {
            int date = Integer.parseInt(match.group(1));
            int hour = Integer.parseInt(match.group(2));
            int minute = Integer.parseInt(match.group(3));
            if (timeOkDayHourMinute(date, hour, minute)) {
                token.identify(ISSUE_TIME);
                token.setParsedValue(DAY1, Integer.valueOf(date));
                token.setParsedValue(HOUR1, Integer.valueOf(hour));
                token.setParsedValue(MINUTE1, Integer.valueOf(minute));
            } else {
                token.identify(ISSUE_TIME, Lexeme.Status.SYNTAX_ERROR, "Invalid date & time values");
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            PartialOrCompleteTimeInstant time;
            if (AerodromeWeatherMessage.class.isAssignableFrom(clz)) {
                time = ((AerodromeWeatherMessage) msg).getIssueTime();
            } else if (MeteorologicalBulletin.class.isAssignableFrom(clz)) {
                time = ((MeteorologicalBulletin) msg).getHeading().getIssueTime();
            } else {
                return Optional.empty();
            }
            String format;
            if (MeteorologicalBulletin.class.isAssignableFrom(clz)) {
                format = "%02d%02d%02d";
            } else {
                format = "%02d%02d%02dZ";
            }
            return Optional.of(this.createLexeme(String.format(format, time.getDay().orElse(-1), time.getHour().orElse(-1), time.getMinute().orElse(-1)),
                            Lexeme.Identity.ISSUE_TIME));
        }
    }

}
