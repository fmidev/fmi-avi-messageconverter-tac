package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.time.DateTimeException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;

public class AdvisoryPhenomenaTimeGroup extends RegexMatchingLexemeVisitor {

    // Note that this accepts hour 24 and parses it into 0 even though it should throw an exception according to the Javadoc
    private static final DateTimeFormatter ADVISORY_PHENOMENA_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/HHmm'Z'");
    private static final int INVALID_MIDNIGHT = 24;

    public AdvisoryPhenomenaTimeGroup(final OccurrenceFrequency prio) {
        super("^(?<day>[0-9]{2})/(?<hour>[0-9]{2})(?<minute>[0-9]{2})Z$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && LexemeIdentity.ADVISORY_PHENOMENA_LABEL.equals(token.getPrevious().getIdentity())) {
                token.identify(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
                final int hour = Integer.parseInt(match.group("hour"));
                if (hour < INVALID_MIDNIGHT) {
                    try {
                        final TemporalAccessor parsed = ADVISORY_PHENOMENA_TIME_FORMAT.parse(token.getTACToken());
                        token.setParsedValue(Lexeme.ParsedValueName.DAY1, parsed.get(ChronoField.DAY_OF_MONTH));
                        token.setParsedValue(Lexeme.ParsedValueName.HOUR1, parsed.get(ChronoField.HOUR_OF_DAY));
                        token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, parsed.get(ChronoField.MINUTE_OF_HOUR));
                    } catch (DateTimeException e) {
                        // NOOP, ignore silently if the time is not valid
                    }
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final Optional<Integer> index = ctx.getParameter("analysisIndex", Integer.class);
                if (index.isPresent()) {
                    final SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index.get());
                    final StringBuilder builder = new StringBuilder();
                    final PartialOrCompleteTimeInstant timeInstant = analysis.getTime();
                    if (timeInstant.getCompleteTime().isPresent()) {
                        builder.append(timeInstant.getCompleteTime().get().format(ADVISORY_PHENOMENA_TIME_FORMAT));
                    } else if (timeInstant.getPartialTime().isPresent()) {
                        final OptionalInt day = timeInstant.getPartialTime().get().getDay();
                        final OptionalInt hour = timeInstant.getPartialTime().get().getHour();
                        final OptionalInt minute = timeInstant.getPartialTime().get().getMinute();
                        if (day.isPresent() && hour.isPresent() && minute.isPresent()) {
                            builder.append(String.format("%02d/%02d%02dZ", day.getAsInt(), hour.getAsInt(), minute.getAsInt()));
                        } else {
                            throw new SerializingException("Insufficient partial analysis time, day:" + day + ", hour:" + hour + ", minute:" + minute);
                        }
                    } else {
                        throw new SerializingException("Analysis time is missing");
                    }
                    retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP));
                }
            }
            return retval;
        }

    }

}
