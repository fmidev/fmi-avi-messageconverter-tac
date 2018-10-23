package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.METAR;

public class METARDetector implements MessageTypeDetector {
    @Override
    public Class<? extends AviationWeatherMessageOrCollection> getMessageType() {
        return METAR.class;
    }

    @Override
    public boolean detects(final LexemeSequence rawSequence) {
        return "METAR".equals(rawSequence.getFirstLexeme().getTACToken());
    }
}
