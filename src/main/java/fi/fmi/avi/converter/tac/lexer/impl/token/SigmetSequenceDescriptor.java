package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

public class SigmetSequenceDescriptor extends TimeHandlingRegex {

    public SigmetSequenceDescriptor(final OccurrenceFrequency prio) {
        super("^([A-Z]{0,1})([0-9]{0,1})([0-9]{1})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (LexemeIdentity.REAL_SIGMET_START.equals(token.getPrevious().getIdentity())) {
            final String id = match.group(0);

            token.identify(LexemeIdentity.SEQUENCE_DESCRIPTOR);
            token.setParsedValue(VALUE, id);
        }
    }
    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET)msg;
                return Optional.of(createLexeme(sigmet.getSequenceNumber(), LexemeIdentity.SEQUENCE_DESCRIPTOR));
            }
            return Optional.empty();
        }
    }

}
