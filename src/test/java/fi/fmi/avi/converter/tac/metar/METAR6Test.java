package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.*;

import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.impl.METARImpl;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

public class METAR6Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar6.json";
	}
	
	@Override
	public String getMessage() {
		return
				"METAR EFHK 010550Z 21018KT 9999 -RA SCT013 BKN025 03/00 Q0988 TEMPO 4000 RASN=";
	}
	
	@Override
	public String getTokenizedMessagePrefix() {
		return "";
	}

	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(new Identity[] {
				METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD,
                AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, TREND_CHANGE_INDICATOR, HORIZONTAL_VISIBILITY, WEATHER, END_TOKEN
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
