package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

/**
 * Created by rinne on 06/03/2018.
 */
public class METARTACSerializer extends METARTACSerializerBase<METAR> {

    @Override
    protected Class<METAR> getMessageClass() {
        return METARImpl.class;
    }

    @Override
    protected Lexeme.Identity getStartTokenIdentity() {
        return Lexeme.Identity.METAR_START;
    }

    @Override
    protected METAR narrow(final AviationWeatherMessage msg, final ConversionHints hints) throws SerializingException {
        if (msg instanceof METARImpl) {
            return (METAR) msg;
        } else {
            throw new SerializingException("Message to serialize is not a METARImpl message POJO!");
        }
    }
}
