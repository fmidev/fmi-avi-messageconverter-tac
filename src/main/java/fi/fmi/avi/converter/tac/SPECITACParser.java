package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.metar.immutable.SPECIImpl;

/**
 * Created by rinne on 06/03/2018.
 */
public class SPECITACParser extends METARTACParserBase<SPECI> {

    @Override
    protected SPECI getMessageInstance() {
        return new SPECIImpl();
    }

    @Override
    protected Lexeme.Identity getExpectedFirstTokenIdentity() {
        return Lexeme.Identity.SPECI_START;
    }
}
