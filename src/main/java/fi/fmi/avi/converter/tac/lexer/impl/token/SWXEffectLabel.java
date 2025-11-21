package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.regex.Matcher;

public class SWXEffectLabel extends RegexMatchingLexemeVisitor {
    public SWXEffectLabel(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^SWX\\sEFFECT:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_EFFECT_LABEL);

    }

    public static class Reconstructor extends AbstractFixedContentOnTypesReconstructor {
        public Reconstructor() {
            super("SWX EFFECT:", LexemeIdentity.SWX_EFFECT_LABEL,
                    SpaceWeatherAdvisoryAmd82.class, SpaceWeatherAdvisoryAmd79.class);
        }
    }
}
