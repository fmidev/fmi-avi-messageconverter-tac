package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REAL_SIGMET_START;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LOCATION_INDICATOR;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetStart extends PrioritizedLexemeVisitor {

    public SigmetStart(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        String[] words=token.getTACToken().split(" ");
        if ((words.length==2)&&"SIGMET".equals(words[1])){
            token.identify(REAL_SIGMET_START);
            token.setParsedValue(LOCATION_INDICATOR, words[0]);
        }
    }
}
