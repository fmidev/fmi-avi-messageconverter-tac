package fi.fmi.avi.converter.tac.bulletin;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.AIRMETBulletin;

public class AIRMETBulletinTACSerializer extends AbstractTACBulletinSerializer<AIRMET, AIRMETBulletin> {

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
            if (message.getTranslatedTAC().isPresent()) {
                return this.getLexingFactory().createLexemeSequence(message.getTranslatedTAC().get(), hints);
            }
        }
        throw new SerializingException("Unabled to serialize AIRMET, either null or translatedTAC is not present");
    }
}
