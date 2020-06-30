package fi.fmi.avi.converter.tac.lexer.impl.token;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;

public class WXREPStart extends PrioritizedLexemeVisitor {
    public static final LexemeIdentity WXREP_START = new LexemeIdentity("WXREP_START");

    public WXREPStart(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getFirst().equals(token) && "WXREP".equalsIgnoreCase(token.getTACToken())) {
            token.identify(WXREP_START);
        }
    }
}
