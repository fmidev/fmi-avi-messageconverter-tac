package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;


/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class ImmutableTAFTACParser extends TAFTACParserBase<TAFImpl> {

    @Override
    public ConversionResult<TAFImpl> convertMessage(final String input, final ConversionHints hints) {
        return convertMessageInternal(input, hints);
    }

}
