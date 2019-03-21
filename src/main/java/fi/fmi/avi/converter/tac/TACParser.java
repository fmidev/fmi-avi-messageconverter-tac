package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public interface TACParser<T extends AviationWeatherMessageOrCollection> extends AviMessageSpecificConverter<String, T> {

    void setTACLexer(AviMessageLexer lexer);

}
