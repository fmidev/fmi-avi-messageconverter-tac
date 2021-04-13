package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.immutable.AdvisoryNumberImpl;

public class AdvisoryNumber extends RegexMatchingLexemeVisitor {
    public AdvisoryNumber(final OccurrenceFrequency prio) {
        super("^(?<advisoryNumber>[\\d]{4}/[0-9]+?)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && token.getPrevious().getIdentity().equals(LexemeIdentity.ADVISORY_NUMBER_LABEL)) {
                token.identify(LexemeIdentity.ADVISORY_NUMBER);

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
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisory) msg).getAdvisoryNumber();

                if (advisoryNumber.getSerialNumber() == 0) {
                    throw new SerializingException("The advisory number is missing the serial number");
                }

                if (advisoryNumber.getYear() == 0) {
                    throw new SerializingException(("The advisory number is missing the year"));
                }

                retval = Optional.of(this.createLexeme(advisoryNumber.asAdvisoryNumber(), LexemeIdentity.ADVISORY_NUMBER));

            }
            return retval;
        }
    }
}
