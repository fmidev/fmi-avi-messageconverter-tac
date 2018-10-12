package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAFBulletin;

public class TAFBulletinTACSerializer extends AbstractTACSerializer<TAFBulletin> {

    private TAFTACSerializer tafSerializer;

    public void setTafSerializer(final TAFTACSerializer serializer) {
        if (serializer.getVariant() != TAFTACSerializer.EncodingVariant.BULLETIN) {
            throw new IllegalArgumentException("Encoding variant must be BULLETIN");
        }
        this.tafSerializer = serializer;
    }

    @Override
    public ConversionResult<String> convertMessage(final TAFBulletin input, final ConversionHints hints) {
        ConversionResult<String> result = new ConversionResult<>();
        try {
            LexemeSequence seq = tokenizeMessage(input, hints);
            result.setConvertedMessage(seq.getTAC());
        } catch (SerializingException se) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.OTHER, se.getMessage()));
        }
        return result;
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        if (!(msg instanceof TAFBulletin)) {
            throw new SerializingException("I can only tokenize TAFBulletins!");
        }
        TAFBulletin bulletin = (TAFBulletin) msg;
        //TODO: the thing
        return null;
    }

}
