package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE2;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;

public class SigmetValidTime extends TimeHandlingRegex {

    public SigmetValidTime(final OccurrenceFrequency prio) {
        super("^VALID\\s(?<startDay>[0-9]{2})(?<startHour>[0-9]{2})(?<startMinute>[0-9]{2})[/-](?<endDay>[0-9]{2})(?<endHour>[0-9]{2})"
                + "(?<endMinute>[0-9]{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final int fromDay = Integer.parseInt(match.group("startDay"));
        final int fromHour = Integer.parseInt(match.group("startHour"));
        final int fromMinute = Integer.parseInt(match.group("startMinute"));
        final int toDay = Integer.parseInt(match.group("endDay"));
        final int toHour = Integer.parseInt(match.group("endHour"));
        final int toMinute = Integer.parseInt(match.group("endMinute"));
        if (timeOkDayHourMinute(fromDay, fromHour, fromMinute) && timeOkDayHourMinute(toDay, toHour, toMinute)) {
            token.identify(LexemeIdentity.VALID_TIME);
            token.setParsedValue(DAY1, fromDay);
            token.setParsedValue(DAY2, toDay);
            token.setParsedValue(HOUR1, fromHour);
            token.setParsedValue(HOUR2, toHour);
            token.setParsedValue(MINUTE1, fromMinute);
            token.setParsedValue(MINUTE2, toMinute);
        } else {
            token.identify(LexemeIdentity.VALID_TIME, Lexeme.Status.SYNTAX_ERROR, "Invalid date and/or time");
        }
    }
}
