package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AUTOMATED;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VARIABLE_WIND_DIRECTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METAR28Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar28.json";
	}
	
	@Override
	public String getMessage() {
        return "METAR EFTU 110820Z AUTO 35004KT 310V030 9999 VV008TCU 07/06 Q0999=";
    }

	@Override
	public Optional<String> getCanonicalMessage() {
        return Optional.of("METAR EFTU 110820Z AUTO 35004KT 310V030 9999 VV008 07/06 Q0999=");
    }

	@Override
	public ConversionResult.Status getExpectedParsingStatus() {
		return ConversionResult.Status.FAIL;
	}

	@Override
	public void assertParsingIssues(List<ConversionIssue> conversionIssues) {
		assertEquals(2, conversionIssues.size());

		ConversionIssue issue = conversionIssues.get(0);
		assertEquals(ConversionIssue.Type.SYNTAX, issue.getType());
		assertTrue("Unexpected error message",issue.getMessage().indexOf("Input message lexing was not fully successful") > -1);

		issue = conversionIssues.get(1);
		assertEquals(ConversionIssue.Type.SYNTAX, issue.getType());
		assertTrue("Unexpected error message", issue.getMessage().indexOf("'CB' and 'TCU' not allowed with 'VV'") > -1);

	}

	@Override
	public LexemeIdentity[] getLexerTokenSequenceIdentity() {
		return spacify(new LexemeIdentity[] {
				METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, AUTOMATED, SURFACE_WIND, VARIABLE_WIND_DIRECTION, HORIZONTAL_VISIBILITY, null,
				AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH, END_TOKEN
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
