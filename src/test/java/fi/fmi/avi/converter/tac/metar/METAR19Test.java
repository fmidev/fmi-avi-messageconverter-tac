package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CAVOK;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.RUNWAY_STATE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.RUNWAY_VISUAL_RANGE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SEA_STATE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TREND_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.WEATHER;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METAR19Test extends AbstractAviMessageTest<METAR> {

    @Override
    public String getJsonFilename() {
        return "metar/metar19.json";
    }

    @Override
    public String getMessage() {
        return "METAR EFHK 111111Z 15008G20KT 0700 R04R/M1500VP2000N R15/P1000VM3000U R22L/1200N R04L/1000VP1500U SN VV006 M08/M10 Q1023 WM01/H14 99//9999 " //
                + "BECMG AT0927 25018G25MPS CAVOK " //
                + "TEMPO FM1051 VV040 " //
                + "TEMPO FM1130 NSC=";
    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, RUNWAY_VISUAL_RANGE,
                RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, WEATHER, CLOUD, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, SEA_STATE,
                RUNWAY_STATE, TREND_CHANGE_INDICATOR, TREND_TIME_GROUP, SURFACE_WIND, CAVOK, TREND_CHANGE_INDICATOR, TREND_TIME_GROUP, CLOUD,
                TREND_CHANGE_INDICATOR, TREND_TIME_GROUP, CLOUD, END_TOKEN });
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
