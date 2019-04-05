package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SIGMET;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;


/**
 * Created by rinne on 10/02/17.
 */
public class Sigmet extends PrioritizedLexemeVisitor {

    public Sigmet(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("SIGMET".equalsIgnoreCase(token.getTACToken())) {
            token.identify(SIGMET);
        }
    }
}
