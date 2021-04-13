package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VALID_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.WEATHER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.List;
import java.util.Optional;

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

public class Taf10VVWithTCUTest extends AbstractAviMessageTest<TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf10_VV_with_TCU.json";
    }

    @Override
    public String getMessage() {
        return "TAF ESNS 301130Z 3012/3021 15008KT 9999 OVC008\r\n" //
                + "TEMPO 3018/3021 0900 SNRA VV002TCU=";
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TAF);
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

    @Override
    public ConversionHints getParserConversionHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TAF);
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

    @Override
    public LexemeIdentity[] getLexerTokenSequenceIdentity() {
        return spacify(new LexemeIdentity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, null, END_TOKEN });
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_WARNINGS;
    }

    @Override
    public void assertParsingIssues(final List<ConversionIssue> conversionIssues) {
        assertEquals(1, conversionIssues.size());
        assertSame(ConversionIssue.Type.SYNTAX, conversionIssues.get(0).getType());
        assertEquals("Lexing problem with 'VV002TCU': 'CB' and 'TCU' not allowed with 'VV'", conversionIssues.get(0).getMessage());
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        return Optional.of("TAF ESNS 301130Z 3012/3021 15008KT 9999 OVC008\r\n" //
                + "TEMPO 3018/3021 0900 SNRA=");
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
