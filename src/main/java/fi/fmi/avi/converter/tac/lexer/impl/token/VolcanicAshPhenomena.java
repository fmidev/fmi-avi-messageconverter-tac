package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

public class VolcanicAshPhenomena extends RegexMatchingLexemeVisitor {

    public VolcanicAshPhenomena(final OccurrenceFrequency prio) {
        super("^(?<type>OBS|FCST)(?:[a-zA-Z0-9\\+\\s]+)?:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        token.setParsedValue(Lexeme.ParsedValueName.TYPE, Type.valueOf(match.group("type")));
    }

    public enum Type { OBS, FCST }

}
