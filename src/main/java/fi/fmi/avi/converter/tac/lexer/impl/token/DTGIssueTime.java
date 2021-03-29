package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

/**
 * Created by rinne on 10/02/17.
 */
public class DTGIssueTime extends TimeHandlingRegex {

    public DTGIssueTime(final OccurrenceFrequency prio) {
        super("^(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})/(?<hour>[0-9]{2})(?<minute>[0-9]{2})Z$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && token.getPrevious().getIdentity().equals(LexemeIdentity.DTG_ISSUE_TIME_LABEL)) {
                final int year = Integer.parseInt(match.group("year"));
                final int month = Integer.parseInt(match.group("month"));
                final int day = Integer.parseInt(match.group("day"));
                final int hour = Integer.parseInt(match.group("hour"));
                final int minute = Integer.parseInt(match.group("minute"));
                try {
                    ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Z"));
                    token.identify(LexemeIdentity.ISSUE_TIME);
                    token.setParsedValue(Lexeme.ParsedValueName.YEAR, year);
                    token.setParsedValue(Lexeme.ParsedValueName.MONTH, month);
                    token.setParsedValue(Lexeme.ParsedValueName.DAY1, day);
                    token.setParsedValue(Lexeme.ParsedValueName.HOUR1, hour);
                    token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, minute);
                } catch (DateTimeException e) {
                    // NOOP, ignore silently if the issue time is not valid
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                SpaceWeatherAdvisory swx = ((SpaceWeatherAdvisory) msg);
                if (swx.getIssueTime().isPresent()) {
                    PartialOrCompleteTimeInstant time = swx.getIssueTime().get();
                    StringBuilder builder = new StringBuilder();
                    builder.append(time.getCompleteTime().get().format(DateTimeFormatter.ofPattern("yyyyMMdd/HHmm'Z'")));
                    retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.ISSUE_TIME));
                }
            }
            return retval;
        }
    }

}
