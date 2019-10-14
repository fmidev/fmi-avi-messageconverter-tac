package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.MAX_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.MIN_TEMPERATURE;
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
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class Taf12Test extends AbstractAviMessageTest<String, TAF> {

	@Override
	public String getJsonFilename() {
		return "taf/taf12.json";
	}

	@Override
	public String getMessage() {
		return "TAF EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM02/3015Z TNM10/3103Z\n" +
				"TEMPO 3012/3018 3000 RADZ BR OVC004\n" +
			    "BECMG 3018/3020 BKN008 SCT015CB\n" +
				"TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" + "BECMG 3104/3106 21016G30KT VV001=";
    }

	@Override
	public ConversionHints getParserConversionHints() {
		ConversionHints hints = new ConversionHints();
		hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TAF);
		hints.put(ConversionHints.KEY_TIMEZONE_ID_POLICY, ConversionHints.VALUE_TIMEZONE_ID_POLICY_STRICT);

        return hints;
	}

	@Override
	public ConversionHints getLexerParsingHints() {
		return ConversionHints.TAF;
	}

	@Override
	public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(
                new LexemeIdentity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, CLOUD, MAX_TEMPERATURE,
                        MIN_TEMPERATURE, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, WEATHER, CLOUD,
                        TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, CLOUD, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
                        TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
                        TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND, CLOUD, END_TOKEN });
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
