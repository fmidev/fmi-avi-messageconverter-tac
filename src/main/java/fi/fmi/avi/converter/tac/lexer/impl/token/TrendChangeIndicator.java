package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.immutable.METARImpl;

/**
 * Token parser for METARImpl trend change indicator types (TEMPO, BECMG and NOSIG)
 */
public class TrendChangeIndicator extends TimeHandlingRegex {

    public enum TrendChangeIndicatorType {
        TEMPORARY_FLUCTUATIONS("TEMPO"), BECOMING("BECMG"), NO_SIGNIFICANT_CHANGES("NOSIG");

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
        super("^(NOSIG|TEMPO|BECMG)$", prio);
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
        public <T extends AviationWeatherMessage> Lexeme getAsLexeme(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
                throws SerializingException {
            Lexeme retval = null;

            if (METARImpl.class.isAssignableFrom(clz)) {
                TrendForecast trend = getAs(specifier, TrendForecast.class);
                if (trend != null) {
                    switch (trend.getChangeIndicator()) {
                        case BECOMING: {
                            retval = this.createLexeme("BECMG", TREND_CHANGE_INDICATOR);
                            break;
                        }
                        case TEMPORARY_FLUCTUATIONS: {
                            retval = this.createLexeme("TEMPO", TREND_CHANGE_INDICATOR);
                            break;
                        }
                        case NO_SIGNIFICANT_CHANGES:
                            retval = this.createLexeme("NOSIG", TREND_CHANGE_INDICATOR);
                            break;
                    }
                }
            }

            return retval;
        }

    }

}
