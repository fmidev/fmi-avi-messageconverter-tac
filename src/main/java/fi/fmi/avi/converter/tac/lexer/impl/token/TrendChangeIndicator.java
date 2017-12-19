package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.TrendTimeGroups;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;

/**
 * Created by rinne on 10/02/17.
 */
public class TrendChangeIndicator extends TimeHandlingRegex {

	public enum TrendChangeIndicatorType {
        TEMPORARY_FLUCTUATIONS("TEMPO"),
        BECOMING("BECMG"),
        NO_SIGNIFICANT_CHANGES("NOSIG");
      
        private String code;

        TrendChangeIndicatorType(final String code) {
            this.code = code;
        }

        public static TrendChangeIndicatorType forCode(final String code) {
            for (TrendChangeIndicatorType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }

    public TrendChangeIndicator(final Priority prio) {
        super("^(NOSIG|TEMPO|BECMG)$",
                prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        TrendChangeIndicatorType indicator;
        if (match.group(1) != null) {
        	token.identify(Identity.TREND_CHANGE_INDICATOR);
            indicator = TrendChangeIndicatorType.forCode(match.group(1));
            token.setParsedValue(TYPE, indicator);
        }
    }
    

    public static class Reconstructor extends FactoryBasedReconstructor {

		@Override
        public <T extends AviationWeatherMessage> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
                throws SerializingException {
            List<Lexeme> retval = new ArrayList<>();

           if (msg instanceof METAR) {
                TrendForecast trend = getAs(specifier, TrendForecast.class);
                if (trend != null) {
                    switch (trend.getChangeIndicator()) {
                        case BECOMING: {
                            retval.add(this.createLexeme("BECMG", TAF_FORECAST_CHANGE_INDICATOR));
                            List<Lexeme> periodOfChange = createTrendTimeChangePeriods(trend.getTimeGroups());
                            if (periodOfChange.isEmpty()) {
                                throw new SerializingException("No period of time for the trend of type BECOMING");
                            }
                            retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                            retval.addAll(periodOfChange);
                            break;
                        }
                        case TEMPORARY_FLUCTUATIONS: {
                            retval.add(this.createLexeme("TEMPO", TAF_FORECAST_CHANGE_INDICATOR));
                            List<Lexeme> periodOfChange = createTrendTimeChangePeriods(trend.getTimeGroups());
                            if (!periodOfChange.isEmpty()) {
                                retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                                retval.addAll(periodOfChange);
                            }
                            break;
                        }
                        case NO_SIGNIFICANT_CHANGES:
                            retval.add(this.createLexeme("NOSIG", TAF_FORECAST_CHANGE_INDICATOR));
                            break;
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
                        if (retval.size() > 0 && Lexeme.Identity.WHITE_SPACE != retval.get(retval.size() - 1).getIdentity()) {
                            retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                        }
                        StringBuilder ret = new StringBuilder("AT");
                        ret.append(timeGroups.getPartialStartTime());
                        retval.add(this.createLexeme(ret.toString(), TAF_FORECAST_CHANGE_INDICATOR));
                    }
                } else {
                    if (timeGroups.getPartialStartTime() != null) {
                        if (retval.size() > 0 && Lexeme.Identity.WHITE_SPACE != retval.get(retval.size() - 1).getIdentity()) {
                            retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                        }
                        StringBuilder ret = new StringBuilder("FM");
                        ret.append(timeGroups.getPartialStartTime());
                        retval.add(this.createLexeme(ret.toString(), TAF_FORECAST_CHANGE_INDICATOR));
                    }
                    if (timeGroups.getPartialEndTime() != null) {
                        if (retval.size() > 0 && Lexeme.Identity.WHITE_SPACE != retval.get(retval.size() - 1).getIdentity()) {
                            retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                        }
                        StringBuilder ret = new StringBuilder("TL");
                        ret.append(timeGroups.getPartialEndTime());
                        retval.add(this.createLexeme(ret.toString(), TAF_FORECAST_CHANGE_INDICATOR));
                    }
                }
            }
            return retval;
        }

    }

}
