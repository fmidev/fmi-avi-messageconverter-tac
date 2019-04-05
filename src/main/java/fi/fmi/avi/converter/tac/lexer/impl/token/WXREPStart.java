package fi.fmi.avi.converter.tac.lexer.impl.token;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;

public class WXREPStart extends PrioritizedLexemeVisitor {
    public WXREPStart(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getFirst().equals(token) && "WXREP".equalsIgnoreCase(token.getTACToken())) {
            token.identify(Lexeme.Identity.WXREP_START);
        }
    }
}
