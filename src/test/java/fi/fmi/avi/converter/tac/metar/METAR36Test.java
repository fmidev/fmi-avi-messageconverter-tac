package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.REMARK;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.REMARKS_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.RUNWAY_STATE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METAR36Test extends AbstractAviMessageTest<String, METAR> {

    @Override
    public String getJsonFilename() {
        return "metar/metar36.json";
    }

    @Override
    public String getMessage() {
        return "METAR URKA 261230Z 28005MPS 9999 -SHRA FEW/// SCT051CB 11/09 Q1010 R22/290160 TEMPO VRB10G16MPS 0500 +TSRA BKN003 BKN020CB RMK QFE752/1003=";
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        return Optional.of(
                "METAR URKA 261230Z 28005MPS 9999 -SHRA FEW/// SCT051CB 11/09 Q1010 R22/290160 TEMPO 0500 +TSRA BKN003 BKN020CB RMK " + "QFE752/1003=");
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "";
    }

    @Override
    public Lexeme.Identity[] getLexerTokenSequenceIdentity() {
        return spacify(new Lexeme.Identity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD,
                AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, RUNWAY_STATE, TREND_CHANGE_INDICATOR, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD,
                REMARKS_START, REMARK, END_TOKEN });
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_ERRORS;
    }

    public void assertParsingIssues(List<ConversionIssue> conversionIssues) {
        assertTrue(conversionIssues.size() == 1);
        assertTrue(ConversionIssue.Severity.ERROR == conversionIssues.get(0).getSeverity());
        assertTrue(ConversionIssue.Type.SYNTAX == conversionIssues.get(0).getType());
        assertTrue(conversionIssues.get(0).getMessage().toLowerCase().contains("variable"));
    }

    @Override
    public ConversionSpecification<String, METAR> getParsingSpecification() {
        return TACConverter.TAC_TO_METAR_POJO;
    }

    @Override
    public ConversionSpecification<METAR, String> getSerializationSpecification() {
        return TACConverter.METAR_POJO_TO_TAC;
    }

    @Override
    public Class<? extends METAR> getTokenizerImplmentationClass() {
        return METARImpl.class;
    }
}
