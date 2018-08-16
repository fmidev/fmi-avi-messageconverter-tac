package fi.fmi.avi.converter.tac.metar;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.*;

public class METAR35Test extends AbstractAviMessageTest<String, METAR> {

	@Override
	public String getJsonFilename() {
		return "metar/metar35.json";
	}
	
	@Override
	public String getMessage() {
        return "METAR EFTU 110820Z AUTO 35004KT 310V030 9999 VV/// 07/06 Q0999=";
    }

	@Override
	public Optional<String> getCanonicalMessage() {
        return Optional.of("METAR EFTU 110820Z AUTO 35004KT 310V030 9999 VV/// 07/06 Q0999=");
    }

    @Override
	public String getTokenizedMessagePrefix() {
		return "";
	}

	@Override
	public Identity[] getLexerTokenSequenceIdentity() {
		return spacify(new Identity[] {
				METAR_START, AERODROME_DESIGNATOR, ISSUE_TIME, AUTOMATED, SURFACE_WIND, VARIABLE_WIND_DIRECTION, HORIZONTAL_VISIBILITY, CLOUD,
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
