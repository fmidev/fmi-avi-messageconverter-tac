package fi.fmi.avi.converter.tac.swx;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.bulletin.AbstractTACBulletinSerializer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherBulletin;

public class SWXBulletinTACSerializer extends AbstractTACBulletinSerializer<SpaceWeatherAdvisory, SpaceWeatherBulletin> {

    private SWXTACSerializer swxSerializer;

    public void setSWXSerializer(final SWXTACSerializer serializer) {
        this.swxSerializer = serializer;
    }

    @Override
    protected SpaceWeatherBulletin accepts(final AviationWeatherMessageOrCollection message) throws SerializingException {
        if (message instanceof SpaceWeatherBulletin) {
            return (SpaceWeatherBulletin) message;
        } else {
            throw new SerializingException("Can only serialize SpaceWeatherBulletins");
        }
    }

    @Override
    protected Class<SpaceWeatherBulletin> getBulletinClass() {
        return SpaceWeatherBulletin.class;
    }

    @Override
    protected LexemeSequence tokenizeSingleMessage(final SpaceWeatherAdvisory message, final ConversionHints hints) throws SerializingException {
        if (message != null) {
            if (swxSerializer != null) {
                return swxSerializer.tokenizeMessage(message, hints);
            }
        }
        throw new SerializingException("Unable to serialize null SpaceWeatherAdvisory");
    }

}
