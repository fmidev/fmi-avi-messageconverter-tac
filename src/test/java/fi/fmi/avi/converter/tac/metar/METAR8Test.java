package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.COLOR_CODE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.metar.METAR;

public class METAR8Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar8.json";
	}

	// Equivalent to METAR7 but with different colors
	@Override
	public String getMessage() {
		return "EGXE 061150Z 03010KT 9999 FEW020 17/11 Q1014 BLACKBLU TEMPO 6000 SHRA SCT020 BLACKWHT=";
	}
	
	@Override
	public String getTokenizedMessagePrefix() {
        return "METAR ";
    }
	
	@Override
	public ConversionHints getLexerParsingHints() {
		return ConversionHints.METAR;
	}

	@Override
	public ConversionHints getParserConversionHints() {
		return ConversionHints.METAR;
	}

	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
        return spacify(new Identity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, AIR_DEWPOINT_TEMPERATURE,
                AIR_PRESSURE_QNH, COLOR_CODE, TREND_CHANGE_INDICATOR, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, COLOR_CODE,
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
        return METAR.class;
    }

}
