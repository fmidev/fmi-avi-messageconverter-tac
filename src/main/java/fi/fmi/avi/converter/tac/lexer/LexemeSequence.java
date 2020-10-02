package fi.fmi.avi.converter.tac.lexer;

import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

/**
 * A sequence of {@link Lexeme}s corresponding to a message or part of it.
 * Typically produced as the result of the {@link AviMessageLexer#lexMessage(String)}
 * or {@link AviMessageTACTokenizer#tokenizeMessage(AviationWeatherMessageOrCollection)}.
 *
 * To create a new instance from a String,
 * use {@link LexingFactory#createLexemeSequence(String, ConversionHints)}
 * or build a sequence dynamically using {@link LexemeSequenceBuilder}
 * available from {@link LexingFactory#createLexemeSequenceBuilder()}.
 *
 * @author Ilkka Rinne / Spatineo 2017
 */
public interface LexemeSequence {

    /**
     * Returns a TAC encoded version of the whole sequence.
     * Implementations should return the exactly same TAC message
     * used for constructing the LexemeSequence is possible.
     *
     * @return the TAC representation
     */
    String getTAC();

    /**
     * Convenience method for accessing the first {@link Lexeme} in the sequence.
     *
     * @return the first {@link Lexeme}
     */
    Lexeme getFirstLexeme();

    /**
     * Convenience method for accessing the last {@link Lexeme} in the sequence.
     *
     * @return the first {@link Lexeme}
     */
    Lexeme getLastLexeme();

    /**
     * List of all {@link Lexeme}s in the sequence from the first to the last.
     * Ignored lexemes are not returned.
     *
     * Note that Java 8 users may filter the returned list conveniently using
     * the Stream API:
     * <pre>
     *     List&lt;Lexeme&gt; recognizedLexemes = lexed.getLexemes().stream()
     *       .filter((lexeme) -&gt; Lexeme.Status.UNRECOGNIZED != lexeme.getStatus())
     *       .collect(Collectors.toList());
     * </pre>
     *
     * @return contained Lexemes as a list
     */
    List<Lexeme> getLexemes();

    /**
     * List of all {@link Lexeme}s in the sequence from the first to the last.
     *
     * Note that Java 8 users may filter the returned list conveniently using
     * the Stream API:
     * <pre>
     *     List&lt;Lexeme&gt; recognizedLexemes = lexed.getLexemes().stream()
     *       .filter((lexeme) -&gt; Lexeme.Status.UNRECOGNIZED != lexeme.getStatus())
     *       .collect(Collectors.toList());
     * </pre>
     *
     * @param acceptIgnored
     *         true to also return any lexemes set as ignored
     *
     * @return contained Lexemes as a list
     */
    List<Lexeme> getLexemes(boolean acceptIgnored);

    /**
     * Returns a list of sub-sequences cut from the sequence split by given {@link LexemeIdentity} set.
     * A new sub-sequence starts at each found {@link Lexeme} identified as any of the given
     * <code>ids</code>. Zero-length sub-sequences are discarded silently so if the first
     * {@link Lexeme} matches, the first returned {@link LexemeSequence} starts at the
     * first {@link Lexeme}. If the last {@link Lexeme} matches, the last returned
     * {@link LexemeSequence} contains only the last Lexeme.
     *
     * If not matches are found, the original {@link LexemeSequence} is returned as the
     * only list item.
     *
     * @param ids
     *         the IDs if the tokens to use for splitting
     *
     * @return the list of split-up sequences
     */
    List<LexemeSequence> splitBy(LexemeIdentity... ids);

    /**
     * Returns a list of sub-sequences cut from the sequence split by given {@link LexemeIdentity} set.
     * A new sub-sequence starts at or after each found {@link Lexeme} identified as any of the given
     * <code>ids</code>, depending on the value of <code>separatorStartsSequence</code>.
     * Zero-length sub-sequences are discarded silently.
     *
     * If not matches are found, the original {@link LexemeSequence} is returned as the
     * only list item.
     *
     * @param ids
     *         the IDs if the tokens to use for splitting
     *
     * @return the list of split-up sequences
     */
    List<LexemeSequence> splitBy(boolean separatorStartsSequence, LexemeIdentity... ids);

    /**
     * Trims any white space from the beginning and end of this sequence.
     *
     * @return the same sequence trimmed
     */
    LexemeSequence trimWhiteSpace();

}
