package fi.fmi.avi.converter.tac.lexer.bulletin;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;

public class SIGMETBulletinTACSerializer extends AbstractTACBulletinSerializer<SIGMET, SIGMETBulletin> {

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
            if (message.getTranslatedTAC().isPresent()) {
                return this.getLexingFactory().createLexemeSequence(message.getTranslatedTAC().get(), hints);
            }
        }
        throw new SerializingException("Unabled to serialize SIGMET, either null or translatedTAC is not present");
    }
}
