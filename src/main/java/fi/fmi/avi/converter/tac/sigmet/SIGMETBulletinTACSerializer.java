package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.bulletin.AbstractTACBulletinSerializer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;

public class SIGMETBulletinTACSerializer extends AbstractTACBulletinSerializer<SIGMET, SIGMETBulletin> {

    private SIGMETTACSerializer sigmetSerializer;

    public void setSigmetSerializer(final SIGMETTACSerializer serializer) {
        this.sigmetSerializer = serializer;
    }

    @Override
    protected SIGMETBulletin accepts(final AviationWeatherMessageOrCollection message) throws SerializingException {
        if (message instanceof SIGMETBulletin) {
            return (SIGMETBulletin) message;
        } else {
            throw new SerializingException("Can only serialize SIGMETBulletins");
        }
    }

    @Override
    protected Class<SIGMETBulletin> getBulletinClass() {
        return SIGMETBulletin.class;
    }

    @Override
    protected LexemeSequence tokenizeSingleMessage(final SIGMET message, final ConversionHints hints) throws SerializingException {
        if (message != null) {
            if (sigmetSerializer != null) {
                return sigmetSerializer.tokenizeMessage(message, hints);
            }
        }
        throw new SerializingException("Unable to serialize null SIGMET");
    }

}
