package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;

/**
 * Created by rinne on 18/01/17.
 */
public abstract class RegexMatchingLexemeVisitor extends PrioritizedLexemeVisitor {

    private final Pattern pattern;

    protected RegexMatchingLexemeVisitor(final String pattern) {
        this(pattern, OccurrenceFrequency.AVERAGE);
    }

    protected RegexMatchingLexemeVisitor(final String pattern, final OccurrenceFrequency priority) {
        super(priority);
        this.pattern = Pattern.compile(pattern);
    }

    public static boolean hasPreceedingLexeme(final Lexeme token, final LexemeIdentity id) {
        Lexeme previous = token.getPrevious();

        while (previous != null) {
            if (previous.getIdentity().equals(id)) {
                return true;
            }
            previous = previous.getPrevious();
        }
        return false;
    }

    public Pattern getPattern() {
        return pattern;
    }

    @Override
    public final void visit(final Lexeme token, final ConversionHints hints) {
        final Matcher m = this.pattern.matcher(token.getTACToken());
        if (m.matches()) {
            this.visitIfMatched(token, m, hints);
        }
    }

    public abstract void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints);
}
