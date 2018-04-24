package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.TrendForecast;

/**
 * Trend time group token parser (FM, AT or TL).
 */
public class TrendTimeGroup extends TimeHandlingRegex {
    public enum TrendTimePeriodType {
        FROM("FM"), AT("AT"), UNTIL("TL");

        private String code;

        TrendTimePeriodType(final String code) {
            this.code = code;
        }

        public static TrendTimePeriodType forCode(final String code) {
            for (TrendTimePeriodType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }

    public TrendTimeGroup(final Priority prio) {
        super("^(FM|TL|AT)([0-9]{2})([0-9]{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        TrendTimePeriodType type = TrendTimePeriodType.forCode(match.group(1));

        int hour = Integer.parseInt(match.group(2));
        int minute = Integer.parseInt(match.group(3));
        if (timeOkHourMinute(hour, minute)) {
            token.identify(Identity.TREND_TIME_GROUP);
            token.setParsedValue(HOUR1, hour);
            token.setParsedValue(MINUTE1, minute);
            token.setParsedValue(TYPE, type);
        } else {
            token.identify(Identity.TREND_TIME_GROUP, Lexeme.Status.SYNTAX_ERROR, "Invalid time");
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessage> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
                throws SerializingException {
            if (METAR.class.isAssignableFrom(clz)) {
                Optional<TrendForecast> trend = getAs(specifier, TrendForecast.class);
                if (trend.isPresent()) {
                    PartialOrCompleteTime validity = null;
                    if (trend.get().getPeriodOfChange().isPresent()) {
                        validity = trend.get().getPeriodOfChange().get();
                    } else if (trend.get().getInstantOfChange().isPresent()) {
                        validity = trend.get().getInstantOfChange().get();
                    }
                    if (validity != null) {
                        switch (trend.get().getChangeIndicator()) {
                            case BECOMING: {
                                return createTrendTimeChangePeriods(validity);
                            }
                            case TEMPORARY_FLUCTUATIONS: {
                                return createTrendTimeChangePeriods(validity);
                            }
                        }
                    }
                }
            }
            return Collections.emptyList();
        }

        private List<Lexeme> createTrendTimeChangePeriods(final PartialOrCompleteTime time) {
            List<Lexeme> retval = new ArrayList<>();
            if (time instanceof PartialOrCompleteTimeInstant) {
                PartialOrCompleteTimeInstant instant = (PartialOrCompleteTimeInstant) time;
                retval.add(this.createLexeme(String.format("AT%02d%02d", instant.getHour(), instant.getMinute()), TREND_TIME_GROUP));
            } else if (time instanceof PartialOrCompleteTimePeriod) {
                PartialOrCompleteTimePeriod period = (PartialOrCompleteTimePeriod) time;
                if (period.getStartTime().isPresent()) {
                    PartialOrCompleteTimeInstant start = period.getStartTime().get();
                    retval.add(this.createLexeme(String.format("FM%02d%02d", start.getHour(), start.getMinute()), TREND_TIME_GROUP));
                    }
                if (period.getEndTime().isPresent()) {
                    PartialOrCompleteTimeInstant end = period.getEndTime().get();
                    retval.add(this.createLexeme(String.format("TL%02d%02d", end.getHour(), end.getMinute()), TREND_TIME_GROUP));
                    }
                }
            return retval;
        }

    }

}
