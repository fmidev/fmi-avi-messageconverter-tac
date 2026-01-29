package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeVisitor;
import fi.fmi.avi.model.MessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

/**
 * Created by rinne on 01/02/17.
 */
public class RecognizingAviMessageTokenLexer implements LexemeVisitor {

    private final List<PrioritizedLexemeVisitor> visitors = new ArrayList<>();
    private SuitabilityTester matcher;

    public MessageType getMessageType() {
        return this.matcher.getMessageType();
    }

    public SuitabilityTester getSuitablityTester() {
        return this.matcher;
    }

    public void setSuitabilityTester(final SuitabilityTester matcher) {
        this.matcher = matcher;
    }

    public void teach(final PrioritizedLexemeVisitor lexer) {
        this.visitors.add(lexer);
        Collections.sort(this.visitors);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        for (final LexemeVisitor v : visitors) {
            if (token.getIdentificationCertainty() < 1.0) {
                token.accept(v, hints);
            } else {
                break;
            }
        }
    }

    public enum RelationalOperator {
        LESS_THAN("M"), MORE_THAN("P");

        private final String code;

        RelationalOperator(final String code) {
            this.code = code;
        }

        public static RelationalOperator forCode(final String code) {
            for (final RelationalOperator w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }
    }

    public enum TendencyOperator {
        UPWARD("U"), DOWNWARD("D"), NO_CHANGE("N");

        private final String code;

        TendencyOperator(final String code) {
            this.code = code;
        }

        public static TendencyOperator forCode(final String code) {
            for (final TendencyOperator w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }
    }

    public interface SuitabilityTester {
        static SuitabilityTester alwaysSuits(final MessageType messageType) {
            return new SuitabilityTesters.AlwaysSuits(requireNonNull(messageType, "messageType"));
        }

        static SuitabilityTester firstLexemeEquals(final String expectedLexeme, final MessageType messageType) {
            requireNonNull(expectedLexeme, "expectedLexeme");
            requireNonNull(messageType, "messageType");
            return new SuitabilityTesters.FirstLexemeEquals(expectedLexeme, messageType);
        }

        static SuitabilityTester firstLexemeMatches(final String pattern, final MessageType messageType) {
            requireNonNull(pattern, "pattern");
            requireNonNull(messageType, "messageType");
            return new SuitabilityTesters.FirstLexemeMatches(Pattern.compile(pattern), messageType);
        }

        boolean test(LexemeSequence sequence);

        MessageType getMessageType();
    }

    private static final class SuitabilityTesters {
        private SuitabilityTesters() {
            throw new AssertionError();
        }

        static class AlwaysSuits implements SuitabilityTester {
            private final MessageType messageType;

            private AlwaysSuits(final MessageType messageType) {
                this.messageType = messageType;
            }

            @Override
            public boolean test(final LexemeSequence sequence) {
                return true;
            }

            @Override
            public MessageType getMessageType() {
                return messageType;
            }
        }

        static class FirstLexemeEquals implements SuitabilityTester {
            private final String expectedLexeme;
            private final MessageType messageType;

            private FirstLexemeEquals(final String expectedLexeme, final MessageType messageType) {
                this.expectedLexeme = expectedLexeme;
                this.messageType = messageType;
            }

            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && expectedLexeme.equals(firstLexeme.getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return messageType;
            }
        }

        static class FirstLexemeMatches implements SuitabilityTester {
            private final Pattern expectedToken;
            private final MessageType messageType;

            private FirstLexemeMatches(final Pattern expectedToken, final MessageType messageType) {
                this.expectedToken = expectedToken;
                this.messageType = messageType;
            }

            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && expectedToken.matcher(firstLexeme.getTACToken()).matches();
            }

            @Override
            public MessageType getMessageType() {
                return messageType;
            }
        }
    }
}
