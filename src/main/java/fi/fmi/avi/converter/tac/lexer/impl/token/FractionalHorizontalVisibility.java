package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;

import java.util.Locale;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Status;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;

/**
 * Created by rinne on 10/02/17.
 */
public class FractionalHorizontalVisibility extends RegexMatchingLexemeVisitor {

    public FractionalHorizontalVisibility(final OccurrenceFrequency prio) {
        super("^([PM])?((([0-9]{1,3}\\s)(([1-9]{1})/([1-9]{1,2})))|([0-9]{1,3})|(([0-9]{1})/([0-9]{1,2})))(?!KT)([A-Z]{1,2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final RecognizingAviMessageTokenLexer.RelationalOperator modifier = RecognizingAviMessageTokenLexer.RelationalOperator.forCode(match.group(1));
        final String bothParts = match.group(3);
        final String wholePartOnly = match.group(8);
        final String fractionOnly = match.group(9);

        int wholePart = -1;
        int fractionNumenator = -1;
        int fractionDenumenator = -1;
        if (wholePartOnly != null) {
            wholePart = Integer.parseInt(wholePartOnly);
        } else if (bothParts != null) {
            wholePart = Integer.parseInt(match.group(4).trim());
            fractionNumenator = Integer.parseInt(match.group(6));
            fractionDenumenator = Integer.parseInt(match.group(7));
        } else if (fractionOnly != null) {
            wholePart = 0;
            fractionNumenator = Integer.parseInt(match.group(10));
            fractionDenumenator = Integer.parseInt(match.group(11));
        }
        if (fractionNumenator > -1 && fractionDenumenator > -1) {
            if (fractionDenumenator != 0) {
                token.identify(HORIZONTAL_VISIBILITY);
                token.setParsedValue(VALUE, wholePart + (double) fractionNumenator / (double) fractionDenumenator);
            } else {
                token.identify(HORIZONTAL_VISIBILITY, Status.SYNTAX_ERROR, "Invalid fractional number '" + fractionNumenator + "/" + fractionDenumenator);
            }
        } else if (wholePart > -1) {
            token.identify(HORIZONTAL_VISIBILITY);
            token.setParsedValue(VALUE, (double) wholePart);
        } else {
            token.identify(HORIZONTAL_VISIBILITY, Status.SYNTAX_ERROR, "Invalid number '" + wholePart + " " + fractionNumenator + "/" + fractionDenumenator);
        }
        if (modifier != null) {
            token.setParsedValue(RELATIONAL_OPERATOR, modifier);
        }
        final String unit = match.group(12).toLowerCase(Locale.US);
        token.setParsedValue(UNIT, unit);
    }
}
