package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AUTOMATED;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;

import java.util.Optional;

import org.junit.Test;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METAR34TestStrictLexing extends AbstractAviMessageTest<METAR> {

    @Override
    public String getJsonFilename() {
        return "metar/metar34.json";
    }

    @Override
    public String getMessage() {
        return "METAR EFIV 181420Z AUTO 21011KT 9999 IC M18/M20 Q1008=";
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        return Optional.empty();
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        return new ConversionHints(ConversionHints.KEY_WEATHER_CODES, ConversionHints.VALUE_WEATHER_CODES_STRICT_WMO_4678);
    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, AUTOMATED, SURFACE_WIND, HORIZONTAL_VISIBILITY, null,
                AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, END_TOKEN });
    }

    @Override
    @Test
    public void testTokenizer() {

    }

    @Override
    @Test
    public void testStringToPOJOParser() {

    }

    @Override
    @Test
    public void testPOJOToStringSerializer() {

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
    public Class<? extends METAR> getTokenizerImplementationClass() {
        return METARImpl.class;
    }

}
