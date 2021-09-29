package fi.fmi.avi.converter.tac.metar;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.SPECI;

public class SPECITACSerializer extends METARTACSerializerBase<SPECI> {

    @Override
    protected SPECI narrow(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) {
        if (msg instanceof SPECI) {
            return (SPECI) msg;
        } else {
            throw new ClassCastException("Message " + msg + " is not SPECI");
        }
    }

    @Override
    protected LexemeIdentity getStartTokenIdentity() {
        return LexemeIdentity.SPECI_START;
    }

    @Override
    protected Class<SPECI> getMessageClass() {
        return SPECI.class;
    }
}
