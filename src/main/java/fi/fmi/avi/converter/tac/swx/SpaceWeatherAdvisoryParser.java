package fi.fmi.avi.converter.tac.swx;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class SpaceWeatherAdvisoryParser extends AbstractTACParser<SpaceWeatherAdvisory> {

    private AviMessageLexer lexer;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public ConversionResult<SpaceWeatherAdvisory> convertMessage(final String input, final ConversionHints hints) {
        final ConversionResult<SpaceWeatherAdvisory> retval = new ConversionResult<>();

        final LexemeSequence lexed = this.lexer.lexMessage(input);
        //TODO: parser implementation

        retval.setStatus(ConversionResult.Status.FAIL);
        return retval;
    }
}
