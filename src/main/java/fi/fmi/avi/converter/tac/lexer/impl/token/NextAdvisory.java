package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class NextAdvisory extends TimeHandlingRegex {
    public NextAdvisory(final OccurrenceFrequency prio) {
        super("(?<type>WILL\\sBE\\sISSUED\\sBY\\s?)?((?<year>[0-9]{4})(?<month>[0-1][0-9])(?<day>[0-3][0-9])\\/"
                + "(?<hour>[0-2][0-9])(?<minute>[0-5][0-9])Z)|(?<nofurther>NO\\sFURTHER\\sADVISORIES)", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && token.getPrevious().getIdentity().equals(LexemeIdentity.NEXT_ADVISORY_LABEL)) {
                token.identify(LexemeIdentity.NEXT_ADVISORY);

                String type = match.group("nofurther");
                type = type == null ? match.group("type") : type;

                if (type == null) {
                    token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_AT);
                    setParsedDateValues(token, match);
                } else if (type.trim().toUpperCase().equals("WILL BE ISSUED BY")) {
                    token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_BY);
                    setParsedDateValues(token, match);
                } else {
                    token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES);
                }
            }
        }
    }

    private void setParsedDateValues(final Lexeme token, final Matcher match) {
        final int year = Integer.parseInt(match.group("year"));
        final int month = Integer.parseInt(match.group("month"));
        final int day = Integer.parseInt(match.group("day"));
        final int hour = Integer.parseInt(match.group("hour"));
        final int minute = Integer.parseInt(match.group("minute"));

        try {
            ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Z"));
            token.setParsedValue(Lexeme.ParsedValueName.YEAR, year);
            token.setParsedValue(Lexeme.ParsedValueName.MONTH, month);
            token.setParsedValue(Lexeme.ParsedValueName.DAY1, day);
            token.setParsedValue(Lexeme.ParsedValueName.HOUR1, hour);
            token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, minute);
        } catch (DateTimeException e) {
            // NOOP, ignore silently if the time is not valid
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                fi.fmi.avi.model.swx.NextAdvisory nextAdvisory = ((SpaceWeatherAdvisory) msg).getNextAdvisory();
                if (nextAdvisory == null) {
                    throw new SerializingException("Next advisory is missing");
                }
                StringBuilder builder = new StringBuilder();
                if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_BY)) {
                    builder.append("WILL BE ISSUED BY ");
                } else if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES)) {
                    builder.append("NO FURTHER ADVISORIES");
                }

                if (nextAdvisory.getTime().isPresent()) {
                    PartialOrCompleteTimeInstant time = nextAdvisory.getTime().get();
                    builder.append(time.getCompleteTime().get().format(DateTimeFormatter.ofPattern("yyyyMMdd/HHmm'Z'")));
                }

                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.NEXT_ADVISORY));
            }
            return retval;
        }
    }
}
