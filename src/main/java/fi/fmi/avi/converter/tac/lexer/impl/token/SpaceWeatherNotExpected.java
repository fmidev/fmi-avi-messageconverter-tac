package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

import java.util.regex.Matcher;

public class SpaceWeatherNotExpected extends RegexMatchingLexemeVisitor {
    public SpaceWeatherNotExpected(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^NO\\sSWX\\sEXP$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.NO_SWX_EXPECTED);

    }
}
