package fi.fmi.avi.converter.tac.lexer.impl;

import static java.util.Optional.empty;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

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
        Optional<S> retval = empty();
        final Object value = this.parameters.get(name);
        if (value != null) {
            if (clz.isAssignableFrom(value.getClass())) {
                retval = Optional.of(clz.cast(value));
            }
        }
        return retval;
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
