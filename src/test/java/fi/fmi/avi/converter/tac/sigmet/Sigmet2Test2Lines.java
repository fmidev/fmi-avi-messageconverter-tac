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

public class Sigmet2Test2Lines extends AbstractAviMessageTestTempSigmet<String, SIGMET> {

	@Override
	public String getJsonFilename() {
		return "../sigmet/sigmet2a_2lines.json";
	}

	@Override
	public String getMessage() {
		return "EHAA SIGMET 1 VALID 111130/111530 EHDB-\r\nEHAA AMSTERDAM FIR OBSC TSGR FCST AT 1200Z S OF LINE N5400 E00100 - N5400 E00300 - N5400 E00600 - N5400 E01000 AND N OF LINE N5100 E00100 - N5100 E01000 STNR INTSF="; // FCST AT 1530Z ENTIRE FIR FCST AT 1530Z N52 E00520=";
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
				SEQUENCE_DESCRIPTOR, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME,
				SIGMET_VA_ERUPTION, SIGMET_VA_NAME, SIGMET_VA_POSITION, SIGMET_PHENOMENON, OBS_OR_FORECAST, SIGMET_2_LINES,
				SIGMET_MOVING, SIGMET_INTENSITY,
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
