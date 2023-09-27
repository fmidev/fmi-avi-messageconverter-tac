package fi.fmi.avi.converter.tac.sigmet;


import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.sigmet.SIGMET;

/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class SIGMETTACParser extends SIGMETTACParserBase<SIGMET> {

    @Override
    public ConversionResult<SIGMET> convertMessage(final String input, final ConversionHints hints) {
        return new ConversionResult<>(convertMessageInternal(input, hints));
    }

}
