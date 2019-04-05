package fi.fmi.avi.converter.tac.lexer.impl.token;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;

public class WXWarningStart extends PrioritizedLexemeVisitor {
    public WXWarningStart(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getFirst().equals(token) && "WX WRNG".equalsIgnoreCase(token.getTACToken())) {
            token.identify(Lexeme.Identity.WX_WARNING_START);
        }
    }
}
