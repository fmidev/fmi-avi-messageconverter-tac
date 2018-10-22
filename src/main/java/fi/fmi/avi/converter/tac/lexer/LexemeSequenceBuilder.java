package fi.fmi.avi.converter.tac.lexer;

import java.util.List;
import java.util.Optional;

/**
 * Used for constructing {@link LexemeSequence}s one or more {@link Lexeme} at a time.
 *
 * An instance of this class can be created using {@link LexingFactory#createLexemeSequenceBuilder()}.
 *
 * @author Ilkka Rinne / Spatineo 2017
 */
public interface LexemeSequenceBuilder {

    /**
     * Adds one {@link Lexeme} as the last one in the constructed sequence.
     *
     * @param lexeme
     *         to add
     *
     * @return the builder
     */
    LexemeSequenceBuilder append(Lexeme lexeme);

    /**
     * Adds all the {@link Lexeme} contained in <code>lexemes</code> to the end
     * of the constructed sequence in the given order.
     *
     * @param lexemes
     *         to add
     *
     * @return the builder
     */
    LexemeSequenceBuilder appendAll(List<Lexeme> lexemes);

    /**
     * Removes the last Lexeme appended if one exists.
     *
     * @return the builder
     */
    LexemeSequenceBuilder removeLast();

    /**
     * Returns the last lexeme in the sequence to build if one exists.
     *
     * @return the last lexeme in the sequence to build if one exists
     */
    Optional<Lexeme> getLast();

    /**
     * Returns the complete {@link LexemeSequence}.
     *
     * @return the sequence
     */
    LexemeSequence build();
}
