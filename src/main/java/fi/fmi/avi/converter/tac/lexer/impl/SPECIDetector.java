package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.SPECI;

public class SPECIDetector implements MessageTypeDetector {
    @Override
    public Class<? extends AviationWeatherMessageOrCollection> getMessageType() {
        return SPECI.class;
    }

    @Override
    public boolean detects(final LexemeSequence rawSequence) {
        return "SPECI".equals(rawSequence.getFirstLexeme().getTACToken());
    }
}
