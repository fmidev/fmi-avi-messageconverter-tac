package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.METARTACSerializer;
import fi.fmi.avi.converter.tac.SPECITACSerializer;
import fi.fmi.avi.converter.tac.TAFTACSerializer;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.taf.TAF;

public class AviMessageTACTokenizerImpl implements AviMessageTACTokenizer {
	private METARTACSerializer metarSerializer;
    private SPECITACSerializer speciSerializer;
    private TAFTACSerializer tafSerializer;

	public void setMETARSerializer(METARTACSerializer serializer) {
		this.metarSerializer = serializer;
	}

    public void setSPECISerializer(SPECITACSerializer serializer) {
        this.speciSerializer = serializer;
    }

	public void setTAFSerializer(TAFTACSerializer serializer) {
		this.tafSerializer = serializer;
	}

	public AviMessageTACTokenizerImpl() {
	}

	@Override
	public LexemeSequence tokenizeMessage(final AviationWeatherMessage msg) throws SerializingException {
		return this.tokenizeMessage(msg, null);
	}

	@Override
	public LexemeSequence tokenizeMessage(final AviationWeatherMessage msg, final ConversionHints hints) throws SerializingException {
        if (msg instanceof SPECI && this.speciSerializer != null) {
            return this.speciSerializer.tokenizeMessage(msg, hints);
        } else if (msg instanceof METARImpl && this.metarSerializer != null) {
            return this.metarSerializer.tokenizeMessage(msg, hints);
		} else if (msg instanceof TAF && this.tafSerializer != null) {
			return this.tafSerializer.tokenizeMessage(msg, hints);
		}
		throw new IllegalArgumentException("Do not know how to tokenize message of type " + msg.getClass().getCanonicalName());
	}

}
