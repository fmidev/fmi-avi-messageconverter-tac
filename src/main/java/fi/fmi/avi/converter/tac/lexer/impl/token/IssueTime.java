package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AerodromeWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

/**
 * Created by rinne on 10/02/17.
 */
public class IssueTime extends TimeHandlingRegex {

    public IssueTime(final OccurrenceFrequency prio) {
        super("^([0-9]{2})?([0-9]{2})([0-9]{2})Z?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() && (AERODROME_DESIGNATOR.equals(token.getPrevious().getIdentity()) || BULLETIN_HEADING_LOCATION_INDICATOR.equals(
                token.getPrevious().getIdentity()) || REP.equals(token.getPrevious().getIdentity()))) {
            int date = -1;
            if (match.group(1) != null) {
                date = Integer.parseInt(match.group(1));
            }
            final int hour = Integer.parseInt(match.group(2));
            final int minute = Integer.parseInt(match.group(3));
            if (date == -1) {
                if (timeOkHourMinute(hour, minute)) {
                    token.identify(ISSUE_TIME);
                    token.setParsedValue(HOUR1, hour);
                    token.setParsedValue(MINUTE1, minute);
                } else {
                    token.identify(ISSUE_TIME, Lexeme.Status.SYNTAX_ERROR, "Invalid time values");
                }
            } else {
                if (timeOkDayHourMinute(date, hour, minute)) {
                    token.identify(ISSUE_TIME);
                    token.setParsedValue(DAY1, date);
                    token.setParsedValue(HOUR1, hour);
                    token.setParsedValue(MINUTE1, minute);
                } else {
                    token.identify(ISSUE_TIME, Lexeme.Status.SYNTAX_ERROR, "Invalid date & time values");
                }
            }

        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            return getIssueTime(msg, clz)//
                    .flatMap(issueTime -> formatIssueTime(issueTime, clz))//
                    .map(formattedIssueTime -> this.createLexeme(formattedIssueTime, LexemeIdentity.ISSUE_TIME));
        }

        private <T extends AviationWeatherMessageOrCollection> Optional<PartialOrCompleteTimeInstant> getIssueTime(final T msg, final Class<T> clz) {
            if (AviationWeatherMessage.class.isAssignableFrom(clz)) {
                return ((AerodromeWeatherMessage) msg).getIssueTime();
            } else if (MeteorologicalBulletin.class.isAssignableFrom(clz)) {
                return Optional.of(((MeteorologicalBulletin<?>) msg).getHeading().getIssueTime());
            } else {
                return Optional.empty();
            }
        }

        private <T extends AviationWeatherMessageOrCollection> Optional<String> formatIssueTime(final PartialOrCompleteTimeInstant time, final Class<T> clz) {
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz) || SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                return time.getCompleteTime()//
                        .map(completeTime -> completeTime.format(DateTimeFormatter.ofPattern("yyyyMMdd/HHmm'Z'")));
            } else {
                final String format = MeteorologicalBulletin.class.isAssignableFrom(clz) ? "%02d%02d%02d" : "%02d%02d%02dZ";
                return Optional.of(String.format(Locale.US, format, time.getDay().orElse(-1), time.getHour().orElse(-1), time.getMinute().orElse(-1)));
            }
        }
    }
}
