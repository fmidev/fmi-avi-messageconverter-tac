package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class METARTACParser extends METARTACParserBase<METAR>{

    @Override
    protected Lexeme.Identity getExpectedFirstTokenIdentity() {
        return Lexeme.Identity.METAR_START;
    }

    @Override
    protected METAR buildUsing(METARImpl.Builder builder) {
        return builder.build();
    }
}
