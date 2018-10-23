package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAF;

public class TAFDetector implements MessageTypeDetector {
    @Override
    public Class<? extends AviationWeatherMessageOrCollection> getMessageType() {
        return TAF.class;
    }

    @Override
    public boolean detects(final LexemeSequence rawSequence) {
        return "TAF".equals(rawSequence.getFirstLexeme().getTACToken());
    }
}
