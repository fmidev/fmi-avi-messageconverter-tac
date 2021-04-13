package fi.fmi.avi.converter.tac.lexer;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COUNTRY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COVER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MEAN_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MIN_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MIN_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MONTH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RUNWAY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.SEQUENCE_NUMBER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TENDENCY_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.YEAR;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;

/**
 * Lexeme is a basic lexical unit of an aviation weather message.
 * A Lexeme is an abstraction of a single semantic character string
 * token of a message encoded using Traditional Alphanumeric Codes (TAC).
 *
 * In AviMessageConverter library a TAC message is first parsed into
 * {@link LexemeSequence} containing a list of identified
 * (and possibly unidentified) {@link Lexeme}s by
 * {@link AviMessageLexer}. This result is then typically fed into
 * {@link AviMessageConverter} to construct a Java POJO for the entire
 * message.
 *
 * In addition to the identification of the token, a Lexeme may contain
 * the start and end index if the token in the original input String,
 * token the lexing status and possible error/warning message
 * provided by the Lexer. Lexer also provides navigation links
 * forward and backward in the LexemeSequence, and acts a the abstract
 * target for the {@link LexemeVisitor} according to the Visitor design
 * pattern for iteratively identifying a set of Lexemes by a matching them
 * against different possible token patterns until they can be either
 * positively identified or all possible options have been tried out.
 *
 * To support further use of the results of the identifying work necessary for
 * identifying a Lexeme, {@link AviMessageLexer} may store
 * values extracted from the parsed string token into the Lexeme
 * by using the {@link #setParsedValue(ParsedValueName, Object)} method.
 * These values may be queried by the {@link AviMessageConverter} for
 * constructing the {@link fi.fmi.avi.model.AviationWeatherMessage} POJOs.
 *
 * {@link LexemeSequence} may also be directly used for providing validating
 * user feedback for a TAC message under construction. Any non-empty
 * String will always pass the {@link AviMessageLexer#lexMessage(String)} method
 * returning a {@link LexemeSequence} with either recognized or unrecognized
 * Lexemes.
 *
 * @author Ilkka Rinne / Spatineo 2017
 */
public interface Lexeme {

    /**
     * Returns the identity of the Lexeme if the Lexeme has been identified.
     *
     * @return Lexeme identity, or null if unrecognized.
     */
    LexemeIdentity getIdentity();

    /**
     * Returns the identity of the Lexeme if the Lexeme has been identified, and
     * the status is either {@link Status#OK} or {@link Status#WARNING}.
     *
     * @return Lexeme identity, or null if status is {@link Status#UNRECOGNIZED} or {@link Status#SYNTAX_ERROR}.
     */
    LexemeIdentity getIdentityIfAcceptable();

    /**
     * Returns the recognizing status of Lexeme.
     *
     * @return the status
     */
    Status getStatus();

    /**
     * Returns the lexing-related message for this Lexeme, if provided by the {@link AviMessageLexer}.
     * This message is typically provided when warning or error status is set.
     *
     * @return the lexing message
     */
    String getLexerMessage();

    /**
     * Sets a lexing note, such as an explanation for warning or error status.
     *
     * @param msg
     *         message
     */
    void setLexerMessage(final String msg);

    /**
     * The start index of the token used for creating this Lexeme in the original message input.
     * This is only available if populated by the lexer.
     *
     * @return index of the first character of the Lexeme token
     */
    int getStartIndex();

    /**
     * The end index of the token used for creating this Lexeme in the original message input.
     * This is only available if populated by the lexer.
     *
     * @return index of the last character of the Lexeme token
     */
    int getEndIndex();

    /**
     * Provides all additional information entries parsed from the original token by the
     * {@link AviMessageLexer}.
     *
     * @return map of entries
     *
     * @see #getParsedValue(ParsedValueName, Class)
     */
    Map<ParsedValueName, Object> getParsedValues();

    /**
     * Returns a particular additional information entry parsed from the original token by the
     * {@link AviMessageLexer} as given type (if possible).
     *
     * The implementations must throw a
     * {@link ClassCastException} if the provided value exists, but cannot be returned as
     * the type given by {@code clz}. The implementations must throw an
     * {@link IllegalArgumentException} if the requested entity is not allowed to be used
     * with the {@link Identity} of this Lexeme.
     *
     * @param name
     *         name of the value
     * @param clz
     *         class of the expected type
     * @param <T>
     *         returned type
     *
     * @return a previously stores value or null if not value is available
     */
    <T> T getParsedValue(ParsedValueName name, Class<T> clz) throws ClassCastException, IllegalArgumentException;

    /**
     * Returns this Lexeme as TAC encoded token.
     *
     * @return the TAC token
     */
    String getTACToken();

    /**
     * Returns the first Lexeme in the {@link LexemeSequence} containing this Lexeme ignoring
     * any possible {@link LexemeIdentity#WHITE_SPACE} and ignored Lexemes.
     * This link is provided mainly as navigation shortcut.
     *
     * @return the first Lexeme of the sequence, if available
     */
    Lexeme getFirst();

    /**
     * Returns the first Lexeme in the {@link LexemeSequence} containing this Lexeme.
     * This link is provided mainly as navigation shortcut.
     *
     * @param acceptIgnored
     *         true if ignored and whitespace Lexemes are to be returned
     *
     * @return the first Lexeme of the sequence, if available
     */
    Lexeme getFirst(boolean acceptIgnored);

    /**
     * Returns the Lexeme immediately before this one in the {@link LexemeSequence}
     * containing this Lexeme ignoring any possible {@link LexemeIdentity#WHITE_SPACE} and ignored Lexemes.
     * For the first Lexeme in sequence this must return
     * {@code null}.
     *
     * @return the previous Lexeme of the sequence, if available
     */
    Lexeme getPrevious();

    /**
     * Returns the Lexeme immediately before this one in the {@link LexemeSequence}
     * containing this Lexeme. For the first Lexeme in sequence this must return
     * {@code null}.
     *
     * @param acceptIgnored
     *         true if ignored and whitespace Lexemes are to be returned
     *
     * @return the previous Lexeme of the sequence, if available
     */
    Lexeme getPrevious(boolean acceptIgnored);

    /**
     * Returns the Lexeme immediately after this one in the {@link LexemeSequence}
     * containing this Lexeme ignoring any possible {@link LexemeIdentity#WHITE_SPACE} and ignored Lexemes.
     * For the last Lexeme in sequence this must return
     * {@code null}.
     *
     * @return the next Lexeme of the sequence, if available
     */
    Lexeme getNext();

    /**
     * Returns the Lexeme immediately after this one in the {@link LexemeSequence}
     * containing this Lexeme. For the last Lexeme in sequence this must return
     * {@code null}.
     *
     * @param acceptIgnored
     *         true if ignored and whitespace Lexemes are to be returned
     *
     * @return the next Lexeme of the sequence, if available
     */
    Lexeme getNext(boolean acceptIgnored);

    /**
     * For checking if the Lexeme knows the previous Lexeme in it's sequence.
     *
     * @return the previous Lexeme
     */
    boolean hasPrevious();

    /**
     * For checking if the Lexeme knows the previous Lexeme in it's sequence.
     *
     * @param acceptIgnored
     *         true if ignored and whitespace Lexemes are to be considered
     *
     * @return the previous Lexeme
     */
    boolean hasPrevious(boolean acceptIgnored);

    /**
     * For checking if the Lexeme knows the next Lexeme in it's sequence.
     *
     * @return the next Lexeme
     */
    boolean hasNext();

    /**
     * For checking if the Lexeme knows the next Lexeme in it's sequence.
     *
     * @param acceptIgnored
     *         true if also ignored and whitespace Lexemes are to be considered
     *
     * @return the next Lexeme
     */
    boolean hasNext(boolean acceptIgnored);

    /**
     * Returns a {@link LexemeSequence} starting from this Lexeme using the {@link #getNext()} iteratively
     * until the {@link #hasNext()} return false.
     *
     * @return the tail lexeme sequence
     *
     * @throws IllegalStateException
     *         if a loop is detected in the iteration
     */
    LexemeSequence getTailSequence() throws IllegalStateException;

    /**
     * A synthetic Lexeme has been created by the lexing process to fix some small syntax
     * issues of the input message, such a missing start token.
     *
     * @return true of the Lexemehas been marked as synthetic
     */

    boolean isSynthetic();

    /**
     * The certainty of the lexeme recognition as {@link #getIdentity()}.
     * if {@link #getStatus()} is {@link Status#UNRECOGNIZED}, value == 0.0.
     * Otherwise the value is ]0.0, 1.0] based on the Lexer's reasoning.
     *
     * Lexer implementations are allowed to override a previous Lexeme
     * recognition only if the value is &lt; 1.0.
     *
     * @return value between 0.0 and 1.0
     */
    double getIdentificationCertainty();

    /**
     * Sets the confidence of the Lexeme identification.
     *
     * @param percentage
     *         between 0.0 and 1.0
     *
     * @see #getIdentificationCertainty()
     */
    void setIdentificationCertainty(double percentage);

    /**
     * This token has been marked as ignored, and it will not be exposed when iterating
     * over the lexeme sequences.
     *
     * @return true is token has been marked as ignored, false otherwise
     */
    boolean isIgnored();

    /**
     * Setting to set this Lexeme as ignored or not.
     *
     * @param ignored
     *         true if to be ignored
     */
    void setIgnored(boolean ignored);

    /**
     * Identifies this Lexeme as {@code id} with {@link Status#OK} and no additional message.
     * The certainty of recognition is 1.0 (100% certain).
     *
     * @param id
     *         identity to assign
     */
    void identify(final LexemeIdentity id);

    /**
     * Identifies this Lexeme as {@code id} with {@code status} and no additional message.
     * The certainty of recognition is 1.0 (100% certain).
     *
     * @param id
     *         identity to assign
     * @param status
     *         to set
     */
    void identify(final LexemeIdentity id, final Status status);

    /**
     * Identifies this Lexeme as {@code id} with {@code status} and {@code message}.
     * The certainty of recognition is 1.0 (100% certain).
     *
     * @param id
     *         identity to assign
     * @param status
     *         to set
     * @param note
     *         additional message, such as lexing warning note
     */
    void identify(final LexemeIdentity id, final Status status, final String note);

    /**
     * Identifies this Lexeme as {@code id} with {@link Status#OK} and no additional message.
     *
     * @param id
     *         identity to assign
     * @param certainty
     *         the level of confidence 0.0 - 1.0
     */
    void identify(final LexemeIdentity id, double certainty);

    /**
     * Identifies this Lexeme as {@code id} with {@code status} and no additional message.
     *
     * @param id
     *         identity to assign
     * @param status
     *         to set
     * @param certainty
     *         the level of confidence 0.0 - 1.0
     */
    void identify(final LexemeIdentity id, final Status status, double certainty);

    /**
     * Identifies this Lexeme as {@code id} with {@code status} and {@code message}.
     *
     * @param id
     *         identity to assign
     * @param status
     *         to set
     * @param note
     *         additional message, such as lexing warning note
     * @param certainty
     *         the level of confidence 0.0 - 1.0
     */
    void identify(final LexemeIdentity id, final Status status, final String note, double certainty);

    /**
     * Return true when the Lexeme is not in {@link Status#UNRECOGNIZED}
     *
     * @return true is recognized
     */
    boolean isRecognized();

    /**
     * Stores an additional information entity to used later in the parsing process.
     * The main purpose is the simplify further parsing be storing the information
     * bits parsed from the token to make lexing possible. This way this information
     * does not have to be parsed more than once.
     *
     * One of the {@link #identify} methods must be called before this method to provide
     * allowed {@code name} checking.
     *
     * @param name
     *         name of the stored value
     * @param value
     *         value to store
     *
     * @throws IllegalArgumentException
     *         if the {@code name} is not allowed to be used with the current Lexeme identity
     * @throws IllegalStateException
     *         if the Lexeme has not yet been identified.
     */
    void setParsedValue(ParsedValueName name, Object value) throws IllegalArgumentException, IllegalStateException;

    /**
     * Provides access to a {@link LexemeVisitor} to refine this Lexeme.
     * Typically called by a {@link LexemeVisitor} to try to recognize
     * the Lexeme.
     *
     * For hierarchical Lexemes, the implementation is responsible for
     * delegating the call to the child nodes.
     *
     * @param visitor
     *         to visit this Lexeme
     * @param hints
     *         hints to pass the lexing process
     *
     * @see LexemeVisitor#visit(Lexeme, ConversionHints)
     */
    void accept(LexemeVisitor visitor, ConversionHints hints);

    /**
     * Finds the next {@link Lexeme} identified as <code>needle</code> in the sequence of Lexemes starting from this Lexeme.
     *
     * @param needle
     *         the identity of the Lexeme to find
     *
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     */
    Lexeme findNext(LexemeIdentity needle);

    /**
     * Finds the next {@link Lexeme} identified as <code>needle</code> in the sequence of Lexemes starting
     * from this Lexeme.
     *
     * If the <code>found</code> is not null, it's {@link Consumer#accept(Object)} is called with the
     * possible match. If not match is found, this method is not called.
     *
     * As {@link Consumer} is a functional interface, it can be implemented as a lambda expression:
     * <pre>
     *     findNext(CORRECTION, lexed.getFirstLexeme(), stopAt, (match) -&gt; taf.setStatus(AviationCodeListUser.TAFStatus.CORRECTION));
     * </pre>
     * or, if the expression is not easily inlined:
     * <pre>
     *     lexed.getFirstLexeme().findNext(AMENDMENT, stopAt, (match) -&gt; {
     *       TAF.TAFStatus status = taf.getStatus();
     *         if (status != null) {
     *           retval.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
     *             "TAF cannot be both " + TAF.TAFStatus.AMENDMENT + " and " + status + " at the same time"));
     *         } else {
     *           taf.setStatus(AviationCodeListUser.TAFStatus.AMENDMENT);
     *         }
     *     });
     * </pre>
     *
     * @param needle
     *         the identity of the Lexeme to find
     * @param found
     *         the function to execute with the match
     *
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     */
    Lexeme findNext(LexemeIdentity needle, Consumer<Lexeme> found);

    /**
     * Finds the next {@link Lexeme} identified as <code>needle</code> in the sequence of Lexemes starting
     * from this Lexeme.
     *
     * If the <code>found</code> is not null, it's {@link Consumer#accept(Object)} is called with the
     * possible match. If not match is found and <code>notFound</code> is not null, the function <code>notFound</code> is
     * called instead of <code>found</code>.
     *
     * @param needle
     *         the identity of the Lexeme to find
     * @param found
     *         the function to execute with the match
     * @param notFound
     *         the function to execute if not match was found
     *
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     */
    Lexeme findNext(LexemeIdentity needle, Consumer<Lexeme> found, LexemeParsingNotifyer notFound);

    /**
     * Lexeme status based on lexing process.
     */
    enum Status {
        UNRECOGNIZED, OK, SYNTAX_ERROR, WARNING
    }

    /**
     * Lexeme identity corresponding to the different token
     * types used in aviation weather messages.
     *
     * @deprecated please use {@link LexemeIdentity} instead
     */
    enum Identity {
        METAR_START,
        SPECI_START,
        TAF_START,
        LOW_WIND_START,
        WX_WARNING_START,
        ARS_START,
        WXREP_START,
        AIREP_START,
        SIGMET_START,
        US_SIGMET_START,
        REP,
        SPACE_WEATHER_ADVISORY_START,
        ADVISORY_PHENOMENA_LABEL(TYPE),
        ADVISORY_PHENOMENA_TIME_GROUP(DAY1, HOUR1, MINUTE1),
        VOLCANIC_ASH_ADVISORY_START,
        CORRECTION,
        AMENDMENT,
        CANCELLATION,
        NIL,
        ROUTINE_DELAYED_OBSERVATION,
        ISSUE_TIME(YEAR, MONTH, DAY1, HOUR1, MINUTE1),
        AERODROME_DESIGNATOR(VALUE, COUNTRY),
        CAVOK,
        AIR_DEWPOINT_TEMPERATURE(VALUE, UNIT),
        AIR_PRESSURE_QNH(VALUE, UNIT),
        SURFACE_WIND(DIRECTION, MAX_VALUE, MEAN_VALUE, UNIT, RELATIONAL_OPERATOR, RELATIONAL_OPERATOR2),
        VARIABLE_WIND_DIRECTION(MIN_DIRECTION, MAX_DIRECTION, UNIT),
        HORIZONTAL_VISIBILITY(RELATIONAL_OPERATOR, VALUE, UNIT, DIRECTION),
        CLOUD(VALUE, COVER, TYPE, UNIT),
        TAF_FORECAST_CHANGE_INDICATOR(DAY1, HOUR1, MINUTE1, TYPE),
        TAF_CHANGE_FORECAST_TIME_GROUP(DAY1, DAY2, HOUR1, HOUR2, MINUTE1),
        TREND_CHANGE_INDICATOR(TYPE),
        NO_SIGNIFICANT_CHANGES,
        TREND_TIME_GROUP(TYPE, HOUR1, MINUTE1),
        NO_SIGNIFICANT_WEATHER,
        AUTOMATED,
        RUNWAY_VISUAL_RANGE(RUNWAY, MIN_VALUE, MAX_VALUE, RELATIONAL_OPERATOR, RELATIONAL_OPERATOR2, TENDENCY_OPERATOR, UNIT),
        WEATHER(VALUE),
        RECENT_WEATHER(VALUE),
        WIND_SHEAR(RUNWAY),
        SEA_STATE(UNIT, UNIT2, VALUE),
        RUNWAY_STATE(RUNWAY, VALUE),
        SNOW_CLOSURE,
        VALID_TIME(DAY1, DAY2, HOUR1, HOUR2, MINUTE1, MINUTE2),
        MIN_TEMPERATURE(DAY1, HOUR1, VALUE),
        MAX_TEMPERATURE(DAY1, HOUR1, VALUE),
        REMARKS_START,
        REMARK(VALUE),
        COLOR_CODE(VALUE),
        WHITE_SPACE(TYPE, VALUE),
        END_TOKEN,
        BULLETIN_HEADING_DATA_DESIGNATORS(VALUE),
        BULLETIN_HEADING_LOCATION_INDICATOR(VALUE),
        BULLETIN_HEADING_BBB_INDICATOR(VALUE, SEQUENCE_NUMBER);

        private final Set<ParsedValueName> possibleParameters;

        Identity(final ParsedValueName... names) {
            possibleParameters = names.length == 0 ? Collections.emptySet() : Collections.unmodifiableSet(EnumSet.of(names[0], names));
        }

        public Set<ParsedValueName> getPossibleNames() {
            return this.possibleParameters;
        }

        public boolean canStore(final ParsedValueName name) {
            return this.possibleParameters.contains(name);
        }

    }

    /**
     * Possible names for querying values of the stored
     * token parameters created during the lexing process. The names used
     * for each of the Lexemes
     */
    enum ParsedValueName {
        COUNTRY,
        YEAR,
        MONTH,
        DAY1,
        DAY2,
        HOUR1,
        HOUR2,
        MINUTE1,
        MINUTE2,
        TYPE,
        COVER,
        VALUE,
        VALUE2,
        UNIT,
        UNIT2,
        MAX_VALUE,
        MIN_VALUE,
        MEAN_VALUE,
        DIRECTION,
        MIN_DIRECTION,
        MAX_DIRECTION,
        RELATIONAL_OPERATOR,
        RELATIONAL_OPERATOR2,
        TENDENCY_OPERATOR,
        RUNWAY,
        SEQUENCE_NUMBER
    }

    enum MeteorologicalBulletinSpecialCharacter {
        START_OF_HEADING('\u0001'),
        START_OF_TEXT('\u0002'),
        END_OF_TEXT('\u0003'),
        END_OF_TRANSMISSION('\u0004'),
        ACKNOWLEDGE('\u0006'),
        HORIZONTAL_TAB('\u0009'),
        LINE_FEED('\n'),
        VERTICAL_TAB('\u000B'),
        FORM_FEED('\u000C'),
        CARRIAGE_RETURN('\r'),
        DATA_LINK_ESCAPE('\u0010'),
        DEVICE_LINK_CONTROL_1('\u0011'),
        DEVICE_LINK_CONTROL_2('\u0012'),
        NEGATIVE_ACKNOWLEDGE('\u0015'),
        SYNCHRONOUS_IDLE('\u0016'),
        END_OF_TRANSMISSION_BLOCK('\u0017'),
        ESCAPE('\u001B'),
        FILE_SEPARATOR('\u001C'),
        GROUP_SEPARATOR('\u001D'),
        RECORD_SEPARATOR('\u001E'),
        DELETE('\u007F'),
        SPACE('\u0020');

        private final char content;

        MeteorologicalBulletinSpecialCharacter(final char content) {
            this.content = content;
        }

        public static MeteorologicalBulletinSpecialCharacter fromChar(final char c) {
            for (final MeteorologicalBulletinSpecialCharacter m : MeteorologicalBulletinSpecialCharacter.values()) {
                if (m.getContent().equals(Character.toString(c))) {
                    return m;
                }
            }
            return null;
        }

        public String getContent() {
            return String.valueOf(this.content);
        }
    }

    /**
     * Lambda function interface to use with
     * {@link #findNext(LexemeIdentity, Consumer, LexemeParsingNotifyer)}
     */
    @FunctionalInterface
    interface LexemeParsingNotifyer {
        void ping();
    }

}
