package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;

public class SpaceWeatherEffect extends RegexMatchingLexemeVisitor {
    public SpaceWeatherEffect(final OccurrenceFrequency prio) {
        super("^(?<phenomenon>(SATCOM|HF\\sCOM|GNSS|RADIATION){1}\\s(MOD|SEV){1})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_EFFECT);
        SpaceWeatherPhenomenon phenomenon = SpaceWeatherPhenomenon.fromCombinedCode(match.group("phenomenon"));
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, phenomenon);
    }
}
