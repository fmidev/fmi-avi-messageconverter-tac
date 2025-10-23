package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.AdvisoryNumber;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Matcher;

public class ReplaceAdvisoryNumber extends AbstractAdvisoryNumber {
    public ReplaceAdvisoryNumber(final OccurrenceFrequency prio) {
        super(prio, LexemeIdentity.REPLACE_ADVISORY_NUMBER, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            final LexemeIdentity previous = token.getPrevious().getIdentity();
            if (previous != null && (previous.equals(previousLexemeIdentity) || previous.equals(lexemeIdentity))) {
                token.identify(lexemeIdentity);
                token.setParsedValue(Lexeme.ParsedValueName.YEAR, Integer.parseInt(match.group("year")));
                token.setParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, Integer.parseInt(match.group("serialNo")));
            }
        }
    }

    public static class Reconstructor extends AbstractAdvisoryNumber.AbstractReconstructor {
        public Reconstructor() {
            super(LexemeIdentity.REPLACE_ADVISORY_NUMBER);
        }

        @Override
        protected <T extends AviationWeatherMessageOrCollection> Optional<String> getAdvisoryNumberString(
                final T msg, final Class<T> clz) throws SerializingException {

            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd79.AdvisoryNumber advisoryNumber =
                        ((SpaceWeatherAdvisoryAmd79) msg).getReplaceAdvisoryNumber().orElse(null);
                if (advisoryNumber == null) {
                    return Optional.empty();
                }
                return Optional.of(toAdvisoryNumberString(advisoryNumber.getYear(), advisoryNumber.getSerialNumber()));
            }

            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final List<AdvisoryNumber> replaceNumbers = ((SpaceWeatherAdvisoryAmd82) msg).getReplaceAdvisoryNumber();
                if (replaceNumbers.isEmpty()) {
                    return Optional.empty();
                }
                final StringJoiner joiner = new StringJoiner(" ");
                for (final fi.fmi.avi.model.swx.amd82.AdvisoryNumber n : replaceNumbers) {
                    joiner.add(toAdvisoryNumberString(n.getYear(), n.getSerialNumber()));
                }
                return Optional.of(joiner.toString());
            }

            return Optional.empty();
        }
    }

}
