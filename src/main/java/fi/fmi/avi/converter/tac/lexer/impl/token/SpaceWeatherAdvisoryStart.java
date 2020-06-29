package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

public class SpaceWeatherAdvisoryStart extends RegexMatchingLexemeVisitor {
    public SpaceWeatherAdvisoryStart(final OccurrenceFrequency prio) {
        super("^SWX\\s+ADVISORY$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.getFirst().equals(token)) {
            token.identify(LexemeIdentity.SPACE_WEATHER_ADVISORY_START);
        }
    }
}
