package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;

import java.util.regex.Matcher;

public class SpaceWeatherVerticalLimit extends RegexMatchingLexemeVisitor {
    public SpaceWeatherVerticalLimit(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        //TODO: Add pattern for FL000-000
        super("^((?<above>ABV)\\s(?<unit>FL)(?<value>\\d*))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);

        String above = match.group("above");
        if(above != null && above != "") {
            token.setParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.ABOVE);
        }
        token.setParsedValue(Lexeme.ParsedValueName.UNIT, match.group("unit"));
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, Integer.parseInt(match.group("value")));
    }
}
