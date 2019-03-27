package fi.fmi.avi.converter.tac.bulletin;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;

public class GenericMeteorologicalBulletinTACSerializer extends AbstractTACBulletinSerializer<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {

    private AviMessageLexer lexer;

    public void setLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    protected GenericMeteorologicalBulletin accepts(final AviationWeatherMessageOrCollection message) throws SerializingException {
        if (message instanceof GenericMeteorologicalBulletin) {
            return (GenericMeteorologicalBulletin) message;
        } else {
            throw new SerializingException("Can only serialize GenericMeteorologicalBulletins");
        }
    }

    @Override
    protected Class<GenericMeteorologicalBulletin> getBulletinClass() {
        return GenericMeteorologicalBulletin.class;
    }

    @Override
    protected LexemeSequence tokenizeSingleMessage(final GenericAviationWeatherMessage message, final ConversionHints hints) throws SerializingException {
        if (message != null) {
            return this.lexer.lexMessage(message.getOriginalMessage(), hints);
        }
        throw new SerializingException("Unable to serialize null message");
    }

}
