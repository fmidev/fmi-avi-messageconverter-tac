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
 */

public class SigmetVaTest_NoName extends AbstractAviMessageTestTempSigmet<String, SIGMET> {

	@Override
	public String getJsonFilename() {
		return "../sigmet/sigmetva_noname.json";
	}

	@Override
	public String getMessage() {
		return "EHAA SIGMET 4 VALID 171610/172210 EHDB-\r\n"
			+"EHAA AMSTERDAM FIR VA ERUPTION PSN S1547 W07150 VA CLD"
			+" OBS AT 1520Z WI S1534 W07117 - S1554 W07101 - S1618 W07151 - S1558 W07158 - S1546 W07150 - S1534 W07117 SFC/FL230 NC FCST AT 2130Z WI S1533 W07059 - S1605 W07101 - S1601 W07144 - S1546 W07152 - S1539 W07143 - S1533 W07059=";
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
				SIGMET_VA_ERUPTION, SIGMET_VA_POSITION, SIGMET_PHENOMENON,
				OBS_OR_FORECAST, SIGMET_WITHIN,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR,
				SIGMET_LEVEL, SIGMET_INTENSITY,
				SIGMET_FCST_AT, SIGMET_WITHIN,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR,
				POLYGON_COORDINATE_PAIR,
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
