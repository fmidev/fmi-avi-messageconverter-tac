package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

;

/**
 * Created by rinne on 01/03/17.
 */
public abstract class FactoryBasedReconstructor implements TACTokenReconstructor {

	private LexingFactory factory;

    public void setLexingFactory(final LexingFactory factory) {
        this.factory = factory;
    }

    public LexingFactory getLexingFactory() {
        return this.factory;
    }

	protected Lexeme createLexeme(final String token) {
		return this.createLexeme(token, null, Lexeme.Status.UNRECOGNIZED);
	}

	protected Lexeme createLexeme(final String token, final Lexeme.Identity identity) {
		return this.createLexeme(token, identity, Lexeme.Status.OK);
	}

	protected Lexeme createLexeme(final String token, final Lexeme.Identity identity, final Lexeme.Status status) {
		if (this.factory != null) {
			return this.factory.createLexeme(token, identity, status);
		} else {
			throw new IllegalStateException("No LexingFactory injected");
		}
	}

    @Override
	public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ReconstructorContext<T> ctx)
			throws SerializingException {
		List<Lexeme> retval = new ArrayList<>();
        Optional<Lexeme> lexeme = getAsLexeme(msg, clz, ctx);
        lexeme.ifPresent(retval::add);
    	return retval;
    }
    
    /**
     * Override this unless the class overrides getAsLexemes(). The default implementation always throws RuntimeException.
     *
	 * @param msg the source message
	 * @param clz the class of the source message
	 * @param ctx context to guide the reconstructor
	 * @param <T> the type of the source message
     * @return the reconstructed Lexeme, if one could be created
     * @throws SerializingException when the Lexeme cannot be constructed
	 */
	public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, ReconstructorContext<T> ctx)
            throws SerializingException {
		throw new RuntimeException("Reconstructor does not implement getAsLexeme");
	}
}
