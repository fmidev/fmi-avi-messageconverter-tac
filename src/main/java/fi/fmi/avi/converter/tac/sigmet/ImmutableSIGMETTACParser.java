package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 *
 */
public class ImmutableSIGMETTACParser extends SIGMETTACParserBase<SIGMETImpl> {

    @Override
    public ConversionResult<SIGMETImpl> convertMessage(final String input, final ConversionHints hints) {
        return convertMessageInternal(input, hints);
    }

    @Override
    public void setTACLexer(AviMessageLexer lexer) {

    }
}
