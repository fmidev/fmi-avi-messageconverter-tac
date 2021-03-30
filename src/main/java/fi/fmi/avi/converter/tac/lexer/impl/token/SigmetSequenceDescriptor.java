package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;

import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

public class SigmetSequenceDescriptor extends TimeHandlingRegex {

    public SigmetSequenceDescriptor(final OccurrenceFrequency prio) {
        super("^([A-Z]{0,1})([0-9]{0,1})([0-9]{1})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        System.err.println(token+"seq prev:"+token.getPrevious().getIdentity()+" "+LexemeIdentity.SIGMET_START.equals(token.getPrevious().getIdentity()));
        if (LexemeIdentity.REAL_SIGMET_START.equals(token.getPrevious().getIdentity())) {
            System.err.println("found seq "+token);
            final String id = match.group(0);

            token.identify(LexemeIdentity.SEQUENCE_DESCRIPTOR);
            token.setParsedValue(VALUE, id);
        }
    }
}
