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

public class TestSigmetMoving extends AbstractAviMessageTestTempSigmet<String, SIGMET> {

	@Override
	public String getJsonFilename() {
		return "../sigmet/sigmet_moving.json";
	}

	@Override
	public String getMessage() {
		return "EHAA SIGMET 1 VALID 271130/271800 EHDB-\r\nEHAA AMSTERDAM FIR EMBD TS OBS AT 1200Z WI N5200 E00500 - N5300 E00600 - N5400 E00500 - N5200 E00500 FL010/035 MOV S 5KT NC=";
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
				SEQUENCE_DESCRIPTOR, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME,
				SIGMET_PHENOMENON, OBS_OR_FORECAST, SIGMET_WITHIN,
                POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
                POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
                POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
                POLYGON_COORDINATE_PAIR,
                SIGMET_LEVEL, SIGMET_MOVING, SIGMET_INTENSITY,
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
