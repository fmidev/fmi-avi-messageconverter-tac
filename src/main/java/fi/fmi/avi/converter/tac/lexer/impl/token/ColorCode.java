package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.COLOR_CODE;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.TrendForecast;

/**
 * Created by rinne on 10/02/17.
 */
public class ColorCode extends RegexMatchingLexemeVisitor {

    public ColorCode(final OccurrenceFrequency prio) {
        super("^(BLU|WHT|YLO1|YLO2|AMB|RED)|(BLACK(BLU|WHT|YLO1|YLO2|AMB|RED)?)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final ColorState state;
        if (match.group(2) == null) {
            state = ColorState.forCode(match.group(1));
        } else {
            state = ColorState.forCode(match.group(2));
        }
        token.identify(COLOR_CODE);
        token.setParsedValue(VALUE, state);
    }

    public enum ColorState {
        BLUE("BLU"), WHITE("WHT"), GREEN("GRN"), YELLOW1("YLO1"), YELLOW2("YLO2"), AMBER("AMB"), RED("RED"), BLACK("BLACK"),
        BLACK_BLUE("BLACKBLU"),
        BLACK_WHITE("BLACKWHT"),
        BLACK_GREEN("BLACKGRN"),
        BLACK_YELLOW1("BLACKYLO1"),
        BLACK_YELLOW2("BLACKYLO2"),
        BLACK_AMBER("BLACKAMB"),
        BLACK_RED("BLACKRED");

        private final String code;

        ColorState(final String code) {
            this.code = code;
        }

        public static ColorState forCode(final String code) {
            for (final ColorState w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

        public String getCode() {
            return this.code;
        }

    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {

            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                final MeteorologicalTerminalAirReport metar = (MeteorologicalTerminalAirReport) msg;
                final Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
                final Optional<fi.fmi.avi.model.AviationCodeListUser.ColorState> color = trend.map(TrendForecast::getColorState).orElse(metar.getColorState());
                if (color.isPresent()) {
                    return Optional.of(this.createLexeme(color.get().name(), COLOR_CODE));
                }
            }
            return Optional.empty();
        }

    }
}
