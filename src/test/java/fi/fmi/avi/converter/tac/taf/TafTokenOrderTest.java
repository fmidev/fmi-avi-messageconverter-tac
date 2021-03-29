package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VALID_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.WEATHER;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class TafTokenOrderTest extends AbstractAviMessageTest<String, TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf2_illegal_weather_code_pos.json";
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
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, WEATHER, CLOUD,
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
        assertTrue(Type.SYNTAX == conversionIssues.get(0).getType());
        assertTrue("Invalid token order: ''-RA'(WEATHER,OK)' was found after one of type CLOUD".equals(conversionIssues.get(0).getMessage()));
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        return Optional.of("TAF EFAB 190815Z 1909/1915 14008G15MPS 9999 BKN010 BKN015=");
    }

}
