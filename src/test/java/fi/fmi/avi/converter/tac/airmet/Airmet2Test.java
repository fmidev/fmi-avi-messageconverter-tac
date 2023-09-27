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

public class Airmet2Test extends AbstractAviMessageTestTempAirmet<String, AIRMET> {

	@Override
	public String getJsonFilename() {
		return "../airmet/airmet2a.json";
	}

	@Override
	public String getMessage() {
		return "EHAA AIRMET 1 VALID 111130/111530 EHDB-\r\nEHAA AMSTERDAM FIR ISOL TSGR FCST AT 1200Z ENTIRE FIR STNR INTSF="; // FCST AT 1530Z ENTIRE FIR FCST AT 1530Z N52 E00520=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS N OF N20 AND S OF N30=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS N10=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS N OF LINE N10 E110 - N11 W111 - N12 E112 - N13 E113 - N14 E114=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS WI N10 E110 - N11 W111 - N12 E112 - N13 E113 - N14 E114=";
//		return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\nEHAA NEW AMSTERDAM FIR SEV ICE (FZRA) OBS ENTIRE FIR=";
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
