package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_CHANGE_FORECAST_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VALID_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class Taf8Test extends AbstractAviMessageTest<String, TAF> {

	@Override
	public String getJsonFilename() {
		return "taf/taf8.json";
	}
	
	@Override
	public String getMessage() {
		return
				"EFVA 270930Z 2712/2812 14015G25KT 8000 -RA SCT020 OVC050\n" +
				"BECMG 2715/2717 5000 -RA BKN007\n" +
				"PROB40 2715/2720 4000 RASN\n" +
				"BECMG 2720/2722 16012KT\n" +
				"TEMPO 2720/2724 8000\n" +
				"PROB40 2802/2806 3000 RASN BKN004=";
	}
	
	@Override
	public String getTokenizedMessagePrefix() {
		return "TAF ";
	}
	
	@Override
	public ConversionHints getLexerParsingHints() {
		return ConversionHints.TAF;
	}

    @Override
	public ConversionHints getParserConversionHints() {
		return ConversionHints.TAF;
	}


	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
        return spacify(new Identity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
                TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, TAF_FORECAST_CHANGE_INDICATOR,
                TAF_CHANGE_FORECAST_TIME_GROUP,
                HORIZONTAL_VISIBILITY, WEATHER, CLOUD, END_TOKEN
		});
	}

	@Override
    public ConversionSpecification<String, TAF> getParsingSpecification() {
        return TACConverter.TAC_TO_TAF_POJO;
    }
    
    @Override
    public ConversionSpecification<TAF, String> getSerializationSpecification() {
        return TACConverter.TAF_POJO_TO_TAC;
    }


	@Override
	public Class<? extends TAF> getTokenizerImplmentationClass() {
		return TAFImpl.class;
	}

}
