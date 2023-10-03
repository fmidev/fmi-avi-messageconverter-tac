package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;

public class SigmetMinimalTest extends AbstractAviMessageTestTempSigmet<String, SIGMET> {

    @Override
    public String getJsonFilename() {
        return "../sigmet/sigmet_minimal_test.json";
    }

    @Override
    public String getMessage() {
        return "EHAA SIGMET M01 VALID 111130/111530 EHDB-\r\nEHAA AMSTERDAM FIR TEST=";
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
        return spacify(new LexemeIdentity[]{SIGMET_START,
                SEQUENCE_DESCRIPTOR, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_USAGE, END_TOKEN});
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
