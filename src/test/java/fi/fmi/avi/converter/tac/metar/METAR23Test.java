package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.*;

import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.impl.METARImpl;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

public class METAR23Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar23.json";
	}
	
	@Override
	public String getMessage() {
		return
				"METAR COR EFUT 111115Z 18004KT 150V240 1500 0500SW R04R/1500N R15/M0050D R22L/1200N R04L/P1000U SN VV006 M08/M10 " + "Q1023 RESN TEMPO 9999=";
	}
	
	@Override
	public String getTokenizedMessagePrefix() {
		return "";
	}
	
	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(new Identity[] {
				METAR_START, CORRECTION, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, VARIABLE_WIND_DIRECTION,
                HORIZONTAL_VISIBILITY, HORIZONTAL_VISIBILITY, RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, RUNWAY_VISUAL_RANGE, WEATHER,
                CLOUD, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, RECENT_WEATHER, TREND_CHANGE_INDICATOR,
                HORIZONTAL_VISIBILITY, END_TOKEN
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
