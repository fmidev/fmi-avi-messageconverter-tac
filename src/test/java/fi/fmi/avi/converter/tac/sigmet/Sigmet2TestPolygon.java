package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

public class Sigmet2TestPolygon extends AbstractAviMessageTestTempSigmet<String, SIGMET> {

	@Override
	public String getJsonFilename() {
		return "../sigmet/sigmet2a_polygon.json";
	}

	@Override
	public String getMessage() {
		return "EHAA SIGMET 1 VALID 111130/111530 EHDB-\r\nEHAA AMSTERDAM FIR OBSC TSGR FCST AT 1200Z WI N5200 E00524 - N5300 E00630 - N5100 E00718 - N5200 E00524 STNR INTSF="; // FCST AT 1530Z ENTIRE FIR FCST AT 1530Z N52 E00520=";
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
				POLYGON_COORDINATE_PAIR, SIGMET_MOVING, SIGMET_INTENSITY,
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
