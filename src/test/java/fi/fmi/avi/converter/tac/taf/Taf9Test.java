package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VALID_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.WEATHER;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class Taf9Test extends AbstractAviMessageTest<String, TAF> {

	@Override
	public String getJsonFilename() {
		return "taf/taf9.json";
	}
	
	@Override
	public String getMessage() {
		return "TAF EKSP 301128Z 3012/3112 28020G30KT 9999 BKN025\n" +
				"TEMPO 3012/3017 30025G38KT 5000 SHRA SCT020CB\n" +
				"TEMPO 3017/3024 6000 -SHRA BKN012TCU\n" +
                "BECMG 3017/3019 26015KT\n" +
				"BECMG 3100/3102 18015KT 5000 RA BKN008\n" +
                "TEMPO 3102/3106 15016G26KT 2500 RASN BKN004\n" +
                "BECMG 3106/3108 26018G30KT 9999 -SHRA BKN020=";
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
	public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
                TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND,
                HORIZONTAL_VISIBILITY, WEATHER, CLOUD, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND, HORIZONTAL_VISIBILITY,
                WEATHER, CLOUD, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP,
                SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, END_TOKEN
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
