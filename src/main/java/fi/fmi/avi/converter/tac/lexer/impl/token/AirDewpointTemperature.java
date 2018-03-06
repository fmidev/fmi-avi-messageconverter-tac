package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.METAR;

/**
 * Created by rinne on 10/02/17.
 */
public class AirDewpointTemperature extends RegexMatchingLexemeVisitor {

    public AirDewpointTemperature(final Priority prio) {
        super("^(M)?([0-9]{2}|//)/(M)?([0-9]{2}|//)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        Double airTemp = null;
        Double dewPointTemp = null;
        if (!"//".equals(match.group(2))) {
            airTemp = Double.valueOf(match.group(2));
        }
        if (!"//".equals(match.group(4))) {
            dewPointTemp = Double.valueOf(match.group(4));
        }
        Double[] values = new Double[2];
        boolean missingValues = false;
        if (airTemp != null) {
            if (match.group(1) != null) {
                if (1.0d/airTemp.doubleValue() == Double.POSITIVE_INFINITY) {
                    airTemp = -0.0d; //explicit value required, 0.0d != -0.0d
                } else {
                    airTemp = Double.valueOf(airTemp.intValue() * -1);
                }
            }
            values[0] = airTemp;
        } else {
            missingValues = true;
        }
        if (dewPointTemp != null) {
            if (match.group(3) != null) {
                if (1.0d/dewPointTemp.doubleValue() == Double.POSITIVE_INFINITY) {
                    dewPointTemp = -0.0d;
                } else {
                    dewPointTemp = Double.valueOf(dewPointTemp.intValue() * -1);
                }
            }
            values[1] = dewPointTemp;
        } else {
            missingValues = true;
        }
        
        if (missingValues) {
            token.identify(AIR_DEWPOINT_TEMPERATURE, Lexeme.Status.WARNING, "Values for air and/or dew point temperature missing");
        } else {
            token.identify(AIR_DEWPOINT_TEMPERATURE);
            token.setParsedValue(VALUE, values);
            token.setParsedValue(UNIT, "degC");
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessage> Lexeme getAsLexeme(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
                throws SerializingException {
            Lexeme retval = null;

            NumericMeasure air = null;
            NumericMeasure dew = null;

            if (METAR.class.isAssignableFrom(clz)) {

                METAR metar = (METAR) msg;

                air = metar.getAirTemperature();
                dew = metar.getDewpointTemperature();

            }

            if (air != null && dew != null) {
                if (air.getValue() == null) {
                    throw new SerializingException("AirTemperature exists, but no value");
                }

                if (dew.getValue() == null) {
                    throw new SerializingException("DewpointTemperature exists, but no value");
                }

                if (!"degC".equals(air.getUom())) {
                    throw new SerializingException("AirTemperature unit of measure is not degC, but '" + air.getUom() + "'");
                }

                if (!"degC".equals(dew.getUom())) {
                    throw new SerializingException("DewpointTemperature unit of measure is not degC, but '" + dew.getUom() + "'");
                }

                StringBuilder builder = new StringBuilder();

                appendValue(air.getValue(), builder);
                builder.append("/");
                appendValue(dew.getValue(), builder);

                retval = this.createLexeme(builder.toString(), Identity.AIR_DEWPOINT_TEMPERATURE);
            }

            return retval;
        }

        private void appendValue(Double v, StringBuilder builder) {
            if (v < 0.0 || 1.0d/v == Double.NEGATIVE_INFINITY) {
                builder.append("M");
            }
            builder.append(String.format("%02d",  Math.round(Math.abs(v))));
        }
    }
}
