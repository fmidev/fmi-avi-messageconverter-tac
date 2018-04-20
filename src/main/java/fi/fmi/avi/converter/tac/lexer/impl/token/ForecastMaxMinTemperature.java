package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.MAX_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.MIN_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;

/**
 * Created by rinne on 10/02/17.
 */
public class ForecastMaxMinTemperature extends TimeHandlingRegex {

    public enum TemperatureForecastType {
        MINIMUM("TN"), MAXIMUM("TX");

        private final String code;

        TemperatureForecastType(final String code) {
            this.code = code;
        }

        public static TemperatureForecastType forCode(final String code) {
            for (TemperatureForecastType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }

    public ForecastMaxMinTemperature(final Priority prio) {
        super("^(TX|TN)(M)?([0-9]{2})/([0-9]{2})?([0-9]{2})(Z)?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        TemperatureForecastType kind = TemperatureForecastType.forCode(match.group(1));
        boolean isNegative = match.group(2) != null;
        double value = Integer.parseInt(match.group(3));
        if (isNegative) {
            if (1.0d/value == Double.POSITIVE_INFINITY) {
                value = -0.0d;
            } else {
                value = value * -1;
            }
        }
        int day = -1;
        if (match.group(3) != null) {
            day = Integer.parseInt(match.group(4));
        }
        int hour = Integer.parseInt(match.group(5));

        Lexeme.Identity kindLexemeIdentity;
        if (TemperatureForecastType.MAXIMUM == kind) {
            kindLexemeIdentity = MAX_TEMPERATURE;
        } else {
            kindLexemeIdentity = MIN_TEMPERATURE;
        }
        
        if (timeOkDayHour(day, hour)) {
            if (hints != null && ConversionHints.VALUE_TIMEZONE_ID_POLICY_STRICT == hints.get(ConversionHints.KEY_TIMEZONE_ID_POLICY)) {
                if (match.group(6) == null) {
                	token.identify(kindLexemeIdentity,Lexeme.Status.WARNING,"Missing time zone ID 'Z'");
                } else {
                	token.identify(kindLexemeIdentity);
                }
            } else {
            	token.identify(kindLexemeIdentity);
            }
        	
            if (day > -1) {
                token.setParsedValue(DAY1, day);
            }
            token.setParsedValue(HOUR1, hour);
            token.setParsedValue(VALUE, value);
        } else {
            token.identify(kindLexemeIdentity,Lexeme.Status.SYNTAX_ERROR,"Invalid day/hour values");
        }
        
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
    	@Override
        public <T extends AviationWeatherMessage> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
                throws SerializingException {
            List<Lexeme> retval = new ArrayList<>();
    		
    		if (TAF.class.isAssignableFrom(clz)) {
                Optional<TAFBaseForecast> forecast = getAs(specifier, TAFBaseForecast.class);
                if (forecast.isPresent() && forecast.get().getTemperatures().isPresent()) {
                    for (TAFAirTemperatureForecast temp : forecast.get().getTemperatures().get()) {

                        if (!"degC".equals(temp.getMaxTemperature().getUom())) {
                            throw new SerializingException(
                                    "Unsupported unit of measurement for maximum temperature: '" + temp.getMaxTemperature().getUom() + "'");
                        }
                        retval.add(this.createLexeme(formatTemp("TX", temp.getMaxTemperature(), temp.getMaxTemperatureTime()), MAX_TEMPERATURE));

                        if (!"degC".equals(temp.getMinTemperature().getUom())) {
                            throw new SerializingException(
                                    "Unsupported unit of measurement for minimum temperature: '" + temp.getMinTemperature().getUom() + "'");
                        }
                        retval.add(this.createLexeme(" ", Lexeme.Identity.WHITE_SPACE));
                        retval.add(this.createLexeme(formatTemp("TN", temp.getMinTemperature(), temp.getMinTemperatureTime()), MIN_TEMPERATURE));
                    }
                }
            }
            return retval;
    	}

        public String formatTemp(String prefix, NumericMeasure temp, PartialOrCompleteTimeInstant time) {
            StringBuilder sb = new StringBuilder(prefix);
            if (temp.getValue() < 0.0 || 1.0d / temp.getValue() == Double.NEGATIVE_INFINITY) {
                sb.append('M');
            }
            sb.append(String.format("%02d", Math.round(Math.abs(temp.getValue()))));
            sb.append('/');
            sb.append(String.format("%02d", time.getDay()));
            sb.append(String.format("%02d", time.getHour()));
            sb.append('Z');
    		return sb.toString();
    	}
    }
}
