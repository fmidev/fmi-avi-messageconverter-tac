package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.regex.Matcher;

public class DTGIssueTimeLabel extends RegexMatchingLexemeVisitor {
    public DTGIssueTimeLabel(final OccurrenceFrequency prio) {
        super("^DTG\\:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.DTG_ISSUE_TIME_LABEL);
    }

    public static class Reconstructor extends AbstractFixedContentOnTypesReconstructor {
        public Reconstructor() {
            super("DTG:", LexemeIdentity.DTG_ISSUE_TIME_LABEL,
                    SpaceWeatherAdvisoryAmd82.class, SpaceWeatherAdvisoryAmd79.class);
        }
    }
}
