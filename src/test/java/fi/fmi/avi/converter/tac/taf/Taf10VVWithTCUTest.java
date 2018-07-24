package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_CHANGE_FORECAST_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VALID_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class Taf10VVWithTCUTest extends AbstractAviMessageTest<String, TAF> {

    @Override
    public String getJsonFilename() {
        return "taf/taf10_VV_with_TCU.json";
    }

    @Override
    public String getMessage() {
        return "ESNS 301130Z 3012/3021 15008KT 9999 OVC008\n" + "TEMPO 3018/3021 0900 SNRA VV002TCU=";
    }

    @Override
    public String getTokenizedMessagePrefix() {
        return "TAF ";
    }

    @Override
    public ConversionHints getLexerParsingHints() {
        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, ConversionHints.VALUE_MESSAGE_TYPE_TAF);
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

    @Override
    public ConversionHints getParserConversionHints() {
        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, ConversionHints.VALUE_MESSAGE_TYPE_TAF);
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

    @Override
    public Identity[] getLexerTokenSequenceIdentity() {
        return spacify(new Identity[] { TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER, null, END_TOKEN });
    }

    @Override
    public ConversionResult.Status getExpectedParsingStatus() {
        return ConversionResult.Status.WITH_ERRORS;
    }

    @Override
    public void assertParsingIssues(List<ConversionIssue> conversionIssues) {
        assertTrue(conversionIssues.size() == 1);
        assertTrue(ConversionIssue.Type.SYNTAX == conversionIssues.get(0).getType());
        assertTrue("Lexing problem with 'VV002TCU': 'CB' and 'TCU' not allowed with 'VV'".equals(conversionIssues.get(0).getMessage()));
    }

    @Override
    public Optional<String> getCanonicalMessage() {
        return Optional.of("ESNS 301130Z 3012/3021 15008KT 9999 OVC008\n" + "TEMPO 3018/3021 0900 SNRA=");
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
