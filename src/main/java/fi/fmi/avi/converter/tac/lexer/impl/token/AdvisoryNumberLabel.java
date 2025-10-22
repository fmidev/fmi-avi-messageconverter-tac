package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.regex.Matcher;

public class AdvisoryNumberLabel extends RegexMatchingLexemeVisitor {
    public AdvisoryNumberLabel(final OccurrenceFrequency prio) {
        super("^ADVISORY\\sNR:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_NUMBER_LABEL);
    }

    public static class Reconstructor extends AbstractFixedContentOnTypesReconstructor {
        public Reconstructor() {
            super("ADVISORY NR:", LexemeIdentity.ADVISORY_NUMBER_LABEL,
                    SpaceWeatherAdvisoryAmd82.class, SpaceWeatherAdvisoryAmd79.class);
        }
    }
}
