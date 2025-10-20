package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.Optional;

public class ReplaceAdvisoryNumber extends AbstractAdvisoryNumber {
    public ReplaceAdvisoryNumber(final OccurrenceFrequency prio) {
        super(prio, LexemeIdentity.REPLACE_ADVISORY_NUMBER, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
    }

    public static class Reconstructor extends AbstractAdvisoryNumber.AbstractReconstructor {
        public Reconstructor() {
            super(LexemeIdentity.REPLACE_ADVISORY_NUMBER);
        }

        @Override
        protected <T extends AviationWeatherMessageOrCollection> Optional<String> getAdvisoryNumberString(final T msg, final Class<T> clz) throws SerializingException {
            String nullableAdvisoryNumberString = null;
            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd79.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisoryAmd79) msg).getReplaceAdvisoryNumber().orElse(null);
                if (advisoryNumber != null) {
                    nullableAdvisoryNumberString = toAdvisoryNumberString(advisoryNumber.getYear(), advisoryNumber.getSerialNumber());
                }
            } else if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd82.AdvisoryNumber advisoryNumber = ((SpaceWeatherAdvisoryAmd82) msg).getReplaceAdvisoryNumber().orElse(null);
                if (advisoryNumber != null) {
                    nullableAdvisoryNumberString = toAdvisoryNumberString(advisoryNumber.getYear(), advisoryNumber.getSerialNumber());
                }
            }
            return Optional.ofNullable(nullableAdvisoryNumberString);
        }
    }
}
