package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationWeatherMessage;

/**
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public interface LexemeSequenceParser<T extends AviationWeatherMessage> extends AviMessageSpecificConverter<LexemeSequence, T> {

}
