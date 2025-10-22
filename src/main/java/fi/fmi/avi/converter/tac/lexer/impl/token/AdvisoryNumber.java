package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
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
        protected <T extends AviationWeatherMessageOrCollection> Optional<String> getAdvisoryNumberString(final T msg, final Class<T> clz) throws SerializingException {
            String nullableAdvisoryNumberString = null;
            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd79.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisoryAmd79) msg).getAdvisoryNumber();
                nullableAdvisoryNumberString = toAdvisoryNumberString(advisoryNumber.getYear(), advisoryNumber.getSerialNumber());
            } else if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd82.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisoryAmd82) msg).getAdvisoryNumber();
                nullableAdvisoryNumberString = toAdvisoryNumberString(advisoryNumber.getYear(), advisoryNumber.getSerialNumber());
            }
            return Optional.ofNullable(nullableAdvisoryNumberString);
        }
    }
}
