package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AUTOMATED;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.impl.METARImpl;

public class METAR34TestStrictSerialization extends AbstractAviMessageTest<String, METAR> {

    @Override
    public String getJsonFilename() {
        return "metar/metar34.json";
    }

    @Override
    public String getMessage() {
        return "METAR EFIV 181420Z AUTO 21011KT 9999 IC M18/M20 Q1008=";
    }

    @Override
    public String getCanonicalMessage() {
        return null;
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        return new ConversionHints(ConversionHints.KEY_WEATHER_CODES, ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY);
    }

    @Override
    public ConversionHints getParserConversionHints() {
        return new ConversionHints(ConversionHints.KEY_WEATHER_CODES, ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY);
    }

    @Override
    public ConversionHints getTokenizerParsingHints() {
        return new ConversionHints(ConversionHints.KEY_WEATHER_CODES, ConversionHints.VALUE_WEATHER_CODES_STRICT_WMO_4678);
    }

    @Override
    @Test(expected = SerializingException.class)
    public void testTokenizer() throws SerializingException, IOException {
        super.testTokenizer();
    }

    @Override
    public ConversionResult.Status getExpectedSerializationStatus() {
        return ConversionResult.Status.FAIL;
    }

    @Override
    public void assertSerializationIssues(final List<ConversionIssue> conversionIssues) {
        assertTrue(conversionIssues.size() == 1);
        assertTrue(ConversionIssue.Type.OTHER == conversionIssues.get(0).getType());
        assertTrue("Illegal weather code IC".equals(conversionIssues.get(0).getMessage()));
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "";
    }

    @Override
    public Identity[] getLexerTokenSequenceIdentity() {
        return spacify(new Identity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, AUTOMATED, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER,
                AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, END_TOKEN });
    }

    @Override
    public ConversionSpecification<String, METAR> getParsingSpecification() {
        return TACConverter.TAC_TO_METAR_POJO;
    }

    @Override
    public ConversionSpecification<METAR, String> getSerializationSpecification() {
        return TACConverter.METAR_POJO_TO_TAC;
    }

    @Override
    public Class<? extends METAR> getTokenizerImplmentationClass() {
        return METARImpl.class;
    }

}
