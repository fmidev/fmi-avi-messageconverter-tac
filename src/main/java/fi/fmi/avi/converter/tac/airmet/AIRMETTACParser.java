package fi.fmi.avi.converter.tac.airmet;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.sigmet.AIRMET;

/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class AIRMETTACParser extends AIRMETTACParserBase<AIRMET> {

    @Override
    public ConversionResult<AIRMET> convertMessage(final String input, final ConversionHints hints) {
        return new ConversionResult<>(convertMessageInternal(input, hints));
    }

}
