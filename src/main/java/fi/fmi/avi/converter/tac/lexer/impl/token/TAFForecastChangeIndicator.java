package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR;

import java.util.ArrayList;
import java.util.List;
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
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Token parser for TAF change forecast type indicators (TEMPO, BECMG, PROB30/40, PROB30/40 TEMPO, FM).
 */
public class TAFForecastChangeIndicator extends TimeHandlingRegex {

    public TAFForecastChangeIndicator(final OccurrenceFrequency prio) {
        super("^(TEMPO|BECMG|PROB40|PROB30|PROB30 TEMPO|PROB40 TEMPO)|(FM([0-9]{2})?([0-9]{2})([0-9]{2}))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final ForecastChangeIndicatorType indicator;
        if (match.group(1) != null) {
            token.identify(TAF_FORECAST_CHANGE_INDICATOR);
            indicator = ForecastChangeIndicatorType.forCode(match.group(1));
            token.setParsedValue(TYPE, indicator);
        } else if (match.group(2) != null) {
            indicator = ForecastChangeIndicatorType.FROM;
            int day = -1;
            if (match.group(3) != null) {
                day = Integer.parseInt(match.group(3));
            }
            final int hour = Integer.parseInt(match.group(4));
            final int minute = Integer.parseInt(match.group(5));
            if (timeOkDayHourMinute(day, hour, minute)) {
                token.identify(TAF_FORECAST_CHANGE_INDICATOR);
                if (day > -1) {
                    token.setParsedValue(DAY1, day);
                }
                token.setParsedValue(HOUR1, hour);
                token.setParsedValue(MINUTE1, minute);
                token.setParsedValue(TYPE, indicator);
            } else {
                token.identify(TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Status.SYNTAX_ERROR, "Invalid time");
            }
        }
    }

    public enum ForecastChangeIndicatorType {
        TEMPORARY_FLUCTUATIONS("TEMPO"),
        BECOMING("BECMG"),
        WITH_40_PCT_PROBABILITY("PROB40"),
        WITH_30_PCT_PROBABILITY("PROB30"),
        TEMPO_WITH_40_PCT_PROBABILITY("PROB40 TEMPO"),
        TEMPO_WITH_30_PCT_PROBABILITY("PROB30 TEMPO"),
        FROM("FM");

        private final String code;

        ForecastChangeIndicatorType(final String code) {
            this.code = code;
        }

        public static ForecastChangeIndicatorType forCode(final String code) {
            for (final ForecastChangeIndicatorType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        private static String encodeValidityTimeFrom(final PartialOrCompleteTimeInstant instant, final ConversionHints hints) {
            final String retval;
            boolean useShortFormat = false;
            if (hints != null) {
                final Object hint = hints.get(ConversionHints.KEY_VALIDTIME_FORMAT);
                if (ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT.equals(hint)) {
                    useShortFormat = true;
                }
            }

            if (!instant.getDay().isPresent() || useShortFormat) {
                retval = String.format("%02d%02d", instant.getHour().orElse(-1), instant.getMinute().orElse(-1));
            } else {
                // Otherwise produce validity in the long format
                retval = String.format("%02d%02d%02d", instant.getDay().orElse(-1), instant.getHour().orElse(-1), instant.getMinute().orElse(-1));
            }
            return retval;
        }

        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            final List<Lexeme> retval = new ArrayList<>();

            if (TAF.class.isAssignableFrom(clz)) {
                final Optional<TAFChangeForecast> changeForecast = ctx.getParameter("forecast", TAFChangeForecast.class);

                if (changeForecast.isPresent()) {
                    switch (changeForecast.get().getChangeIndicator()) {
                        case BECOMING:
                            retval.add(this.createLexeme("BECMG", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case TEMPORARY_FLUCTUATIONS:
                            retval.add(this.createLexeme("TEMPO", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case PROBABILITY_30:
                            retval.add(this.createLexeme("PROB30", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case PROBABILITY_40:
                            retval.add(this.createLexeme("PROB40", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case PROBABILITY_30_TEMPORARY_FLUCTUATIONS:
                            retval.add(this.createLexeme("PROB30", TAF_FORECAST_CHANGE_INDICATOR));
                            retval.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
                            retval.add(this.createLexeme("TEMPO", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case PROBABILITY_40_TEMPORARY_FLUCTUATIONS:
                            retval.add(this.createLexeme("PROB40", TAF_FORECAST_CHANGE_INDICATOR));
                            retval.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
                            retval.add(this.createLexeme("TEMPO", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case FROM:
                            if (changeForecast.get().getPeriodOfChange().getStartTime().isPresent()) {
                                retval.add(this.createLexeme(
                                        "FM" + encodeValidityTimeFrom(changeForecast.get().getPeriodOfChange().getStartTime().get(), ctx.getHints()),
                                        TAF_FORECAST_CHANGE_INDICATOR));
                            } else {
                                throw new SerializingException("Validity time start is not available in TAF change forecast");
                            }
                            break;
                    }
                }
            }
            return retval;
        }

    }

}
