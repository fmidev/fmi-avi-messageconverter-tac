package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REMARK;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REMARKS_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.RUNWAY_STATE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.WEATHER;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METAR36Test extends AbstractAviMessageTest<METAR> {

    @Override
    public String getJsonFilename() {
        return "metar/metar36.json";
    }

    @Override
    public String getMessage() {
        return "METAR URKA 261230Z 28005MPS 9999 -SHRA FEW/// SCT051CB 11/09 Q1010 R22/290160 TEMPO VRB10G16MPS 0500 +TSRA BKN003 BKN020CB RMK QFE752/1003=";
    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD,
                AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, RUNWAY_STATE, TREND_CHANGE_INDICATOR, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD,
                REMARKS_START, REMARK, END_TOKEN });
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
