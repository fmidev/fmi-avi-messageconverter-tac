package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.regex.Pattern;

import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAFBulletin;

public class TAFBulletinDetector implements MessageTypeDetector {
    @Override
    public Class<? extends AviationWeatherMessageOrCollection> getMessageType() {
        return TAFBulletin.class;
    }

    @Override
    public boolean detects(final LexemeSequence rawSequence) {
        Pattern tafBulletinStart = Pattern.compile("^(FC|T)([A-Z]{2})([0-9]{2})$");
        return tafBulletinStart.matcher(rawSequence.getFirstLexeme().getTACToken()).matches();
    }
}
