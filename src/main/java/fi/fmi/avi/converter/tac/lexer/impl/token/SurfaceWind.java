package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MEAN_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.ObservedSurfaceWind;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.TrendForecastSurfaceWind;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFSurfaceWind;

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
        super("^(VRB|[0-9]{3})([0-9]{2})(G[0-9]{2})?(KT|MPS|KMH)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        boolean formatOk = true;
        int direction = -1, mean, gustValue = -1;
        String unit;
        if (!"VRB".equals(match.group(1))) {
            direction = Integer.parseInt(match.group(1));
        }
        mean = Integer.parseInt(match.group(2));
        String gust = match.group(3);
        if (gust != null && 'G' == gust.charAt(0)) {
            try {
                gustValue = Integer.parseInt(gust.substring(1));
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
            if (gustValue > -1) {
                token.setParsedValue(MAX_VALUE, Integer.valueOf(gustValue));
            }
            if ("KT".equalsIgnoreCase(unit)) {
                unit = "[kn_i]";
            }
            token.setParsedValue(UNIT, unit.toLowerCase());
        } else {
            token.identify(SURFACE_WIND, Lexeme.Status.SYNTAX_ERROR, "Wind direction or speed values invalid");
        }
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

		@Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (TAF.class.isAssignableFrom(clz)) {
                Optional<TAFBaseForecast> base = ctx.getParameter("forecast", TAFBaseForecast.class);
                Optional<TAFChangeForecast> change = ctx.getParameter("forecast", TAFChangeForecast.class);
                Optional<TAFSurfaceWind> wind = Optional.empty();
                if (base.isPresent()) {
                     wind = base.get().getSurfaceWind();
                    if (!wind.isPresent()) {
                        throw new SerializingException("Surface wind missing from TAF base forecast");
                    }
                } else if (change.isPresent()) {
                    wind = change.get().getSurfaceWind();
                }
                if (wind.isPresent()) {
                    StringBuilder builder = new StringBuilder();
                    if (wind.get().isVariableDirection()) {
                        builder.append("VRB");
                    } else if (wind.get().getMeanWindDirection().isPresent()) {
                        if (!wind.get().getMeanWindDirection().get().getUom().equals("deg")) {
                            throw new SerializingException("Mean wind direction unit is not 'deg': " + wind.get().getMeanWindDirection().get().getUom());
                        } else {
                            builder.append(String.format("%03d", wind.get().getMeanWindDirection().get().getValue().intValue()));
                        }
                    }
                    this.appendCommonWindParameters(builder, wind.get().getMeanWindSpeed(), wind.get().getWindGust().orElse(null));
                    retval = Optional.of(this.createLexeme(builder.toString(), Lexeme.Identity.SURFACE_WIND));
                }

            } else if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
                String tokenStr = null;
                if (trend.isPresent()) {
                    Optional<TrendForecastSurfaceWind> wind = trend.get().getSurfaceWind();
                    if (wind.isPresent()) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(String.format("%03d", wind.get().getMeanWindDirection().getValue().intValue()));
                        this.appendCommonWindParameters(builder, wind.get().getMeanWindSpeed(), wind.get().getWindGust().orElse(null));
                        tokenStr = builder.toString();
                    }
                } else {
                    Optional<ObservedSurfaceWind> wind = ((MeteorologicalTerminalAirReport)msg).getSurfaceWind();
                    if (wind.isPresent()) {
                        StringBuilder builder = new StringBuilder();
                        if (wind.get().isVariableDirection()) {
                            builder.append("VRB");
                        } else if (wind.get().getMeanWindDirection().isPresent()) {
                            if (!wind.get().getMeanWindDirection().get().getUom().equals("deg")) {
                                throw new SerializingException("Mean wind direction unit is not 'deg': " + wind.get().getMeanWindDirection().get().getUom());
                            } else {
                                builder.append(String.format("%03d", wind.get().getMeanWindDirection().get().getValue().intValue()));
                            }
                        } else {
                            throw new SerializingException("Mean wind direction must be set if variable wind direction is false");
                        }
                        this.appendCommonWindParameters(builder, wind.get().getMeanWindSpeed(), wind.get().getWindGust().orElse(null));
                        tokenStr = builder.toString();
                    }
                }
                
                if (tokenStr != null) {
                    retval = Optional.of(this.createLexeme(tokenStr, Lexeme.Identity.SURFACE_WIND));
                }
            }

			return retval;
		}

        private void appendCommonWindParameters(StringBuilder builder, NumericMeasure meanSpeed, NumericMeasure gustSpeed)
                throws SerializingException {
            int speed = meanSpeed.getValue().intValue();
            appendSpeed(builder, speed);

            if (gustSpeed != null) {
                if (!gustSpeed.getUom().equals(gustSpeed.getUom())) {
                    throw new SerializingException(
                            "Wind gust speed unit '" + gustSpeed.getUom() + "' is not the same as mean wind speed unit '" + meanSpeed.getUom() + "'");
                }
                builder.append("G");
                appendSpeed(builder, gustSpeed.getValue().intValue());
            }

            String uom = meanSpeed.getUom();
            if ("[kn_i]".equals(uom)) {
                uom = "KT";
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
