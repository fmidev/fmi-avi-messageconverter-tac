package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

/**
 *
 * TODO:
 * - OBS_OR_FORECAST is not detecting correctly
 * - FirType with three words fails (NEW AMSTERDAM FIR)
 * - Wrong phenomenon is returned (EMB_TS instead of SEV_ICE_FZRA)
 * - sigmet1a.json is not yet correct ()
 */

public class Sigmet1Test extends AbstractAviMessageTestTempSigmet<String, SIGMET> {

	@Override
	public String getJsonFilename() {
		return "../sigmet/sigmet1a.json";
	}

	@Override
	public String getMessage() {
		return "EHAA SIGMET M01 VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR SEV ICE (FZRA) OBS ENTIRE FIR STNR NC=";
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
        return spacify(new LexemeIdentity[] { SIGMET_START, REAL_SIGMET_START,
				SEQUENCE_DESCRIPTOR, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, SIGMET_FIR_NAME_WORD, FIR_NAME,
				PHENOMENON_SIGMET, OBS_OR_FORECAST, SIGMET_ENTIRE_AREA,

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
