package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR2;

import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
public class ValidTime extends TimeHandlingRegex {
    private static final Logger LOG = LoggerFactory.getLogger(ValidTime.class);

    public ValidTime(final Priority prio) {
        super("^(([0-9]{2})([0-9]{2})([0-9]{2}))|(([0-9]{2})([0-9]{2})/([0-9]{2})([0-9]{2}))$", prio);
    }

    static int calculateNumberOfHours(final PartialOrCompleteTimePeriod period) {
        if (period.getStartTime().isPresent() && period.getEndTime().isPresent()) {
            PartialOrCompleteTimeInstant start = period.getStartTime().get();
            PartialOrCompleteTimeInstant end = period.getEndTime().get();
            if (start.getCompleteTime().isPresent() && end.getCompleteTime().isPresent()) {
                long hours = Duration.between(start.getCompleteTime().get(), end.getCompleteTime().get()).toHours();
                if (hours < Integer.MAX_VALUE) {
                    return (int) hours;
                } else {
                    throw new IllegalArgumentException("Too many hours between " + start.getCompleteTime() + " and " + end.getCompleteTime());
                }
            } else {

                /*
                 * This is trickier than one might expect. There are first of all special cases where
                 * people write numbers that look alright but are difficult for computer. Like 0700/0724
                 * Another class of problematic timecodes is where the days roll over to the next month.
                 * This tool has no context to determine which month and how many days there are supposed
                 * to be in it. However because of the domain, we make the assumption that endDay is
                 * startDay + 1 unless startDay < 28. If startDay < 28 => throw an exception as we cannot
                 * make assumptions on the length of month.
                 */
                int startDay = start.getDay();
                int startHour = start.getHour();
                int endDay = end.getDay();
                int endHour = end.getHour();

                if (endDay == -1) {
                    endDay = startDay;
                }

                // Store original parameters for exception texts
                String dateStr = String.format("%02d%02d/%02d%02d", startDay, startHour, endDay, endHour);

                if (endHour == 24) {
                    endHour = 0;
                    endDay++;
                }

                if (endDay < startDay) {
                    if (startDay < 28) {
                        throw new IllegalArgumentException("Unable to calculate number of hours of a period " + dateStr);
                    }

                    // Make the assumption that startDay is the last day in that month. This
                    // assumption is based on the domain: aviation forecasts will not span more than 48 hours
                    endDay += startDay;
                }

                int startN = startDay * 24 + startHour;
                int endN = endDay * 24 + endHour;

                return endN - startN;

            }
        } else {
            throw new IllegalArgumentException("Unable to calculate number of hours in a period");
        }
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() && token.getPrevious().getIdentity() == Identity.ISSUE_TIME) {
            if (match.group(1) != null) {
                //old 24h TAF, just one day field
                int day = Integer.parseInt(match.group(2));
                int fromHour = Integer.parseInt(match.group(3));
                int toHour = Integer.parseInt(match.group(4));
                if (timeOkDayHour(day, fromHour) && timeOkDayHour(day, toHour)) {
                    token.identify(Identity.VALID_TIME);
                    token.setParsedValue(DAY1, day);
                    token.setParsedValue(HOUR1, fromHour);
                    token.setParsedValue(HOUR2, toHour);
                } else {
                    token.identify(Identity.VALID_TIME, Lexeme.Status.SYNTAX_ERROR, "Invalid date and/or time");
                }

            } else {
                //30h TAF
                int fromDay = Integer.parseInt(match.group(6));
                int fromHour = Integer.parseInt(match.group(7));
                int toDay = Integer.parseInt(match.group(8));
                int toHour = Integer.parseInt(match.group(9));
                if (timeOkDayHour(fromDay, fromHour) && timeOkDayHour(toDay, toHour)) {
                    token.identify(Identity.VALID_TIME);
                    token.setParsedValue(DAY1, fromDay);
                    token.setParsedValue(DAY2, toDay);
                    token.setParsedValue(HOUR1, fromHour);
                    token.setParsedValue(HOUR2, toHour);
                } else {
                    token.identify(Identity.VALID_TIME, Lexeme.Status.SYNTAX_ERROR, "Invalid date and/or time");
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        private static String encodeValidityTimePeriod(final PartialOrCompleteTimePeriod period, final ConversionHints hints) {
            String retval = null;
            boolean useShortFormat = false;
            try {
                if (hints != null) {
                    Object hint = hints.get(ConversionHints.KEY_VALIDTIME_FORMAT);
                    if (ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT.equals(hint)) {
                        int numberOfHours = calculateNumberOfHours(period);
                        if (numberOfHours < 24) {
                            useShortFormat = true;
                        }
                    }
                }
            } catch (IllegalArgumentException iae) {
                LOG.info("Unable to determine whether to use long format or not", iae);
            }
            if (period.getStartTime().isPresent() && period.getEndTime().isPresent()) {
                PartialOrCompleteTimeInstant start = period.getStartTime().get();
                PartialOrCompleteTimeInstant end = period.getEndTime().get();
                if (end.getDay() < 0 || useShortFormat) {
                    retval = String.format("%02d%02d%02d", start.getDay(), start.getHour(), end.getHour());
                } else {
                    retval = String.format("%02d%02d/%02d%02d", start.getDay(), start.getHour(), end.getDay(), end.getHour());
                }
            }
            return retval;
        }

        @Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (TAF.class.isAssignableFrom(clz)) {
                TAF taf = (TAF) msg;
                if (taf.getValidityTime().isPresent()) {
                    String period = encodeValidityTimePeriod(taf.getValidityTime().get(), ctx.getHints());
                    return Optional.of(this.createLexeme(period, Lexeme.Identity.VALID_TIME));
                } else {
                    throw new SerializingException("Validity time not available in TAF");
                }
            }
            return Optional.empty();
        }
    }
}
