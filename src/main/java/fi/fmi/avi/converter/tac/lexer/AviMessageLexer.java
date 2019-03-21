package fi.fmi.avi.converter.tac.lexer;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationCodeListUser;

/**
 *
 * AviMessageLexer creates a {@link LexemeSequence}s out of TAC encoded Strings.
 *
 * Lexing of the TAC message only tries to identify each of the individual tokens
 * (or {@link Lexeme}s) of the input message and store their parsed parameters for
 * later use.
 *
 * Typical uses the AviMessageLexer implementations include the lexing phase
 * of the TAC message parsing before creating the actual Java POJO, and basic
 * input validation for TAC messages.
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public interface AviMessageLexer {

    /**
     * Lexes the input String with the default parsing settings.
     *
     * @param input
     *         the TAC encoded message
     *
     * @return sequence of recongized or unrecognized {@link Lexeme}s
     *
     * @see LexemeSequence
     * s
     */
    LexemeSequence lexMessage(String input);

    /**
     * Lexes the input String with the given parsing settings.
     *
     * @see LexemeSequence
     * @see ConversionHints
     *
     * @param input the TAC encoded message
     * @param hints parsing hints to be passed to the lexer implementation
     * @return sequence of recognized or unrecognized {@link Lexeme}s
     */
    LexemeSequence lexMessage(String input, ConversionHints hints);

    /**
     * Tries to recognize the given String as one of the aviation message types in
     * {@link AviationCodeListUser.MessageType}. Must use the same
     * logic as {@link #lexMessage(String, ConversionHints)} does internally.
     *
     * @param input the TAC encoded message
     * @param hints parsing hints to be passed to the lexer implementation
     * @return the type if recognized
     */
    Optional<AviationCodeListUser.MessageType> recognizeMessageType(String input, ConversionHints hints);

}
