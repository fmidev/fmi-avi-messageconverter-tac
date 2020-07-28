package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

import java.util.regex.Matcher;

public class SpaceWeatherEffectLabel extends RegexMatchingLexemeVisitor {
    public SpaceWeatherEffectLabel(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^SWX\\sEFFECT:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_EFFECT_LABEL);

    }
}
