package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

public class Sigmet2Test1Line extends AbstractAviMessageTestTempSigmet<String, SIGMET> {

	@Override
	public String getJsonFilename() {
		return "../sigmet/sigmet2a_1line.json";
	}

	@Override
	public String getMessage() {
		return "EHAA SIGMET 2 VALID 221010/221610 EHDB-\r\nEHAA AMSTERDAM FIR VA ERUPTION MT SABANCAYA PSN S1547 W07150 VA CLD OBS AT 0910Z E OF LINE N49 E005 - N54 E005 - N60 E005 SFC/FL240 STNR INTSF=";
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
				SIGMET_VA_ERUPTION, SIGMET_VA_NAME, SIGMET_VA_POSITION,
				SIGMET_PHENOMENON, OBS_OR_FORECAST, SIGMET_LINE, SIGMET_LEVEL, SIGMET_MOVING, SIGMET_INTENSITY,
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
