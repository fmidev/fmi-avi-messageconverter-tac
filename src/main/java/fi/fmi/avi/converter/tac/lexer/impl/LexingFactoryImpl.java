package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;

/**
 * Created by rinne on 10/02/17.
 */

public class LexingFactoryImpl implements LexingFactory {

    @Override
    public LexemeSequence createLexemeSequence(final String input, final ConversionHints hints) {
        LexemeSequenceImpl result = new LexemeSequenceImpl(input);
        appendArtifialStartTokenIfNecessary(input, result, hints);
        return result;
    }

    @Override
    public LexemeSequenceBuilder createLexemeSequenceBuilder() {
        return new LexemeSequenceBuilderImpl();
    }

    @Override
    public Lexeme createLexeme(final String token) {
        return new LexemeImpl(token);
    }

    @Override
    public Lexeme createLexeme(final String token, final Lexeme.Identity identity) {
        return new LexemeImpl(token, identity);
    }

    @Override
    public Lexeme createLexeme(final String token, final Lexeme.Identity identity, final Lexeme.Status status) {
        return new LexemeImpl(token, identity, status);
    }

    private static void appendArtifialStartTokenIfNecessary(final String input, final LexemeSequenceImpl result, final ConversionHints hints) {
        if (hints != null && hints.containsKey(ConversionHints.KEY_MESSAGE_TYPE)) {
            LexemeImpl artificialStartToken = null;
            if (hints.get(ConversionHints.KEY_MESSAGE_TYPE) == ConversionHints.VALUE_MESSAGE_TYPE_METAR && !input.startsWith("METAR ")) {
                artificialStartToken = new LexemeImpl("METAR", Lexeme.Identity.METAR_START);
            } else if (hints.get(ConversionHints.KEY_MESSAGE_TYPE) == ConversionHints.VALUE_MESSAGE_TYPE_TAF && !input.startsWith("TAF ")) {
                artificialStartToken = new LexemeImpl("TAF", Lexeme.Identity.TAF_START);
            }
            if (artificialStartToken != null) {
                artificialStartToken.setSynthetic(true);
                result.addAsFirst(new LexemeImpl(" ", Lexeme.Identity.WHITE_SPACE));
                result.addAsFirst(artificialStartToken);
            }
        }
    }

}
