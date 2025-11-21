package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.util.DashVariant;

import java.util.Optional;

public class PolygonCoordinatePairSeparator extends PrioritizedLexemeVisitor {

    public PolygonCoordinatePairSeparator(final OccurrenceFrequency prio) {
        super(prio);
    }

    private static boolean isPolygonCoordinatePair(final Lexeme nullableLexeme) {
        return LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(Optional.ofNullable(nullableLexeme)
                .map(Lexeme::getIdentity)
                .orElse(null));
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (DashVariant.isDash(token.getTACToken())
                && isPolygonCoordinatePair(token.getPrevious())
                && isPolygonCoordinatePair(token.getNext())) {
            token.identify(LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR);
        }
    }
}