package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VALID_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;
import static org.junit.Assert.assertTrue;

import java.util.List;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.impl.TAFImpl;

public class TafTokenOrderTest extends AbstractAviMessageTest<String, TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf2.json";
    }

    @Override
    public String getMessage() {
        return "TAF EFAB 190815Z 1909/1915 14008G15MPS 9999 BKN010 -RA BKN015=";
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "";
    }

    @Override
    public Identity[] getLexerTokenSequenceIdentity() {
        return spacify(new Identity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, WEATHER, CLOUD,
                END_TOKEN });
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

    @Override
    public Status getExpectedParsingStatus() {
        return Status.WITH_ERRORS;
    }

    @Override
    public void assertParsingIssues(List<ConversionIssue> conversionIssues) {
        assertTrue(conversionIssues.size() == 1);
        assertTrue(Type.SYNTAX_ERROR == conversionIssues.get(0).getType());
        assertTrue("Token ''-RA'(WEATHER,OK)' was found before one of type CLOUD".equals(conversionIssues.get(0).getMessage()));
    }

    @Override
    public String getCanonicalMessage() {
        return "TAF EFAB 190815Z 1909/1915 14008G15MPS 9999 BKN010 BKN015=";
    }

}
