package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

import java.util.Optional;

import static java.util.Objects.requireNonNull;

public abstract class AbstractFixedContentReconstructor extends FactoryBasedReconstructor {
    private final String lexemeContent;
    private final LexemeIdentity lexemeIdentity;

    protected AbstractFixedContentReconstructor(final String lexemeContent, final LexemeIdentity lexemeIdentity) {
        this.lexemeContent = requireNonNull(lexemeContent, "lexemeContent");
        this.lexemeIdentity = requireNonNull(lexemeIdentity, "lexemeIdentity");
    }

    protected abstract <T extends AviationWeatherMessageOrCollection> boolean isReconstructable(T msg, Class<T> clz, ReconstructorContext<T> ctx);

    @Override
    public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
        return isReconstructable(msg, clz, ctx)
                ? Optional.of(createLexeme(lexemeContent, lexemeIdentity))
                : Optional.empty();
    }
}
