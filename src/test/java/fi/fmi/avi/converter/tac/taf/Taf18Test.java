package fi.fmi.avi.converter.tac.taf;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.*;

public class Taf18Test extends AbstractAviMessageTest<String, TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf18.json";
    }

    @Override
    public String getMessage() {
        return
                "TAF EFHK 010825Z 0109/0209 25015KT 5000 SHRA SCT030\n"
                        + "FM011530 00000KT CAVOK=";
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "";
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        return ConversionHints.TAF;
    }

    @Override
    public ConversionHints getParserConversionHints() {
        return ConversionHints.TAF;
    }

    @Override
    public Identity[] getLexerTokenSequenceIdentity() {
        return spacify(new Identity[]{TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, SURFACE_WIND, CAVOK, END_TOKEN});
    }

    @Override
    public ConversionSpecification<String, TAF> getParsingSpecification() {
        return TACConverter.TAC_TO_TAF_POJO;
    }

    @Override
    public ConversionSpecification<TAF, String> getSerializationSpecification() {
        return TACConverter.TAF_POJO_TO_TAC;
    }


    @Override
    public Class<? extends TAF> getTokenizerImplmentationClass() {
        return TAFImpl.class;
    }

}