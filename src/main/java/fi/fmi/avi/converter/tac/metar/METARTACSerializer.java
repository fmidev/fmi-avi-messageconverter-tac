package fi.fmi.avi.converter.tac.metar;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.METAR;

public class METARTACSerializer extends METARTACSerializerBase<METAR> {

    @Override
    protected METAR narrow(AviationWeatherMessageOrCollection msg, ConversionHints hints) {
        if (msg instanceof METAR) {
            return (METAR) msg;
        } else {
            throw new ClassCastException("Message " + msg + " is not METAR");
        }
    }

    @Override
    protected LexemeIdentity getStartTokenIdentity() {
        return LexemeIdentity.METAR_START;
    }

    @Override
    protected Class<METAR> getMessageClass() {
        return METAR.class;
    }
}
