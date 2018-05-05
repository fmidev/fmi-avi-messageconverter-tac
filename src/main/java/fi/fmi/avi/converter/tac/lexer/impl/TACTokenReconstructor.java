package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.List;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
/**
 *
 * Created by rinne on 15/02/17.
 */
public interface TACTokenReconstructor {

    void setLexingFactory(LexingFactory factory);

    /**
     * Returns one or more Lexemes produced by this TACTokenReconstructor using the data from the given message.
     * When more than one alternative Lexemes can be generated based on the data of the given {@code msg},
     * the parameters given in {@code ctx} are used to specify which Lexeme is intended.
     *
     * Typically only the one specified Lexeme is returned. More than one can only be returned if they should
     * immediately follow each other in the TAC message, and are always tightly coupled semantically, such as
     * "PROB30 TEMPO" or "TXM02/3015 TNM10/3103"
     *
     * @param msg the source message
     * @param ctx reconstruction context to guide the reconstructor
     * @param <T> the type of the source message
     *
     * @return the list of reconstructed Lexemes
     *
     * @throws SerializingException when the Lexeme cannot be reconstructed from the contents of the <code>msg</code>
     */
    <T extends AviationWeatherMessage> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ReconstructorContext<T> ctx) throws SerializingException;
}
