package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

import java.util.regex.Matcher;

public class SpaceWeatherVerticalLimit extends RegexMatchingLexemeVisitor {
    //TODO:
    public SpaceWeatherVerticalLimit(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^ABV\\sFL(\\d*)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_VERTICAL_LIMIT);

    }
}
