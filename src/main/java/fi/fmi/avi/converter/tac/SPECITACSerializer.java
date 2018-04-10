package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;

/**
 * Created by rinne on 06/03/2018.
 */
public class SPECITACSerializer extends METARTACSerializerBase<SPECI> {

    @Override
    protected Class<SPECI> getMessageClass() {
        return SPECI.class;
    }

    @Override
    protected Lexeme.Identity getStartTokenIdentity() {
        return Lexeme.Identity.SPECI_START;
    }

    @Override
    protected SPECI narrow(final AviationWeatherMessage msg, final ConversionHints hints) throws SerializingException {
        if (msg instanceof METAR) {
            return (SPECI) msg;
        } else {
            throw new SerializingException("Message to serialize is not a SPECI message POJO!");
        }
    }
}
