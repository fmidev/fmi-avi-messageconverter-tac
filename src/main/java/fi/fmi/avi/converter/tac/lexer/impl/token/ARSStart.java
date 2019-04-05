package fi.fmi.avi.converter.tac.lexer.impl.token;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;

public class ARSStart extends PrioritizedLexemeVisitor {
    public ARSStart(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getFirst().equals(token) && "ARS".equalsIgnoreCase(token.getTACToken())) {
            token.identify(Lexeme.Identity.ARS_START);
        }
    }
}
