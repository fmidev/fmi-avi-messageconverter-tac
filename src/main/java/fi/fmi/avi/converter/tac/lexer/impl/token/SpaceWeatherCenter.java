package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;

public class SpaceWeatherCenter extends TimeHandlingRegex {
    public SpaceWeatherCenter(final Priority prio) {
        super("^SWXC\\:\\s{1}(?<issuer>[A-Z a-z 0-9]*)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SPACE_WEATHER_CENTRE);
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, match.group("issuer"));
    }
}