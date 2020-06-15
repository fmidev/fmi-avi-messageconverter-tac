package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;

public class NextAdvisory extends TimeHandlingRegex {
    public NextAdvisory(final Priority prio) {
        super("NXT\\sADVISORY\\:\\s(?<type>NO\\sFURTHER\\sADVISORIES|WILL\\sBE\\sISSUED\\sBY)?\\s?((?<year>[0-9]{4})(?<month>[0-1][0-9])(?<day>[0-3][0-9])\\/"
                + "(?<hour>[0-2][0-9])(?<minute>[0-5][0-9])Z)?", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.NEXT_ADVISORY);

        String type = match.group("type");
        if (type == null) {
            token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_AT);
            setParsedDateValues(token, match, hints);
        } else if (type.equals("WILL BE ISSUED BY")) {
            token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_BY);
            setParsedDateValues(token, match, hints);
        } else {
            token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES);
        }
    }

    private void setParsedDateValues(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.setParsedValue(Lexeme.ParsedValueName.YEAR, Integer.parseInt(match.group("year")));
        token.setParsedValue(Lexeme.ParsedValueName.MONTH, Integer.parseInt(match.group("month")));
        token.setParsedValue(Lexeme.ParsedValueName.DAY1, Integer.parseInt(match.group("day")));
        token.setParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.parseInt(match.group("hour")));
        token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.parseInt(match.group("minute")));

    }
}
