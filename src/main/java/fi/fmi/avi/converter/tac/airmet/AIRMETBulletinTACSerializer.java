package fi.fmi.avi.converter.tac.airmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.bulletin.AbstractTACBulletinSerializer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.AIRMETBulletin;

public class AIRMETBulletinTACSerializer extends AbstractTACBulletinSerializer<AIRMET, AIRMETBulletin> {

    private AIRMETTACSerializer airmetSerializer;

    public void setAirmetSerializer(final AIRMETTACSerializer serializer) {
        this.airmetSerializer = serializer;
    }

    @Override
    protected AIRMETBulletin accepts(final AviationWeatherMessageOrCollection message) throws SerializingException {
        if (message instanceof AIRMETBulletin) {
            return (AIRMETBulletin) message;
        } else {
            throw new SerializingException("Can only serialize AIRMETBulletins");
        }
    }

    @Override
    protected Class<AIRMETBulletin> getBulletinClass() {
        return AIRMETBulletin.class;
    }

    @Override
    protected LexemeSequence tokenizeSingleMessage(final AIRMET message, final ConversionHints hints) throws SerializingException {
        if (message != null) {
            if (airmetSerializer != null) {
                return airmetSerializer.tokenizeMessage(message, hints);
            }
        }
        throw new SerializingException("Unable to serialize null AIRMET");
    }

}
