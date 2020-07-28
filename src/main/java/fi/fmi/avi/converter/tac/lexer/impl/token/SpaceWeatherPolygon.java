package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

import java.util.regex.Matcher;

public class SpaceWeatherPolygon extends RegexMatchingLexemeVisitor {
    //TODO:
    public SpaceWeatherPolygon(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^(((N|S)\\d*\\s(W|E)\\d*)(\\s-\\s)?){5}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_POLYGON);
    }
}
