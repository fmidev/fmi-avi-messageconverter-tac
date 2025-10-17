package fi.fmi.avi.converter.tac.swx.amd79;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.bulletin.AbstractTACBulletinSerializer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAmd79Bulletin;

public class SWXAmd79BulletinTACSerializer extends AbstractTACBulletinSerializer<SpaceWeatherAdvisoryAmd79, SpaceWeatherAmd79Bulletin> {

    private SWXAmd79TACSerializer swxSerializer;

    public void setSWXAmd79Serializer(final SWXAmd79TACSerializer serializer) {
        this.swxSerializer = serializer;
    }

    @Override
    protected SpaceWeatherAmd79Bulletin accepts(final AviationWeatherMessageOrCollection message) throws SerializingException {
        if (message instanceof SpaceWeatherAmd79Bulletin) {
            return (SpaceWeatherAmd79Bulletin) message;
        } else {
            throw new SerializingException("Can only serialize " + getBulletinClass().getSimpleName());
        }
    }

    @Override
    protected Class<SpaceWeatherAmd79Bulletin> getBulletinClass() {
        return SpaceWeatherAmd79Bulletin.class;
    }

    @Override
    protected LexemeSequence tokenizeSingleMessage(final SpaceWeatherAdvisoryAmd79 message, final ConversionHints hints) throws SerializingException {
        if (message != null) {
            if (swxSerializer != null) {
                return swxSerializer.tokenizeMessage(message, hints);
            }
        }
        throw new SerializingException("Unable to serialize null SpaceWeatherAdvisory");
    }

}
