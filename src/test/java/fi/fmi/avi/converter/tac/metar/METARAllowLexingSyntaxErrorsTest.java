package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.NO_SIGNIFICANT_CHANGES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METARAllowLexingSyntaxErrorsTest extends AbstractAviMessageTest<METAR> {

    @Override
    public String getJsonFilename() {
        return "metar/metar_missing_surface_wind.json";
    }

    @Override
    public String getMessage() {
        return "METAR EFHK 051052Z blaablaa 9999 FEW033 BKN110 M00/M02 Q1005 NOSIG=";
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        return Optional.of("METAR EFHK 051052Z 9999 FEW033 BKN110 M00/M02 Q1005 NOSIG=");
    }

    @Override
    public ConversionHints getParserConversionHints() {
        final ConversionHints hints = super.getParserConversionHints();
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_ERRORS;
    }

    @Override
    public void assertParsingIssues(final List<ConversionIssue> conversionIssues) {
        assertEquals(2, conversionIssues.size());

        ConversionIssue issue = conversionIssues.get(0);
        assertEquals(ConversionIssue.Type.SYNTAX, issue.getType());
        assertTrue("Unexpected error message", issue.getMessage().contains("Lexing problem with 'blaablaa'"));

        issue = conversionIssues.get(1);
        assertEquals(ConversionIssue.Type.SYNTAX, issue.getType());
        assertTrue("Unexpected error message", issue.getMessage().contains("Missing surface wind information in"));

    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(
                new LexemeIdentity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, null, HORIZONTAL_VISIBILITY, CLOUD, CLOUD, AIR_DEWPOINT_TEMPERATURE,
                        AIR_PRESSURE_QNH, NO_SIGNIFICANT_CHANGES, END_TOKEN });
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
    public Class<? extends METAR> getTokenizerImplementationClass() {
        return METARImpl.class;
    }

}
