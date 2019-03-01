package fi.fmi.avi.converter.tac.lexer.bulletin;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.taf.TAFTACSerializer;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;

public class TAFBulletinTACSerializer extends AbstractTACBulletinSerializer<TAF, TAFBulletin> {

    private TAFTACSerializer tafSerializer;

    public void setTafSerializer(final TAFTACSerializer serializer) {
        this.tafSerializer = serializer;
    }

    @Override
    protected TAFBulletin accepts(final AviationWeatherMessageOrCollection message) throws SerializingException {
        if (message instanceof TAFBulletin) {
            return (TAFBulletin) message;
        } else {
            throw new SerializingException("Can only serialize TAFBulletins");
        }
    }

    @Override
    protected Class<TAFBulletin> getBulletinClass() {
        return TAFBulletin.class;
    }

    @Override
    protected LexemeSequence tokenizeSingleMessage(final TAF message, final ConversionHints hints) throws SerializingException {
        if (message != null) {
            if (tafSerializer != null) {
                return tafSerializer.tokenizeMessage(message, hints);
            }
        }
        throw new SerializingException("Unabled to serialize null TAF");
    }

}
