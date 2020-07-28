package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

import java.util.regex.Matcher;

public class SpaceWeatherNotAvailable extends RegexMatchingLexemeVisitor {
    public SpaceWeatherNotAvailable(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^NOT\\sAVBL", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.NO_SWX_AVAILABLE);

    }
}
