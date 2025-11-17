package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ReconstructorContext<T extends AviationWeatherMessageOrCollection> {

    private final ConversionHints hints;
    private final T source;

    private Map<String, Object> parameters;

    public ReconstructorContext(final T source) {
        this(source, ConversionHints.EMPTY);
    }

    public ReconstructorContext(final T source, final ConversionHints hints) {
        this.source = source;
        this.hints = hints;
        this.parameters = new HashMap<>();
    }

    public <S> Optional<S> getParameter(final String name, final Class<S> clz) {
        return Optional.ofNullable(getNullableParameter(name, clz));
    }

    public <S> S getMandatoryParameter(final String name, final Class<S> clz) throws SerializingException {
        final S value = getNullableParameter(name, clz);
        if (value == null) {
            throw new SerializingException(name + " context parameter not provided or is not of type " + clz.getName());
        } else {
            return value;
        }
    }

    private <S> @Nullable S getNullableParameter(final String name, final Class<S> clz) {
        final Object value = this.parameters.get(name);
        if (value != null) {
            if (clz.isInstance(value)) {
                return clz.cast(value);
            }
        }
        return null;
    }

    public ConversionHints getHints() {
        return hints;
    }

    public T getSource() {
        return source;
    }

    public Object setParameter(final String key, final Object value) {
        return this.parameters.put(key, value);
    }

    public Object removeParameter(final String key) {
        return this.parameters.remove(key);
    }

    public void setHint(final ConversionHints.Key key, final Object value) {
        this.hints.put(key, value);
    }

    public void clearHint(final ConversionHints.Key key) {
        this.hints.remove(key);
    }

    public ReconstructorContext<T> copyWithParameter(final String name, final Object value) {
        final ReconstructorContext<T> retval = new ReconstructorContext<>(this.source, this.hints);
        retval.parameters = new HashMap<>(this.parameters);
        retval.parameters.put(name, value);
        return retval;
    }
}
