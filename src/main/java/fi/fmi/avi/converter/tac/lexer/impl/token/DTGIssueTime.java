package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;

/**
 * Created by rinne on 10/02/17.
 */
public class DTGIssueTime extends TimeHandlingRegex {

    public DTGIssueTime(final Priority prio) {
        super("^DTG:\\s+(?<year>[0-9]{4})(?<month>[0-9]{2})(?<day>[0-9]{2})/(?<hour>[0-9]{2})(?<minute>[0-9]{2})Z$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final int year = Integer.parseInt(match.group("year"));
        final int month = Integer.parseInt(match.group("month"));
        final int day = Integer.parseInt(match.group("day"));
        final int hour = Integer.parseInt(match.group("hour"));
        final int minute = Integer.parseInt(match.group("minute"));
        try {
            YearMonth yearMonth = YearMonth.of(year, month);
            if (timeOkDayHourMinute(day, hour, minute)) {
                token.identify(Lexeme.Identity.ISSUE_TIME);
                token.setParsedValue(Lexeme.ParsedValueName.YEAR, year);
                token.setParsedValue(Lexeme.ParsedValueName.MONTH, month);
                token.setParsedValue(Lexeme.ParsedValueName.DAY1, day);
                token.setParsedValue(Lexeme.ParsedValueName.HOUR1, hour);
                token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, minute);
            }
        } catch (DateTimeException e) {
            //NOOP, ignore silently if year & month not valid
        }
    }

}
