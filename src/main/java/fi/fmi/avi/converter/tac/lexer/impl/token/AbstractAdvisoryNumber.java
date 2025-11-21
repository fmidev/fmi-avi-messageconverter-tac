package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

import java.util.regex.Matcher;

import static java.util.Objects.requireNonNull;

public abstract class AbstractAdvisoryNumber extends RegexMatchingLexemeVisitor {
    protected final LexemeIdentity lexemeIdentity;
    protected final LexemeIdentity previousLexemeIdentity;

    protected AbstractAdvisoryNumber(final OccurrenceFrequency priority, final LexemeIdentity lexemeIdentity, final LexemeIdentity previousLexemeIdentity) {
        super("^(?<year>[0-9]{4})/(?<serialNo>[0-9]{1,4})$", priority);
        this.lexemeIdentity = requireNonNull(lexemeIdentity, "lexemeIdentity");
        this.previousLexemeIdentity = requireNonNull(previousLexemeIdentity, "previousLexemeIdentity");
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious() && token.getPrevious().getIdentity() != null) {
            if (previousLexemeIdentityMatches(token.getPrevious().getIdentity())) {
                token.identify(lexemeIdentity);

                token.setParsedValue(Lexeme.ParsedValueName.YEAR, Integer.parseInt(match.group("year")));
                token.setParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, Integer.parseInt(match.group("serialNo")));
            }
        }
    }

    protected boolean previousLexemeIdentityMatches(final LexemeIdentity previousIdentity) {
        return previousIdentity.equals(this.previousLexemeIdentity);
    }

    public static abstract class AbstractReconstructor extends FactoryBasedReconstructor {
        protected final LexemeIdentity lexemeIdentity;

        protected AbstractReconstructor(final LexemeIdentity lexemeIdentity) {
            this.lexemeIdentity = requireNonNull(lexemeIdentity, "lexemeIdentity");
        }

        protected static String toAdvisoryNumberString(final int year, final int serialNumber)
                throws SerializingException {
            if (serialNumber <= 0) {
                throw new SerializingException("The advisory number is missing the serial number");
            }
            if (year <= 0) {
                throw new SerializingException("The advisory number is missing the year");
            }
            return year + "/" + serialNumber;
        }

        protected Lexeme createAdvisoryNumberLexeme(final int year, final int serial) throws SerializingException {
            return this.createLexeme(toAdvisoryNumberString(year, serial), lexemeIdentity);
        }
    }
}
