package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.model.MessageType;

/**
 * Created by rinne on 21/12/16.
 */
public class AviMessageLexerImpl implements AviMessageLexer {
    private static final Logger LOG = LoggerFactory.getLogger(AviMessageLexerImpl.class);
    private static final int MAX_ITERATIONS = 100;

    final private List<RecognizingAviMessageTokenLexer> tokenLexers = new ArrayList<>();

    private LexingFactory factory;

    public void setLexingFactory(final LexingFactory factory) {
        this.factory = factory;
    }

    public LexingFactory getLexingFactory() {
        return this.factory;
    }

    public void addTokenLexer(final RecognizingAviMessageTokenLexer l) {
        this.tokenLexers.add(l);
    }

    @Override
    public LexemeSequence lexMessage(final String input) {
        return this.lexMessage(input, null);
    }

    @Override
    public LexemeSequence lexMessage(final String input, final ConversionHints hints) {
        if (this.factory == null) {
            throw new IllegalStateException("LexingFactory not injected");
        }
        final LexemeSequence result = this.factory.createLexemeSequence(input, hints);
        final Optional<RecognizingAviMessageTokenLexer> tokenLexer = this.tokenLexers.stream()
                .filter((lexer) -> lexer.getSuitablityTester().test(result)).findFirst();
        if (tokenLexer.isPresent()) {
            boolean lexemesChanged = true;
            int iterationCount = 0;
            while (lexemesChanged && iterationCount < MAX_ITERATIONS) {
                lexemesChanged = false;
                iterationCount++;
                int oldHashCode;
                final List<Lexeme> lexemes = result.getLexemes()
                        .stream()
                        .filter(l -> l.getIdentificationCertainty() < 1.0)
                        .collect(Collectors.toList());
                for (final Lexeme lexeme : lexemes) {
                    oldHashCode = lexeme.hashCode();
                    lexeme.accept(tokenLexer.get(), hints);
                    lexemesChanged = lexemesChanged || oldHashCode != lexeme.hashCode();
                }
            }
            if (iterationCount == MAX_ITERATIONS) {
                LOG.warn("Lexing result for {} did not stabilize within the maximum iteration count " + MAX_ITERATIONS + ", result may be incomplete",
                        result.getFirstLexeme().getIdentity());
            }
        }
        return result;
    }

    /**
     * Tries to recognize the given String as one of the aviation message types in
     * {@link MessageType}. Must use the same
     * logic as {@link #lexMessage(String, ConversionHints)} does internally.
     *
     * @param input
     *         the TAC encoded message
     * @param hints
     *         parsing hints to be passed to the lexer implementation
     *
     * @return the type if recognized
     */
    @Override
    public Optional<MessageType> recognizeMessageType(final String input, final ConversionHints hints) {
        if (this.factory == null) {
            throw new IllegalStateException("LexingFactory not injected");
        }
        final LexemeSequence result = this.factory.createLexemeSequence(input, hints);
        if (!this.tokenLexers.isEmpty()) {
            final Optional<RecognizingAviMessageTokenLexer> tokenLexer = this.tokenLexers.stream()
                    .filter((lexer) -> lexer.getSuitablityTester().test(result)).findFirst();
            if (tokenLexer.isPresent()) {
                return Optional.of(tokenLexer.get().getMessageType());
            }
        }
        return Optional.empty();
    }

}
