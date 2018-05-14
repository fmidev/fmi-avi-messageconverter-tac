package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.NO_SIGNIFICANT_WEATHER;
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

public class Taf11Test extends AbstractAviMessageTest<String, TAF> {

	@Override
	public String getJsonFilename() {
		return "taf/taf11.json";
	}
	
	@Override
	public String getMessage() {
		return
				"EVRA 301103Z 3012/3112 15013KT 8000 OVC008\n" +
				"TEMPO 3012/3015 14015G26KT 5000 -RA OVC010\n" +
				"FM301500 18012KT 9000 NSW SCT015 BKN020\n" +
                "TEMPO 3017/3103 19020G33KT 3000 -SHRA BKN012CB BKN020=";
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
        return spacify(new Identity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, SURFACE_WIND, HORIZONTAL_VISIBILITY, NO_SIGNIFICANT_WEATHER, CLOUD, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
                TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND,
                HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD, END_TOKEN
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
