package fi.fmi.avi.converter.tac.lexer.impl.token;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;

public class AIREPStart extends PrioritizedLexemeVisitor {
    public AIREPStart(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getFirst().equals(token) && "AIREP".equalsIgnoreCase(token.getTACToken())) {
            token.identify(Lexeme.Identity.AIREP_START);
        }
    }
}