package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MEAN_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.ObservedSurfaceWind;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Created by rinne on 10/02/17.
 */
public class SurfaceWind extends RegexMatchingLexemeVisitor {

    public enum WindDirection {
        VARIABLE("VRB");

        private String code;

        WindDirection(final String code) {
            this.code = code;
        }

        public static WindDirection forCode(final String code) {
            for (WindDirection w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }
    }

    public SurfaceWind(final Priority prio) {
        super("^(VRB|[0-9]{3})(P?[0-9]{2,3})(GP?[0-9]{2,3})?(KT|MPS|KMH)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        boolean formatOk = true;
        int direction = -1, mean, gustValue = -1;
        boolean meanWindAbove = false;
        boolean gustAbove = false;

        String unit;
        if (!"VRB".equals(match.group(1))) {
            direction = Integer.parseInt(match.group(1));
        }
        if (match.group(2).charAt(0) == 'P') {
            mean = Integer.parseInt(match.group(2).substring(1));
            meanWindAbove = true;
        } else {
            mean = Integer.parseInt(match.group(2));
        }
        String gust = match.group(3);
        if (gust != null && 'G' == gust.charAt(0)) {
            try {
                if (gust.charAt(1) == 'P') {
                    gustValue = Integer.parseInt(gust.substring(2));
                    gustAbove = true;
                } else {
                    gustValue = Integer.parseInt(gust.substring(1));
                }
                if (gustValue < 0) {
                    formatOk = false;
                }
            } catch (NumberFormatException nfe) {
                formatOk = false;
            }
        }
        unit = match.group(4);
        if (direction > 360 || mean < 0 || unit == null) {
            formatOk = false;
        }

        if (formatOk) {
        	token.identify(SURFACE_WIND);
            if (direction == -1) {
                token.setParsedValue(DIRECTION, WindDirection.VARIABLE);
            } else {
                token.setParsedValue(DIRECTION, Integer.valueOf(direction));
            }
            token.setParsedValue(MEAN_VALUE, Integer.valueOf(mean));
            if (meanWindAbove) {
                token.setParsedValue(RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.ABOVE);
            }
            if (gustValue > -1) {
                token.setParsedValue(MAX_VALUE, Integer.valueOf(gustValue));
            }
            if (gustAbove) {
                token.setParsedValue(RELATIONAL_OPERATOR2, AviationCodeListUser.RelationalOperator.ABOVE);
            }
            if ("KT".equalsIgnoreCase(unit)) {
                unit = "[kn_i]";
            } else if ("MPS".equalsIgnoreCase(unit)) {
                unit = "m/s";
            }
            token.setParsedValue(UNIT, unit.toLowerCase());
        } else {
            token.identify(SURFACE_WIND, Lexeme.Status.SYNTAX_ERROR, "Wind direction or speed values invalid");
        }
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

		@Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (TAF.class.isAssignableFrom(clz)) {
                Optional<TAFBaseForecast> base = ctx.getParameter("forecast", TAFBaseForecast.class);
                Optional<TAFChangeForecast> change = ctx.getParameter("forecast", TAFChangeForecast.class);
                Optional<fi.fmi.avi.model.SurfaceWind> wind = Optional.empty();
                if (base.isPresent()) {
                     wind = base.get().getSurfaceWind();
                    if (!wind.isPresent()) {
                        throw new SerializingException("Surface wind missing from TAF base forecast");
                    }
                } else if (change.isPresent()) {
                    wind = change.get().getSurfaceWind();
                }
                if (wind.isPresent()) {
                    return getForecastSurfaceWindLexeme(wind.get());
                }

            } else if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
                if (trend.isPresent()) {
                    Optional<fi.fmi.avi.model.SurfaceWind> wind = trend.get().getSurfaceWind();
                    if (wind.isPresent()) {
                        return getForecastSurfaceWindLexeme(wind.get());
                    }
                } else {
                    Optional<ObservedSurfaceWind> wind = ((MeteorologicalTerminalAirReport)msg).getSurfaceWind();
                    if (wind.isPresent()) {
                        return getForecastSurfaceWindLexeme(wind.get());
                    }
                }
            }
            return Optional.empty();
        }

        private <S extends fi.fmi.avi.model.SurfaceWind> Optional<Lexeme> getForecastSurfaceWindLexeme(final S wind) throws SerializingException {
            StringBuilder builder = new StringBuilder();
            if (wind.isVariableDirection()) {
                builder.append("VRB");
            } else if (wind.getMeanWindDirection().isPresent()) {
                if (!wind.getMeanWindDirection().get().getUom().equals("deg")) {
                    throw new SerializingException("Mean wind direction unit is not 'deg': " + wind.getMeanWindDirection().get().getUom());
                } else {
                    builder.append(String.format("%03d", wind.getMeanWindDirection().get().getValue().intValue()));
                }
            } else {
                throw new SerializingException("Mean wind direction must be set if variable wind direction is false");
            }
            this.appendCommonWindParameters(builder, wind.getMeanWindSpeed(), wind.getMeanWindSpeedOperator().orElse(null), wind.getWindGust().orElse(null),
                    wind.getWindGustOperator().orElse(null));
            return Optional.of(this.createLexeme(builder.toString(), Lexeme.Identity.SURFACE_WIND));
        }

        private void appendCommonWindParameters(StringBuilder builder, NumericMeasure meanSpeed, AviationCodeListUser.RelationalOperator meanSpeedOperator,
                NumericMeasure gustSpeed, AviationCodeListUser.RelationalOperator gustOperator)
                throws SerializingException {
            int speed = meanSpeed.getValue().intValue();
            if (meanSpeedOperator != null && AviationCodeListUser.RelationalOperator.ABOVE == meanSpeedOperator) {
                builder.append('P');
            }
            appendSpeed(builder, speed);

            if (gustSpeed != null) {
                if (!gustSpeed.getUom().equals(gustSpeed.getUom())) {
                    throw new SerializingException(
                            "Wind gust speed unit '" + gustSpeed.getUom() + "' is not the same as mean wind speed unit '" + meanSpeed.getUom() + "'");
                }
                builder.append('G');
                if (gustOperator != null && AviationCodeListUser.RelationalOperator.ABOVE == gustOperator) {
                    builder.append('P');
                }
                appendSpeed(builder, gustSpeed.getValue().intValue());
            }

            String uom = meanSpeed.getUom();
            if ("[kn_i]".equals(uom)) {
                uom = "KT";
            } else if ("m/s".equals(uom)) {
                uom = "MPS";
            } else {
                uom = uom.toUpperCase();
            }
            builder.append(uom);
        }

        private void appendSpeed(StringBuilder builder, int speed) throws SerializingException {
            if (speed < 0 || speed >= 1000) {
                throw new SerializingException("Wind speed value " + speed + " is not withing acceptable range [0,1000]");
            }
			builder.append(String.format("%02d", speed));
		}
	}
}
