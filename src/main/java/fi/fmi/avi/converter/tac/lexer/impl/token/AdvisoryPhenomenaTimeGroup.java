package fi.fmi.avi.converter.tac.lexer.impl.token;




import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

public class AdvisoryPhenomenaTimeGroup extends TimeHandlingRegex {

    public AdvisoryPhenomenaTimeGroup(final Priority prio) {
        super("^(?<day>[0-9]{2})/(?<hour>[0-9]{2})(?<minute>[0-9]{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() && token.getPrevious().getIdentity() == Identity.SPACE_WEATHER_PHENOMENA_LABEL) {
            final int day = Integer.parseInt(match.group("day"));
            final int hour = Integer.parseInt(match.group("hour"));
            final int minute = Integer.parseInt(match.group("minute"));
            if (timeOkDayHourMinute(day, hour,minute)) {
                token.identify(Identity.ADVISORY_PHENOMENA_TIME_GROUP);
                token.setParsedValue(Lexeme.ParsedValueName.DAY1, day);
                token.setParsedValue(Lexeme.ParsedValueName.HOUR1, hour);
                token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, minute);
            }


        }
    }

}
