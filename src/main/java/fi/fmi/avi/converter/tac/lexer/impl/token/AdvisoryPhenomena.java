package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;

/**
 * Created by rinne on 10/02/17.
 */
public class AdvisoryPhenomena extends TimeHandlingRegex {

    public enum Type { OBS, FCST }

    public AdvisoryPhenomena(final Priority prio) {
        super("^(?<type>OBS|FCST)(?:[a-zA-Z0-9\\+\\s]+)?:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        token.setParsedValue(Lexeme.ParsedValueName.TYPE, Type.valueOf(match.group("type")));
    }

}
