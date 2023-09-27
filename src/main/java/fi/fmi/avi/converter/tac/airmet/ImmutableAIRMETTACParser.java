package fi.fmi.avi.converter.tac.airmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;

/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 *
 */
public class ImmutableAIRMETTACParser extends AIRMETTACParserBase<AIRMETImpl> {

    @Override
    public ConversionResult<AIRMETImpl> convertMessage(final String input, final ConversionHints hints) {
        return convertMessageInternal(input, hints);
    }
}
