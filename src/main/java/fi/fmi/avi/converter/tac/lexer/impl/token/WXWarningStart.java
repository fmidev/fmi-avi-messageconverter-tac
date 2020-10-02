package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

public class WXWarningStart extends RegexMatchingLexemeVisitor {
    public static final LexemeIdentity WX_WARNING_START = new LexemeIdentity("WX_WARNING_START");

    public WXWarningStart(final OccurrenceFrequency prio) {
        super("^WX\\s+WRNG$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.getFirst().equals(token)) {
            token.identify(WX_WARNING_START);
        }
    }
}
