package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;

public class SPECITACSerializer extends METARTACSerializerBase<SPECI> {

    @Override
    protected SPECI narrow(AviationWeatherMessage msg, ConversionHints hints) {
        if (msg instanceof SPECI) {
            return (SPECI) msg;
        } else {
            throw new ClassCastException("Message " + msg + " is not SPECI");
        }
    }

    @Override
    protected Lexeme.Identity getStartTokenIdentity() {
        return Lexeme.Identity.SPECI_START;
    }

    @Override
    protected Class<SPECI> getMessageClass() {
        return SPECI.class;
    }
}
