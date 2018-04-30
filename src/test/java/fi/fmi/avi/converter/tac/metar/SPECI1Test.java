package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.RUNWAY_VISUAL_RANGE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SPECI_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class SPECI1Test extends AbstractAviMessageTest<String, SPECI> {

    @Override
    public String getJsonFilename() {
        return "metar/speci1.json";
    }

    @Override
    public String getMessage() {
        return "SPECI EFHK 012231Z 00000KT 4500 R04R/0500D R15/0600VP1500D R22L/0275N R04L/P1500D BR FEW003 SCT050 14/13 Q1008 " + "TEMPO 2000=";
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "";
    }

    @Override
    public Identity[] getLexerTokenSequenceIdentity() {
        return spacify(
                new Identity[] { SPECI_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE,
                        RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, WEATHER, CLOUD, CLOUD, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, TREND_CHANGE_INDICATOR,
                        HORIZONTAL_VISIBILITY, END_TOKEN });
    }

    @Override
    public ConversionSpecification<String, SPECI> getParsingSpecification() {
        return TACConverter.TAC_TO_SPECI_POJO;
    }

    @Override
    public ConversionSpecification<SPECI, String> getSerializationSpecification() {
        return TACConverter.SPECI_POJO_TO_TAC;
    }

    @Override
    public Class<? extends SPECI> getTokenizerImplmentationClass() {
        return METARImpl.class;
    }

}
