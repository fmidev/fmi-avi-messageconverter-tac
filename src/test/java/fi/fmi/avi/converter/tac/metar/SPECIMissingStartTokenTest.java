package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AUTOMATED;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CAVOK;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SPECI_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.metar.SPECI;

public class SPECIMissingStartTokenTest extends AbstractAviMessageTest<String, SPECI> {

    @Override
    public String getJsonFilename() {
        return "metar/speci_missing_start_token.json";
    }

    @Override
    public String getMessage() {
        return "EFTU 011350Z AUTO VRB02KT CAVOK 22/12 Q1008=";
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "SPECI ";
    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { SPECI_START, AERODROME_DESIGNATOR, ISSUE_TIME, AUTOMATED, SURFACE_WIND, CAVOK, AIR_DEWPOINT_TEMPERATURE,
                AIR_PRESSURE_QNH, END_TOKEN });
    }

    @Override
    public ConversionHints getParserConversionHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.SPECI);
        return hints;
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.SPECI);
        return hints;
    }

    @Override
    public ConversionSpecification<String, SPECI> getParsingSpecification() {
        return TACConverter.TAC_TO_SPECI_POJO;
    }

    @Override
    public ConversionSpecification<SPECI, String> getSerializationSpecification() {
        return TACConverter.SPECI_POJO_TO_TAC;
    }

    @Override
    public Class<? extends SPECI> getTokenizerImplmentationClass() {
        return SPECI.class;
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_WARNINGS;
    }

    @Override
    public void assertParsingIssues(List<ConversionIssue> conversionIssues) {
        assertEquals(1, conversionIssues.size());
        assertSame(ConversionIssue.Type.SYNTAX, conversionIssues.get(0).getType());
        assertEquals("Message does not start with a start token", conversionIssues.get(0).getMessage());
    }

}
