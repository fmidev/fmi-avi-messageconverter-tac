package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VALID_TIME;
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
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class TafMissingStartTokenTest extends AbstractAviMessageTest<TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf_missing_start_token.json";
    }

    @Override
    public String getMessage() {
        return "EFAB 190815Z 1909/1915 14008G15MPS 9999 BKN010 BKN015=";
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "TAF ";
    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(
                new LexemeIdentity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, CLOUD, END_TOKEN });
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
    public ConversionHints getParserConversionHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TAF);
        return hints;
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TAF);
        return hints;
    }

    @Override
    public Class<? extends TAF> getTokenizerImplementationClass() {
        return TAFImpl.class;
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_WARNINGS;
    }

    @Override
    public void assertParsingIssues(final List<ConversionIssue> conversionIssues) {
        assertEquals(1, conversionIssues.size());
        assertSame(ConversionIssue.Type.SYNTAX, conversionIssues.get(0).getType());
        assertEquals("Message does not start with a start token: TAF", conversionIssues.get(0).getMessage());
    }

}
