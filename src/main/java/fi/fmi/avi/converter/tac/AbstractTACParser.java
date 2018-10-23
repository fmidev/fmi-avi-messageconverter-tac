package fi.fmi.avi.converter.tac;

import fi.fmi.avi.model.AviationWeatherMessage;

/**
 * Common parent class for AviMessageConverter implementations.
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public abstract class AbstractTACParser<T extends AviationWeatherMessage> implements TACParser<T> {


}
