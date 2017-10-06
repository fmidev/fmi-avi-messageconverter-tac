package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.METAR;

/**
 * Created by rinne on 10/02/17.
 */
public class Whitespace extends RegexMatchingLexemeVisitor {

    public Whitespace(final Priority prio) {
        super("^\\s+$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (Identity.WHITE_SPACE == token.getIdentityIfAcceptable()) {
            return;
        }
        token.identify(Identity.WHITE_SPACE);
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, token.getTACToken());
    }
}
