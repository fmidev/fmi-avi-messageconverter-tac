package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AUTOMATED;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VARIABLE_WIND_DIRECTION;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.impl.METARImpl;

public class METAR28Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar28.json";
	}
	
	@Override
	public String getMessage() {
		return
				"METAR EFTU 110820Z AUTO 35004KT 310V030 9999 FEW008/// OVC024/// 07/06 Q0999=";
	}

	@Override
	public String getCanonicalMessage() {
		return "METAR EFTU 110820Z AUTO 35004KT 310V030 9999 FEW008 OVC024 07/06 Q0999=";
	}

    @Override
	public String getTokenizedMessagePrefix() {
		return "";
	}

	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(new Identity[] {
				METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, AUTOMATED, SURFACE_WIND, VARIABLE_WIND_DIRECTION, HORIZONTAL_VISIBILITY, CLOUD,
				CLOUD, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, END_TOKEN
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
