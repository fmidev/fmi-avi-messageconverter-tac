package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AUTOMATED;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VARIABLE_WIND_DIRECTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult.Status;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METAR15Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar15.json";
	}
	
	@Override
	public String getMessage() {
		return
				"EFKK 091050Z AUTO 01009KT 340V040 9999 FEW012 BKN046 ///// Q////=";
	}
	
	@Override
	public Optional<String> getCanonicalMessage() {
		return Optional.of(
				"EFKK 091050Z AUTO 01009KT 340V040 9999 FEW012 BKN046=");
	}
	
	@Override
	public String getTokenizedMessagePrefix() {
        return "METAR ";
    }
	
	@Override
    public ConversionHints getLexerParsingHints() {
        return ConversionHints.METAR;
    }

    @Override
    public ConversionHints getParserConversionHints() {
        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, ConversionHints.VALUE_MESSAGE_TYPE_METAR);
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        return hints;
    }

	@Override
    public Status getExpectedParsingStatus() {
        return Status.WITH_ERRORS;
    }

    @Override
    public void assertParsingIssues(List<ConversionIssue> conversionIssues) {
        assertEquals(4, conversionIssues.size());

        ConversionIssue issue = conversionIssues.get(0);
        assertEquals(ConversionIssue.Type.SYNTAX, issue.getType());
        assertTrue("Unexpected error message", issue.getMessage().indexOf("Values for air and/or dew point temperature missing") > -1);

        issue = conversionIssues.get(1);
        assertEquals(ConversionIssue.Type.SYNTAX, issue.getType());
        assertTrue("Unxexpected error message", issue.getMessage().indexOf("Missing value for air pressure") > -1);

        issue = conversionIssues.get(2);
        assertEquals(ConversionIssue.Type.MISSING_DATA, issue.getType());
        assertTrue("Unxexpected error message", issue.getMessage().indexOf("Missing air temperature and dewpoint temperature values in /////") > -1);

        issue = conversionIssues.get(3);
        assertEquals(ConversionIssue.Type.MISSING_DATA, issue.getType());
        assertTrue("Unxexpected error message", issue.getMessage().indexOf("Missing air pressure value: Q////") > -1);


    }

	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(new Identity[] {
				METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, AUTOMATED, SURFACE_WIND, VARIABLE_WIND_DIRECTION,
                HORIZONTAL_VISIBILITY, CLOUD, CLOUD, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, END_TOKEN
		});
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
