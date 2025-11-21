package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.Optional;

public class AdvisoryNumber extends AbstractAdvisoryNumber {
    public AdvisoryNumber(final OccurrenceFrequency prio) {
        super(prio, LexemeIdentity.ADVISORY_NUMBER, LexemeIdentity.ADVISORY_NUMBER_LABEL);
    }

    public static class Reconstructor extends AbstractAdvisoryNumber.AbstractReconstructor {
        public Reconstructor() {
            super(LexemeIdentity.ADVISORY_NUMBER);
        }

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) throws SerializingException {
            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd79.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisoryAmd79) msg).getAdvisoryNumber();
                return Optional.of(createAdvisoryNumberLexeme(advisoryNumber.getYear(), advisoryNumber.getSerialNumber()));
            }
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd82.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisoryAmd82) msg).getAdvisoryNumber();
                return Optional.of(createAdvisoryNumberLexeme(advisoryNumber.getYear(), advisoryNumber.getSerialNumber()));
            }
            return Optional.empty();
        }
    }
}
