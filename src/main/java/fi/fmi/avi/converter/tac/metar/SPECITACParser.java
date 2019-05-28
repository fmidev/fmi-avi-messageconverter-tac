package fi.fmi.avi.converter.tac.metar;

import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.SPECIImpl;

public class SPECITACParser extends METARAndSPECITACParserBase<SPECI, SPECIImpl.Builder> {

    @Override
    protected LexemeIdentity getExpectedFirstTokenIdentity() {
        return LexemeIdentity.SPECI_START;
    }

    @Override
    protected SPECI buildUsing(final SPECIImpl.Builder builder) {
        return builder.build();
    }

    @Override
    protected SPECIImpl.Builder getBuilder() {
        return SPECIImpl.builder();
    }
}
