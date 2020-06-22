package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

public class SpaceWeatherPresetLocation extends RegexMatchingLexemeVisitor {

    public SpaceWeatherPresetLocation(final Priority prio) {
        super("^(?<type>EQN|EQS|HSH|HNH|MSH|MNH|DAYLIGHT_SIDE)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);

        token.setParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.fromCode(match.group("type")));
    }
}
