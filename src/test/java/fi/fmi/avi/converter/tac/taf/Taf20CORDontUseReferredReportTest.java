package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CAVOK;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CORRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VALID_TIME;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

public class Taf20CORDontUseReferredReportTest extends AbstractAviMessageTest<String, TAF> {

	@Override
	public String getJsonFilename() {
		return "taf/taf20cor_dont_use_referred_report.json";
	}
	
	@Override
	public String getMessage() {
		return
				"TAF COR EFHK 100912Z 1009/1109 00000KT CAVOK=";
	}
	
	@Override
	public String getTokenizedMessagePrefix() {
		return "";
	}

	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(new Identity[] {
				TAF_START, CORRECTION, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, CAVOK, END_TOKEN });
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
	public ConversionHints getLexerParsingHints() {
		final ConversionHints retval = new ConversionHints(super.getLexerParsingHints());
		retval.put(ConversionHints.KEY_TAF_REFERENCE_POLICY, ConversionHints
				.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_CNL);
		return retval;
	}

	@Override
	public ConversionHints getParserConversionHints() {
		final ConversionHints retval = new ConversionHints(super.getParserConversionHints());
		retval.put(ConversionHints.KEY_TAF_REFERENCE_POLICY, ConversionHints
				.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_CNL);
		return retval;
	}

	@Override
	public ConversionHints getTokenizerParsingHints() {
		final ConversionHints retval = new ConversionHints(super.getTokenizerParsingHints());
		retval.put(ConversionHints.KEY_TAF_REFERENCE_POLICY, ConversionHints
				.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_CNL);
		return retval;
	}
}
