package fi.fmi.avi.converter.tac.airmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

/**
 *
 */

public class Airmet2Test_sfcvis extends AbstractAviMessageTestTempAirmet<String, AIRMET> {

	@Override
	public String getJsonFilename() {
		return "../airmet/airmet2_sfcvis.json";
	}

	@Override
	public String getMessage() {
		return "EHAA AIRMET 1 VALID 111130/111530 EHDB-\r\nEHAA AMSTERDAM FIR SFC VIS 1000M (FG) OBS AT 1200Z ENTIRE FIR STNR WKN=";
	}

	@Override
	public ConversionHints getLexerParsingHints() {
		return ConversionHints.AIRMET;
	}

	@Override
	public ConversionHints getParserConversionHints() {
		return ConversionHints.AIRMET;
	}

	@Override
	public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { AIRMET_START,
				SEQUENCE_DESCRIPTOR, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME,
				AIRMET_PHENOMENON, OBS_OR_FORECAST, SIGMET_ENTIRE_AREA,
				SIGMET_MOVING, SIGMET_INTENSITY,
				END_TOKEN });
	}

	@Override
    public ConversionSpecification<String, AIRMET> getParsingSpecification() {
		return TACConverter.TAC_TO_AIRMET_POJO;
    }

    @Override
    public ConversionSpecification<AIRMET, String> getSerializationSpecification() {
        return TACConverter.AIRMET_POJO_TO_TAC;
    }

	@Override
	public Class<? extends AIRMET> getTokenizerImplmentationClass() {
		return AIRMETImpl.class;
	}

}
