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

    private Pattern pattern;

    public RegexMatchingLexemeVisitor(final String pattern) {
        this(pattern, OccurrenceFrequency.AVERAGE);
    }

    public RegexMatchingLexemeVisitor(final String pattern, final OccurrenceFrequency priority) {
        super(priority);
        this.pattern = Pattern.compile(pattern);
    }

    public Pattern getPattern() {
		return pattern;
	}
    
    @Override
    public final void visit(final Lexeme token, final ConversionHints hints) {
        Matcher m = this.pattern.matcher(token.getTACToken());
        if (m.matches()) {
            this.visitIfMatched(token, m, hints);
        }
    }

    public abstract void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints);

    public static boolean hasPreceedingLexeme(Lexeme token, LexemeIdentity id) {
        Lexeme previous = token.getPrevious();

        while(previous != null) {
            if (previous.getIdentity().equals(id)) {
                return true;
            }
            previous = previous.getPrevious();
        }
        return false;
    }

    /**
     * @deprecated This does exactly the same as Lexeme.getPrevious(), so should not be used
     */
    public static Lexeme getPreviousToken(Lexeme token) {
        Lexeme previous = token.getPrevious();
        try {
            while (previous != null && previous.getIdentity() == null) {
                previous = previous.getPrevious();
            }
        }catch(Exception e) {
            System.out.println("asdfg");
        }
        return previous;
    }

}
