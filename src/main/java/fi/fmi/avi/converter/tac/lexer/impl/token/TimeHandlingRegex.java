package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

/**
 * Created by rinne on 22/02/17.
 */
public abstract class TimeHandlingRegex extends RegexMatchingLexemeVisitor {

    protected TimeHandlingRegex(final String pattern, final OccurrenceFrequency priority) {
        super(pattern, priority);
    }

    static boolean timeOkDayHour(final int date, final int hour) {
        return timeOkDayHourMinute(date, hour, -1);
    }

    static boolean timeOkHour(final int hour) {
        return timeOkDayHourMinute(-1, hour, -1);
    }

    static boolean timeOkHourMinute(final int hour, final int minute) {
        return timeOkDayHourMinute(-1, hour, minute);
    }

    static boolean timeOkDayHourMinute(final int date, final int hour, final int minute) {
        if (date != 0 && date < 32 && hour < 25 && minute < 60) {
            return hour != 24 || minute <= 0;
        } else {
            return false;
        }
    }
}
