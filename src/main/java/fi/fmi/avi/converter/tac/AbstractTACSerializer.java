package fi.fmi.avi.converter.tac;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.TACTokenReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudLayer;

/**
 * Created by rinne on 07/06/17.
 */
public abstract class AbstractTACSerializer<S extends AviationWeatherMessage> implements AviMessageSpecificConverter<S, String>, AviMessageTACTokenizer {

    private Map<Lexeme.Identity, TACTokenReconstructor> reconstructors = new HashMap<>();

    private LexingFactory factory;

    public void setLexingFactory(final LexingFactory factory) {
        this.factory = factory;
    }

    public LexingFactory getLexingFactory() {
        return this.factory;
    }

    public void addReconstructor(final Lexeme.Identity id, TACTokenReconstructor reconstructor) {
        reconstructor.setLexingFactory(this.factory);
        this.reconstructors.put(id, reconstructor);
    }

    public TACTokenReconstructor removeReconstructor(final Lexeme.Identity id) {
        return this.reconstructors.remove(id);
    }

    @Override
    public abstract LexemeSequence tokenizeMessage(final AviationWeatherMessage msg) throws SerializingException;

    @Override
    public abstract LexemeSequence tokenizeMessage(final AviationWeatherMessage msg, final ConversionHints hints) throws SerializingException;

    public TACTokenReconstructor getReconstructor(final Lexeme.Identity id) {
        return this.reconstructors.get(id);
    }

    protected <V extends AviationWeatherMessage> int appendCloudLayers(final LexemeSequenceBuilder builder, final V msg, final Class<V> clz,
            final List<CloudLayer> layers, final ReconstructorContext<V> ctx) throws SerializingException {
        int retval = 0;
        if (layers != null) {
            for (CloudLayer layer : layers) {
                ctx.setParameter("layer", layer);
                retval += appendToken(builder, Lexeme.Identity.CLOUD, msg, clz, ctx);
                retval += appendWhitespace(builder, ' ');
            }
        }
        return retval;
    }

    protected <V extends AviationWeatherMessage> int appendToken(final LexemeSequenceBuilder builder, final Lexeme.Identity id, final V msg, final Class<V> clz,
                                                                 final ReconstructorContext<V> ctx) throws SerializingException {
        TACTokenReconstructor rec = this.reconstructors.get(id);
        int retval = 0;
        if (rec != null) {
            List<Lexeme> list = rec.getAsLexemes(msg, clz, ctx);
            if (list != null) {
                for (Lexeme l : list) {
                    builder.append(l);
                    retval++;
                }
            }
        }
        return retval;
    }

    protected int appendWhitespace(final LexemeSequenceBuilder builder, final char toAppend) {
        if (Character.isWhitespace(toAppend)) {
            builder.append(factory.createLexeme(String.valueOf(toAppend), Lexeme.Identity.WHITE_SPACE));
        } else {
            throw new IllegalArgumentException("Character '" + toAppend + "' is not whitespace");
        }
        return 1;
    }

}
