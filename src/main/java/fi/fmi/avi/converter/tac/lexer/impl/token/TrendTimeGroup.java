package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.TrendTimeGroups;

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
            List<Lexeme> retval = new ArrayList<>();
            if (METAR.class.isAssignableFrom(clz)) {
                TrendForecast trend = getAs(specifier, TrendForecast.class);
                if (trend != null) {
                    switch (trend.getChangeIndicator()) {
                        case BECOMING: {
                            List<Lexeme> periodOfChange = createTrendTimeChangePeriods(trend.getTimeGroups());
                            if (periodOfChange.isEmpty()) {
                                throw new SerializingException("No period of time for the trend of type BECOMING");
                            }
                            retval.addAll(periodOfChange);
                            break;
                        }
                        case TEMPORARY_FLUCTUATIONS: {
                            List<Lexeme> periodOfChange = createTrendTimeChangePeriods(trend.getTimeGroups());
                            if (!periodOfChange.isEmpty()) {
                                retval.addAll(periodOfChange);
                            }
                            break;
                        }
                    }
                }

            }
            return retval;
        }

        private List<Lexeme> createTrendTimeChangePeriods(final TrendTimeGroups timeGroups) {
            List<Lexeme> retval = new ArrayList<>();
            if (timeGroups != null) {
                if (timeGroups.isSingleInstance()) {
                    if (timeGroups.getPartialStartTime() != null) {
                        retval.add(this.createLexeme("AT" + timeGroups.getPartialStartTime(), TREND_TIME_GROUP));
                    }
                } else {
                    if (timeGroups.getPartialStartTime() != null) {
                        retval.add(this.createLexeme("FM" + timeGroups.getPartialStartTime(), TREND_TIME_GROUP));
                    }
                    if (timeGroups.getPartialEndTime() != null) {
                        retval.add(this.createLexeme("TL" + timeGroups.getPartialEndTime(), TREND_TIME_GROUP));
                    }
                }
            }
            return retval;
        }

    }

}
