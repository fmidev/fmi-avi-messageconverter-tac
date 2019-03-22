package fi.fmi.avi.converter.tac.metar;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.model.metar.immutable.METARImpl;

public class ImmutableMETARTACParser extends METARAndSPECITACParserBase<METARImpl, METARImpl.Builder> {

    @Override
    protected Lexeme.Identity getExpectedFirstTokenIdentity() {
        return Lexeme.Identity.METAR_START;
    }

    @Override
    protected METARImpl buildUsing(final METARImpl.Builder builder) {
        return builder.build();
    }

    @Override
    protected METARImpl.Builder getBuilder() {
        return METARImpl.builder();
    }
}
