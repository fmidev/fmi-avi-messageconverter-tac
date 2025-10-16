package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.AdvisoryNumber;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.immutable.AdvisoryNumberImpl;

import java.util.Optional;
import java.util.regex.Matcher;

public class ReplaceAdvisoryNumber extends RegexMatchingLexemeVisitor {
    public ReplaceAdvisoryNumber(final OccurrenceFrequency prio) {
        super("^(?<advisoryNumber>[\\d]{4}/[0-9]+?)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && token.getPrevious().getIdentity().equals(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL)) {
                token.identify(LexemeIdentity.REPLACE_ADVISORY_NUMBER);

                final AdvisoryNumberImpl advisoryNumber = AdvisoryNumberImpl.Builder.from(match.group("advisoryNumber")).build();
                token.setParsedValue(Lexeme.ParsedValueName.VALUE, advisoryNumber);
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                if (((SpaceWeatherAdvisoryAmd79) msg).getReplaceAdvisoryNumber().isPresent()) {
                    final AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisoryAmd79) msg).getReplaceAdvisoryNumber().get();

                    if (advisoryNumber.getSerialNumber() == 0) {
                        throw new SerializingException("The advisory number is missing the serial number");
                    }

                    if (advisoryNumber.getYear() == 0) {
                        throw new SerializingException(("The advisory number is missing the year"));
                    }

                    retval = Optional.of(this.createLexeme(advisoryNumber.asAdvisoryNumber(), LexemeIdentity.REPLACE_ADVISORY_NUMBER));
                }
            }
            return retval;
        }

    }
}
