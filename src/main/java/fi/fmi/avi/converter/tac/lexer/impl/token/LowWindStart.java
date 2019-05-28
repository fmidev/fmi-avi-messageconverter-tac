package fi.fmi.avi.converter.tac.lexer.impl.token;


import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

public class LowWindStart extends RegexMatchingLexemeVisitor {
    public static final LexemeIdentity LOW_WIND_START = new LexemeIdentity("LOW_WIND_START");

    public LowWindStart(final PrioritizedLexemeVisitor.Priority prio) {
        super("^LOW\\s+WIND$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.getFirst().equals(token)) {
            token.identify(LOW_WIND_START);
        }
    }
}
