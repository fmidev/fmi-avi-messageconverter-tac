package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CAVOK;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.NO_SIGNIFICANT_WEATHER;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VALID_TIME;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class Taf19BaseWeatherNSWTest extends AbstractAviMessageTest<String, TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf19.json";
    }

    @Override
    public String getMessage() {
        return "TAF EFHK 010825Z 0109/0209 25015KT 5000 NSW NSC\r\n"
                        + "FM011530 00000KT CAVOK=";
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        return Optional.of("TAF EFHK 010825Z 0109/0209 25015KT 5000\r\n"
                        + "FM011530 00000KT CAVOK=");
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_ERRORS;
    }

    @Override
    public void assertParsingIssues(final List<ConversionIssue> conversionIssues) {
        assertEquals("One parsing issue expected", 1, conversionIssues.size());
        assertTrue("NSW error not correctly reported", conversionIssues.get(0).getMessage().contains("NSW not allowed"));
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
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[]{TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, NO_SIGNIFICANT_WEATHER,
                CLOUD, TAF_FORECAST_CHANGE_INDICATOR, SURFACE_WIND, CAVOK, END_TOKEN});
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
