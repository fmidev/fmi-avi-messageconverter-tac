package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.*;

import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.impl.METARImpl;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

public class METAR19Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar19.json";
	}
	
	@Override
	public String getMessage() {
		return
				"METAR EFHK 111111Z 15008G20KT 0700 R04R/M1500VP2000N R15/P1000VM3000U R22L/1200N R04L/1000VP1500U SN VV006 M08/M10 Q1023 " + "WM01/H14 99//9999 " +
				"BECMG AT0927 25018G25MPS CAVOK " +
				"TEMPO FM1051 VV040 "+
				"TEMPO FM1130 NSC=";
	}
	
	@Override
	public String getTokenizedMessagePrefix() {
		return "";
	}
	
	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(new Identity[] {
				METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, RUNWAY_VISUAL_RANGE,
                RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, WEATHER, CLOUD, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH,
                SEA_STATE, RUNWAY_STATE,
                TREND_CHANGE_INDICATOR, TREND_TIME_GROUP, SURFACE_WIND, CAVOK,
                TREND_CHANGE_INDICATOR, TREND_TIME_GROUP, CLOUD,
                TREND_CHANGE_INDICATOR, TREND_TIME_GROUP, CLOUD,
                END_TOKEN
		});
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
