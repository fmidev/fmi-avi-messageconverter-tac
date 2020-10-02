package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

/**
 * Created by rinne on 10/02/17.
 */
public class Whitespace extends RegexMatchingLexemeVisitor {

    public Whitespace(final OccurrenceFrequency prio) {
        super("^\\s+$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (LexemeIdentity.WHITE_SPACE.equals(token.getIdentityIfAcceptable())) {
            return;
        }
        token.identify(LexemeIdentity.WHITE_SPACE);
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, token.getTACToken());
    }
}
