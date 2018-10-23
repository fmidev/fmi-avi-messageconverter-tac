package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

/**
 * Created by rinne on 21/12/16.
 */
public class AviMessageLexerImpl implements AviMessageLexer {
    private static final Logger LOG = LoggerFactory.getLogger(AviMessageLexerImpl.class);
    private static final int MAX_ITERATIONS = 100;

    private List<MessageTypeDetector> detectors = new ArrayList<>();
    private Map<Class<? extends AviationWeatherMessageOrCollection>, RecognizingAviMessageTokenLexer> tokenLexers = new HashMap<>();

    private LexingFactory factory;

    public void setLexingFactory(final LexingFactory factory) {
        this.factory = factory;
    }

    public LexingFactory getLexingFactory() {
        return this.factory;
    }

    public void addTokenLexer(final MessageTypeDetector detector, final RecognizingAviMessageTokenLexer l) {
        this.detectors.add(detector);
        this.tokenLexers.put(detector.getMessageType(), l);
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
        LexemeSequence result = this.factory.createLexemeSequence(input, hints);
        RecognizingAviMessageTokenLexer tokenLexer = null;

        for (MessageTypeDetector detector : detectors) {
            if (detector.detects(result)) {
                tokenLexer = tokenLexers.get(detector.getMessageType());
            }
        }
        if (tokenLexer != null) {
            boolean lexemesChanged = true;
            int iterationCount = 0;
            while (lexemesChanged && iterationCount < MAX_ITERATIONS) {
                lexemesChanged = false;
                iterationCount++;
                int oldHashCode;
                List<Lexeme> lexemes = result.getLexemes()
                        .stream()
                        .filter(l -> l.getIdentificationCertainty() < 1.0)
                        .collect(Collectors.toList());
                for (Lexeme lexeme : lexemes) {
                    oldHashCode = lexeme.hashCode();
                    lexeme.accept(tokenLexer, hints);
                    lexemesChanged = lexemesChanged || oldHashCode != lexeme.hashCode();
                }
            }
            if (iterationCount == MAX_ITERATIONS) {
                LOG.warn("Lexing result for " + result.getFirstLexeme().getIdentity() + " did not stabilize within the maximum iteration count "
                        + MAX_ITERATIONS + ", result may be incomplete");
            }
        }
        return result;
    }

}
