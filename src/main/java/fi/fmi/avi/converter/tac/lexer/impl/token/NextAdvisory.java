package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import javax.annotation.Nullable;
import java.time.DateTimeException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;

import static java.util.Objects.requireNonNull;

public class NextAdvisory extends TimeHandlingRegex {
    public NextAdvisory(final OccurrenceFrequency prio) {
        super("(?<type>WILL\\s+BE\\s+ISSUED\\s+BY\\s*)?((?<year>[0-9]{4})(?<month>[0-1][0-9])(?<day>[0-3][0-9])\\/"
                + "(?<hour>[0-2][0-9])(?<minute>[0-5][0-9])Z)|(?<nofurther>NO\\s+FURTHER\\s+ADVISORIES)", prio);
    }

    private static Type resolveType(final Matcher matcher) {
        if (matcher.group("nofurther") != null) {
            return Type.NO_FURTHER_ADVISORIES;
        } else if (matcher.group("type") != null) {
            return Type.WILL_BE_ISSUED_BY;
        } else {
            return Type.AT;
        }
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && token.getPrevious().getIdentity().equals(LexemeIdentity.NEXT_ADVISORY_LABEL)) {
                token.identify(LexemeIdentity.NEXT_ADVISORY);

                final Type type = resolveType(match);
                token.setParsedValue(Lexeme.ParsedValueName.TYPE, type);
                if (type != Type.NO_FURTHER_ADVISORIES) {
                    setParsedDateValues(token, match);
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
            // Check validity of values
            //noinspection ResultOfMethodCallIgnored
            ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.UTC);
            token.setParsedValue(Lexeme.ParsedValueName.YEAR, year);
            token.setParsedValue(Lexeme.ParsedValueName.MONTH, month);
            token.setParsedValue(Lexeme.ParsedValueName.DAY1, day);
            token.setParsedValue(Lexeme.ParsedValueName.HOUR1, hour);
            token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, minute);
        } catch (final DateTimeException e) {
            // NOOP, ignore silently if the time is not valid
        }
    }

    public enum Type {
        AT("",
                fi.fmi.avi.model.swx.amd79.NextAdvisory.Type.NEXT_ADVISORY_AT,
                fi.fmi.avi.model.swx.amd82.NextAdvisory.Type.NEXT_ADVISORY_AT),
        WILL_BE_ISSUED_BY("WILL BE ISSUED BY ",
                fi.fmi.avi.model.swx.amd79.NextAdvisory.Type.NEXT_ADVISORY_BY,
                fi.fmi.avi.model.swx.amd82.NextAdvisory.Type.NEXT_ADVISORY_BY),
        NO_FURTHER_ADVISORIES("NO FURTHER ADVISORIES",
                fi.fmi.avi.model.swx.amd79.NextAdvisory.Type.NO_FURTHER_ADVISORIES,
                fi.fmi.avi.model.swx.amd82.NextAdvisory.Type.NO_FURTHER_ADVISORIES);

        private final String tacContent;
        private final fi.fmi.avi.model.swx.amd79.NextAdvisory.Type amd79Type;
        private final fi.fmi.avi.model.swx.amd82.NextAdvisory.Type amd82Type;

        Type(
                final String tacContent,
                final fi.fmi.avi.model.swx.amd79.NextAdvisory.Type amd79Type,
                final fi.fmi.avi.model.swx.amd82.NextAdvisory.Type amd82Type) {
            this.tacContent = requireNonNull(tacContent, "tacContent");
            this.amd79Type = requireNonNull(amd79Type, "amd79Type");
            this.amd82Type = requireNonNull(amd82Type, "amd82Type");
        }

        public fi.fmi.avi.model.swx.amd79.NextAdvisory.Type getAmd79Type() {
            return amd79Type;
        }

        public fi.fmi.avi.model.swx.amd82.NextAdvisory.Type getAmd82Type() {
            return amd82Type;
        }

        public String getTacContent() {
            return tacContent;
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        private static void appendNextAdvisoryTime(final StringBuilder builder, @Nullable final PartialOrCompleteTimeInstant nextAdvisoryTime) {
            Optional.ofNullable(nextAdvisoryTime)//
                    .flatMap(PartialOrCompleteTimeInstant::getCompleteTime)//
                    .ifPresent(completeTime -> builder.append(completeTime.format(DateTimeFormatter.ofPattern("yyyyMMdd/HHmm'Z'"))));
        }

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd82.NextAdvisory nextAdvisory = ((SpaceWeatherAdvisoryAmd82) msg).getNextAdvisory();
                if (nextAdvisory == null) {
                    throw new SerializingException("Next advisory is missing");
                }
                final StringBuilder builder = new StringBuilder();
                if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.amd82.NextAdvisory.Type.NEXT_ADVISORY_BY)) {
                    builder.append(Type.WILL_BE_ISSUED_BY.getTacContent());
                } else if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.amd82.NextAdvisory.Type.NO_FURTHER_ADVISORIES)) {
                    builder.append(Type.NO_FURTHER_ADVISORIES.getTacContent());
                }
                appendNextAdvisoryTime(builder, nextAdvisory.getTime().orElse(null));

                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.NEXT_ADVISORY));
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd79.NextAdvisory nextAdvisory = ((SpaceWeatherAdvisoryAmd79) msg).getNextAdvisory();
                if (nextAdvisory == null) {
                    throw new SerializingException("Next advisory is missing");
                }
                final StringBuilder builder = new StringBuilder();
                if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.amd79.NextAdvisory.Type.NEXT_ADVISORY_BY)) {
                    builder.append(Type.WILL_BE_ISSUED_BY.getTacContent());
                } else if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.amd79.NextAdvisory.Type.NO_FURTHER_ADVISORIES)) {
                    builder.append(Type.NO_FURTHER_ADVISORIES.getTacContent());
                }
                appendNextAdvisoryTime(builder, nextAdvisory.getTime().orElse(null));

                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.NEXT_ADVISORY));
            }
            return retval;
        }
    }
}
