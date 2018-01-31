package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VARIABLE_WIND_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.impl.METARImpl;

public class METAR30Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar30.json";
	}
	
	@Override
	public String getMessage() {
		return
				"METAR EFHK 240620Z 36003KT 300V360 9999 -SHSN FEW020CB BKN032 M02/M03 Q1032 NOSIG=";
	}

	@Override
	public String getCanonicalMessage() {
		return "METAR EFHK 240620Z 36003KT 300V360 9999 -SHSN FEW020CB BKN032 M02/M03 Q1032 NOSIG=";
	}

    @Override
	public String getTokenizedMessagePrefix() {
		return "";
	}

	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(
				new Identity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, VARIABLE_WIND_DIRECTION, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
						CLOUD, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, TREND_CHANGE_INDICATOR, END_TOKEN
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
