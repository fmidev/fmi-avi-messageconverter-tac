package fi.fmi.avi.converter.tac.metar;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class SPECITACParser extends METARTACParserBase<SPECI> {

    @Override
    protected Lexeme.Identity getExpectedFirstTokenIdentity() {
        return Lexeme.Identity.SPECI_START;
    }

    @Override
    protected SPECI buildUsing(METARImpl.Builder builder) {
        return builder.buildAsSPECI();
    }
}
