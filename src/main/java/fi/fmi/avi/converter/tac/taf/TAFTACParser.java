package fi.fmi.avi.converter.tac.taf;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.taf.TAF;

/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class TAFTACParser extends TAFTACParserBase<TAF> {

    @Override
    public ConversionResult<TAF> convertMessage(final String input, final ConversionHints hints) {
        return new ConversionResult<>(convertMessageInternal(input, hints));
    }

}
