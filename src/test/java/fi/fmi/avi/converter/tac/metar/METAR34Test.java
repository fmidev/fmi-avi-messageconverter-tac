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

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.metar.METAR;

public class METAR34Test extends AbstractAviMessageTest<String, METAR> {

    @Override
    public String getJsonFilename() {
        return "metar/metar34.json";
    }

    @Override
    public String getMessage() {
        return "METAR EFIV 181420Z AUTO 21011KT 9999 IC M18/M20 Q1008=";
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
        return new ConversionHints(ConversionHints.KEY_WEATHER_CODES, ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY);
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
        return METAR.class;
    }

}
