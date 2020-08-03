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
        super("^ADVISORY\\sNR:\\s(?<advisoryNumber>[\\d]{4}/[\\d]*)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_NUMBER);

        AdvisoryNumberImpl advisoryNumber = AdvisoryNumberImpl.builder().from(match.group("advisoryNumber")).build();
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, advisoryNumber);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                fi.fmi.avi.model.swx.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisory) msg).getAdvisoryNumber();

                if (advisoryNumber.getSerialNumber() == 0) {
                    throw new SerializingException("The advisory number is missing the serial number");
                }

                if (advisoryNumber.getYear() == 0) {
                    throw new SerializingException(("The advisory number is missing the year"));
                }

                StringBuilder builder = new StringBuilder();
                builder.append("ADVISORY NR: ");
                appendWhiteSpaceToString(builder, 21);
                builder.append(advisoryNumber.getYear());
                builder.append("/");
                builder.append(advisoryNumber.getSerialNumber());

                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.ADVISORY_NUMBER));

            }
            return retval;
        }
    }
}
