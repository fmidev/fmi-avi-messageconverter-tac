package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeVisitor;
import fi.fmi.avi.model.MessageType;

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
        boolean test(LexemeSequence sequence);

        MessageType getMessageType();
    }

}
