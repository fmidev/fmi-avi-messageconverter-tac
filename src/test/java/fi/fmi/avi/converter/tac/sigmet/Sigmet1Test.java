package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

public class Sigmet1Test extends AbstractAviMessageTest<String, SIGMET> {

	@Override
	public String getJsonFilename() {
		return "sigmet/sigmet1a.json";
	}
	
	@Override
	public String getMessage() {
		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS N OF N20=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS N OF N20 AND S OF N30=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS N10=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS N OF LINE N10 E110 - N11 W111 - N12 E112 - N13 E113 - N14 E114=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS WI N10 E110 - N11 W111 - N12 E112 - N13 E113 - N14 E114=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS ENTIRE FIR=";
	}
	
	@Override
	public ConversionHints getLexerParsingHints() {
		return ConversionHints.SIGMET;
	}

	@Override
	public ConversionHints getParserConversionHints() {
		return ConversionHints.SIGMET;
	}

	@Override
	public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { SIGMET_START,
	//			AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD,
    //            TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
    //            TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, SURFACE_WIND,
    //            TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, TAF_FORECAST_CHANGE_INDICATOR,
    //            TAF_CHANGE_FORECAST_TIME_GROUP,
    //            HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
				END_TOKEN });
	}

	@Override
    public ConversionSpecification<String, SIGMET> getParsingSpecification() {
		return TACConverter.TAC_TO_SIGMET_POJO;
    }
    
    @Override
    public ConversionSpecification<SIGMET, String> getSerializationSpecification() {
        return TACConverter.SIGMET_POJO_TO_TAC;
    }

	@Override
	public Class<? extends SIGMET> getTokenizerImplmentationClass() {
		return SIGMETImpl.class;
	}

}
