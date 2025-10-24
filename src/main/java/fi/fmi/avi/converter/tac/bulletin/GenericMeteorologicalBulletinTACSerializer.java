package fi.fmi.avi.converter.tac.bulletin;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;

public class GenericMeteorologicalBulletinTACSerializer extends AbstractTACBulletinSerializer<GenericAviationWeatherMessage, GenericMeteorologicalBulletin> {

    private AviMessageLexer lexer;

    public void setLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public ConversionResult<String> convertMessage(final AviationWeatherMessageOrCollection input, final ConversionHints hints) {
        final ConversionResult<String> result = new ConversionResult<>();
        try {
            final LexemeSequence seq = tokenizeMessage(input, hints);
            result.setConvertedMessage(seq.getTAC());
        } catch (final SerializingException se) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, se.getMessage()));
        }
        return result;
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
            if (GenericAviationWeatherMessage.Format.TAC == message.getMessageFormat()) {
                return this.lexer.lexMessage(message.getOriginalMessage(), hints);
            } else {
                throw new SerializingException("The originalMessage content is not in TAC format, automatic conversion from IWXXM not supported");
            }
        }
        throw new SerializingException("Unable to serialize null message");
    }

}
