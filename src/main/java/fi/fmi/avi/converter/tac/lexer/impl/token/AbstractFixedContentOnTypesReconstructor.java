package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static java.util.Objects.requireNonNull;

public abstract class AbstractFixedContentOnTypesReconstructor extends AbstractFixedContentReconstructor {
    private final List<Class<? extends AviationWeatherMessage>> messageTypes;

    @SafeVarargs
    protected AbstractFixedContentOnTypesReconstructor(final String lexemeContent, final LexemeIdentity lexemeIdentity, final Class<? extends AviationWeatherMessage>... messageTypes) {
        this(lexemeContent, lexemeIdentity, Arrays.asList(requireNonNull(messageTypes, "messageTypes")));
    }

    protected AbstractFixedContentOnTypesReconstructor(final String lexemeContent, final LexemeIdentity lexemeIdentity, final List<Class<? extends AviationWeatherMessage>> messageTypes) {
        super(lexemeContent, lexemeIdentity);
        this.messageTypes = Collections.unmodifiableList(new ArrayList<>(requireNonNull(messageTypes, "messageTypes")));
    }


    @Override
    protected <T extends AviationWeatherMessageOrCollection> boolean isReconstructable(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
        for (final Class<? extends AviationWeatherMessage> messageType : messageTypes) {
            if (messageType.isAssignableFrom(clz)) {
                return true;
            }
        }
        return false;
    }
}
