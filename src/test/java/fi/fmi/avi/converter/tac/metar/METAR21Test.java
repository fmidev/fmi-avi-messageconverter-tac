package fi.fmi.avi.converter.tac.metar;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CAVOK;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.METAR_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.RUNWAY_STATE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SURFACE_WIND;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METAR21Test extends AbstractAviMessageTest<METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar21.json";
	}

	@Override
	public String getMessage() {
		return "METAR EFTU 011350Z VRB02KT CAVOK 22/12 Q1008 R15L///////=";
	}

	@Override
	public LexemeIdentity[] getLexerTokenSequenceIdentity() {
		return spacify(new LexemeIdentity[] { METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, SURFACE_WIND, CAVOK, AIR_DEWPOINT_TEMPERATURE, AIR_PRESSURE_QNH,
				RUNWAY_STATE, END_TOKEN });
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
