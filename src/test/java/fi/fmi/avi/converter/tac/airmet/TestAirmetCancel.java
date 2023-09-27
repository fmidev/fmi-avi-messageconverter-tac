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

public class TestAirmetCancel extends AbstractAviMessageTestTempAirmet<String, AIRMET> {

	@Override
	public String getJsonFilename() {
		return "../airmet/airmetcancel.json";
	}

	@Override
	public String getMessage() {
		return "EHAA AIRMET M03 VALID 271330/271530 EHDB-\r\nEHAA AMSTERDAM FIR CNL AIRMET M01 271130/271530=";
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
				AIRMET_CANCEL,
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
