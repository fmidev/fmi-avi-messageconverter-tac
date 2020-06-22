package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.swx.immutable.AdvisoryNumberImpl;

public class AdvisoryNumber extends RegexMatchingLexemeVisitor {
    public AdvisoryNumber(final Priority prio) {
        super("^ADVISORY\\sNR:\\s(?<advisoryNumber>[\\d]{4}/[\\d]*)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_NUMBER);

        AdvisoryNumberImpl advisoryNumber = AdvisoryNumberImpl.builder().from(match.group("advisoryNumber")).build();
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, advisoryNumber);
    }
}
