package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

public interface MessageTypeDetector {
    Class<? extends AviationWeatherMessageOrCollection> getMessageType();

    boolean detects(final LexemeSequence rawSequence);

}
