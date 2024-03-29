package fi.fmi.avi.converter.tac;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.TACTokenReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;

/**
 * Created by rinne on 07/06/17.
 */
public abstract class AbstractTACSerializer<S extends AviationWeatherMessageOrCollection>
        implements AviMessageSpecificConverter<S, String>, AviMessageTACTokenizer {

    private final Map<LexemeIdentity, TACTokenReconstructor> reconstructors = new HashMap<>();

    private LexingFactory factory;

    public LexingFactory getLexingFactory() {
        return this.factory;
    }

    public void setLexingFactory(final LexingFactory factory) {
        this.factory = factory;
    }

    public void addReconstructor(final LexemeIdentity id, final TACTokenReconstructor reconstructor) {
        reconstructor.setLexingFactory(this.factory);
        this.reconstructors.put(id, reconstructor);
    }

    public TACTokenReconstructor removeReconstructor(final LexemeIdentity id) {
        return this.reconstructors.remove(id);
    }

    @Override
    public abstract LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException;

    @Override
    public abstract LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException;

    public TACTokenReconstructor getReconstructor(final LexemeIdentity id) {
        return this.reconstructors.get(id);
    }

    protected <V extends AviationWeatherMessage> int appendCloudLayers(final LexemeSequenceBuilder builder, final V msg, final Class<V> clz,
            final List<? extends CloudLayer> layers, final ReconstructorContext<V> ctx) throws SerializingException {
        int retval = 0;
        if (layers != null) {
            for (final CloudLayer layer : layers) {
                ctx.setParameter("layer", layer);
                retval += appendToken(builder, LexemeIdentity.CLOUD, msg, clz, ctx);
                retval += appendWhitespace(builder, MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
        return retval;
    }

    protected <V extends AviationWeatherMessageOrCollection> int appendToken(final LexemeSequenceBuilder builder, final LexemeIdentity id, final V msg,
            final Class<V> clz, final ReconstructorContext<V> ctx) throws SerializingException {
        final TACTokenReconstructor rec = this.reconstructors.get(id);
        int retval = 0;
        if (rec != null) {
            final List<Lexeme> list = rec.getAsLexemes(msg, clz, ctx);
            if (list != null) {
                for (final Lexeme l : list) {
                    builder.append(l);
                    retval++;
                }
            }
        }
        return retval;
    }

    protected int appendWhitespace(final LexemeSequenceBuilder builder, final MeteorologicalBulletinSpecialCharacter toAppend) {
        return appendWhitespace(builder, toAppend, 1);
    }

    protected int appendWhitespace(final LexemeSequenceBuilder builder, final MeteorologicalBulletinSpecialCharacter toAppend, final int count) {
        for (int i = 0; i < count; i++) {
            final Lexeme l = factory.createLexeme(toAppend.getContent(), LexemeIdentity.WHITE_SPACE);
            l.setParsedValue(Lexeme.ParsedValueName.TYPE, toAppend);
            builder.append(l);
        }
        return count;
    }

}
