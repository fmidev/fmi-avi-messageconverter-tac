package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class PolygonCoordinatePairSeparator extends PrioritizedLexemeVisitor {

    private static final Set<String> DASH_VARIANTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "-", // U+002D hyphen-minus
            "‐", // U+2010 hyphen
            "–", // U+2013 en dash
            "—", // U+2014 em dash
            "‒", // U+2012 figure dash
            "―", // U+2015 horizontal bar
            "−",  // U+2212 minus sign
            "﹣", // U+FE63 small hyphen-minus
            "－"  // U+FF0D fullwidth hyphen-minus
    )));

    public PolygonCoordinatePairSeparator(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (DASH_VARIANTS.contains(token.getTACToken())
                && LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(token.getPrevious().getIdentity())
                && LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(token.getNext().getIdentity())) {
            token.identify(LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR);
        }
    }
}