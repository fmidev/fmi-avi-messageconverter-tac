package fi.fmi.avi.converter.tac.taf;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

import java.util.List;
import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;
import static org.junit.Assert.assertEquals;

public class TAFInvalidHorizontalVisibilityLexemeTest extends AbstractAviMessageTest<TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf_invalid_horizontal_visibility.json";
    }

    @Override
    public String getMessage() {
        return "TAF EFJY 152026Z 1521/1621 17006KT 9999 -SHSN OVC008\r\n" //
                + "TEMPO 1521/1604 5000 -FZDZSN BKN004\r\n" //
                + "BECMG 1604/1606 2500 BR BKN004\r\n" //
                + "TEMPO 1606/1612 5000 NSW BKN007\r\n" //
                + "BECMG 1612/1614 8000 NSW OVC007\r\n" //
                + "PROB30 1614/1621 400O BR BKN004=";
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_WARNINGS;
    }

    @Override
    public void assertParsingIssues(final List<ConversionIssue> conversionIssues) {
        assertEquals("One parsing issue expected", 1, conversionIssues.size());
        assertEquals(ConversionIssue.Type.SYNTAX, conversionIssues.get(0).getType());
        assertEquals("Lexing problem with '400O'", conversionIssues.get(0).getMessage());
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        // The invalid "400O" token is not parsed, so serialization produces output without it
        return Optional.of("TAF EFJY 152026Z 1521/1621 17006KT 9999 -SHSN OVC008\r\n" //
                + "TEMPO 1521/1604 5000 -FZDZSN BKN004\r\n" //
                + "BECMG 1604/1606 2500 BR BKN004\r\n" //
                + "TEMPO 1606/1612 5000 NSW BKN007\r\n" //
                + "BECMG 1612/1614 8000 NSW OVC007\r\n" //
                + "PROB30 1614/1621 BR BKN004=");
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        final ConversionHints hints = new ConversionHints(ConversionHints.TAF);
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

    @Override
    public ConversionHints getParserConversionHints() {
        final ConversionHints hints = new ConversionHints(ConversionHints.TAF);
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[]{
                TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, NO_SIGNIFICANT_WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, NO_SIGNIFICANT_WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, null, WEATHER, CLOUD, END_TOKEN
        });
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
    public Class<? extends TAF> getTokenizerImplementationClass() {
        return TAFImpl.class;
    }

}
