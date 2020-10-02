package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;

public class PolygonCoordinatePairSeparator extends PrioritizedLexemeVisitor {
    public PolygonCoordinatePairSeparator(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("-".equals(token.getTACToken()) && LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(token.getPrevious().getIdentity())
                && LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(token.getNext().getIdentity())) {
            token.identify(LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR);
        }
    }
}
