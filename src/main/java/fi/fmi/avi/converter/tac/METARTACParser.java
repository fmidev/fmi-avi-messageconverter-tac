package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.impl.METARImpl;

/**
 * Created by rinne on 06/03/2018.
 */
public class METARTACParser extends METARTACParserBase<METAR> {

    @Override
    protected METAR getMessageInstance() {
        return new METARImpl();
    }

    @Override
    protected Lexeme.Identity getExpectedFirstTokenIdentity() {
        return Lexeme.Identity.METAR_START;
    }
}
