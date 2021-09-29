package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TREND_TIME_GROUP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.metar.TrendForecast;

/**
 * Trend time group token parser (FM, AT or TL).
 */
public class TrendTimeGroup extends TimeHandlingRegex {
    public TrendTimeGroup(final OccurrenceFrequency prio) {
        super("^(FM|TL|AT)([0-9]{2})([0-9]{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final TrendTimePeriodType type = TrendTimePeriodType.forCode(match.group(1));

        final int hour = Integer.parseInt(match.group(2));
        final int minute = Integer.parseInt(match.group(3));
        if (timeOkHourMinute(hour, minute)) {
            token.identify(LexemeIdentity.TREND_TIME_GROUP);
            token.setParsedValue(HOUR1, hour);
            token.setParsedValue(MINUTE1, minute);
            token.setParsedValue(TYPE, type);
        } else {
            token.identify(LexemeIdentity.TREND_TIME_GROUP, Lexeme.Status.SYNTAX_ERROR, "Invalid time");
        }
    }

    public enum TrendTimePeriodType {
        FROM("FM"), AT("AT"), UNTIL("TL");

        private final String code;

        TrendTimePeriodType(final String code) {
            this.code = code;
        }

        public static TrendTimePeriodType forCode(final String code) {
            for (final TrendTimePeriodType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            final Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
            if (trend.isPresent()) {
                PartialOrCompleteTime validity = null;
                if (trend.get().getPeriodOfChange().isPresent()) {
                    validity = trend.get().getPeriodOfChange().get();
                } else if (trend.get().getInstantOfChange().isPresent()) {
                    validity = trend.get().getInstantOfChange().get();
                }
                if (validity != null) {
                    switch (trend.get().getChangeIndicator()) {
                        case BECOMING:
                        case TEMPORARY_FLUCTUATIONS:
                            return createTrendTimeChangePeriods(validity);
                    }
                }
            }
            return Collections.emptyList();
        }

        private List<Lexeme> createTrendTimeChangePeriods(final PartialOrCompleteTime time) {
            final List<Lexeme> retval = new ArrayList<>();
            if (time instanceof PartialOrCompleteTimeInstant) {
                final PartialOrCompleteTimeInstant instant = (PartialOrCompleteTimeInstant) time;
                retval.add(this.createLexeme(String.format("AT%02d%02d", instant.getHour().orElse(-1), instant.getMinute().orElse(-1)), TREND_TIME_GROUP));
            } else if (time instanceof PartialOrCompleteTimePeriod) {
                final PartialOrCompleteTimePeriod period = (PartialOrCompleteTimePeriod) time;
                if (period.getStartTime().isPresent()) {
                    final PartialOrCompleteTimeInstant start = period.getStartTime().get();
                    retval.add(this.createLexeme(String.format("FM%02d%02d", start.getHour().orElse(-1), start.getMinute().orElse(-1)), TREND_TIME_GROUP));
                }
                if (period.getEndTime().isPresent()) {
                    final PartialOrCompleteTimeInstant end = period.getEndTime().get();
                    retval.add(this.createLexeme(String.format("TL%02d%02d", end.getHour().orElse(-1), end.getMinute().orElse(-1)), TREND_TIME_GROUP));
                }
            }
            return retval;
        }

    }

}
