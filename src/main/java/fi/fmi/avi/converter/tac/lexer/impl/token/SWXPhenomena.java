package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;

/**
 * Created by rinne on 10/02/17.
 */
public class SWXPhenomena extends TimeHandlingRegex {

    public SWXPhenomena(final Priority prio) {
        super("^(?<type>OBS|FCST)\\s+SWX\\s*(\\+(?<offset>[0-9]{1,2})\\s+HR)?:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(Lexeme.Identity.SPACE_WEATHER_PHENOMENA_LABEL);
        token.setParsedValue(Lexeme.ParsedValueName.TYPE, match.group("type")); //define enum for these?
        if (match.group("offset") != null) {
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, match.group("offset"));
        }
    }

}
