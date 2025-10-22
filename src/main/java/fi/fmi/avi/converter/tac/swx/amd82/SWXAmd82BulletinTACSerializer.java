package fi.fmi.avi.converter.tac.swx.amd82;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.bulletin.AbstractTACBulletinSerializer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAmd82Bulletin;

public class SWXAmd82BulletinTACSerializer extends AbstractTACBulletinSerializer<SpaceWeatherAdvisoryAmd82, SpaceWeatherAmd82Bulletin> {

    private SWXAmd82TACSerializer swxSerializer;

    public void setSWXSerializer(final SWXAmd82TACSerializer serializer) {
        this.swxSerializer = serializer;
    }

    @Override
    protected SpaceWeatherAmd82Bulletin accepts(final AviationWeatherMessageOrCollection message) throws SerializingException {
        if (message instanceof SpaceWeatherAmd82Bulletin) {
            return (SpaceWeatherAmd82Bulletin) message;
        } else {
            throw new SerializingException("Can only serialize " + getBulletinClass().getSimpleName());
        }
    }

    @Override
    protected Class<SpaceWeatherAmd82Bulletin> getBulletinClass() {
        return SpaceWeatherAmd82Bulletin.class;
    }

    @Override
    protected LexemeSequence tokenizeSingleMessage(final SpaceWeatherAdvisoryAmd82 message, final ConversionHints hints) throws SerializingException {
        if (message != null) {
            if (swxSerializer != null) {
                return swxSerializer.tokenizeMessage(message, hints);
            }
        }
        throw new SerializingException("Unable to serialize null SpaceWeatherAdvisory");
    }

}
