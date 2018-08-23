package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TREND_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.TrendForecast;

/**
 * Token parser for METAR trend change indicator types (TEMPO, BECMG and NOSIG)
 */
public class TrendChangeIndicator extends TimeHandlingRegex {

    public enum TrendChangeIndicatorType {
        TEMPORARY_FLUCTUATIONS("TEMPO"), BECOMING("BECMG");

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
        super("^(TEMPO|BECMG)$", prio);
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
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
            if (trend.isPresent()) {
                switch (trend.get().getChangeIndicator()) {
                    case BECOMING: {
                        return Optional.of(this.createLexeme("BECMG", TREND_CHANGE_INDICATOR));
                    }
                    case TEMPORARY_FLUCTUATIONS: {
                        return Optional.of(this.createLexeme("TEMPO", TREND_CHANGE_INDICATOR));
                    }
                }
            }
            return Optional.empty();
        }

    }

}
