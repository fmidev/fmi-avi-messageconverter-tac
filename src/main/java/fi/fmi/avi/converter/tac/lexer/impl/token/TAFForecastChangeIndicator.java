package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Token parser for TAF change forecast type indicators (TEMPO, BECMG, PROB30/40, PROB30/40 TEMPO, FM).
 */
public class TAFForecastChangeIndicator extends TimeHandlingRegex {

	public enum ForecastChangeIndicatorType {
        TEMPORARY_FLUCTUATIONS("TEMPO"),
        BECOMING("BECMG"),
        WITH_40_PCT_PROBABILITY("PROB40"),
        WITH_30_PCT_PROBABILITY("PROB30"),
        TEMPO_WITH_40_PCT_PROBABILITY("PROB40 TEMPO"),
        TEMPO_WITH_30_PCT_PROBABILITY("PROB30 TEMPO"),
        FROM("FM");

        private String code;

        ForecastChangeIndicatorType(final String code) {
            this.code = code;
        }

        public static ForecastChangeIndicatorType forCode(final String code) {
            for (ForecastChangeIndicatorType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }

    public TAFForecastChangeIndicator(final Priority prio) {
        super("^(TEMPO|BECMG|PROB40|PROB30|PROB30 TEMPO|PROB40 TEMPO)|(FM([0-9]{2})?([0-9]{2})([0-9]{2}))$",
                prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        ForecastChangeIndicatorType indicator;
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
            int hour = Integer.parseInt(match.group(4));
            int minute = Integer.parseInt(match.group(5));
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
    

    public static class Reconstructor extends FactoryBasedReconstructor {

		@Override
        public <T extends AviationWeatherMessage> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
                throws SerializingException {
            List<Lexeme> retval = new ArrayList<>();

            if (TAF.class.isAssignableFrom(clz)) {
                TAFChangeForecast changeForecast = getAs(specifier, TAFChangeForecast.class);

                if (changeForecast != null) {
                    switch (changeForecast.getChangeIndicator()) {
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
                            retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                            retval.add(this.createLexeme("TEMPO", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case PROBABILITY_40_TEMPORARY_FLUCTUATIONS:
                            retval.add(this.createLexeme("PROB40", TAF_FORECAST_CHANGE_INDICATOR));
                            retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                            retval.add(this.createLexeme("TEMPO", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                        case FROM:
                            StringBuilder ret = new StringBuilder("FM");
                            if (changeForecast.getValidityStartDayOfMonth() > -1) {
                                ret.append(String.format("%02d%02d%02d", changeForecast.getValidityStartDayOfMonth(), changeForecast.getValidityStartHour(),
                                        changeForecast.getValidityStartMinute()));
                            } else {
                                ret.append(String.format("%02d%02d", changeForecast.getValidityStartHour(), changeForecast.getValidityStartMinute()));
                            }
                            retval.add(this.createLexeme(ret.toString(), TAF_FORECAST_CHANGE_INDICATOR));
                            break;
                    }
                }
            }
            return retval;
        }

    }

}
