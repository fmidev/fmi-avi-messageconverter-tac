package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_DEWPOINT_TEMPERATURE;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;

/**
 * Created by rinne on 10/02/17.
 */
public class AirDewpointTemperature extends RegexMatchingLexemeVisitor {

    public AirDewpointTemperature(final OccurrenceFrequency prio) {
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
        final Double[] values = new Double[2];
        boolean missingValues = false;
        if (airTemp != null) {
            if (match.group(1) != null) {
                if (1.0d / airTemp == Double.POSITIVE_INFINITY) {
                    airTemp = -0.0d; //explicit value required, 0.0d != -0.0d
                } else {
                    airTemp = (double) (airTemp.intValue() * -1);
                }
            }
            values[0] = airTemp;
        } else {
            missingValues = true;
        }
        if (dewPointTemp != null) {
            if (match.group(3) != null) {
                if (1.0d / dewPointTemp == Double.POSITIVE_INFINITY) {
                    dewPointTemp = -0.0d;
                } else {
                    dewPointTemp = (double) (dewPointTemp.intValue() * -1);
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
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                final Optional<NumericMeasure> airTemp = ((MeteorologicalTerminalAirReport) msg).getAirTemperature();
                final Optional<NumericMeasure> dewpointTemp = ((MeteorologicalTerminalAirReport) msg).getDewpointTemperature();
                if (airTemp.isPresent() && dewpointTemp.isPresent()) {
                    final NumericMeasure air = airTemp.get();
                    final NumericMeasure dew = dewpointTemp.get();

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

                    final StringBuilder builder = new StringBuilder();

                    appendValue(air.getValue(), builder);
                    builder.append("/");
                    appendValue(dew.getValue(), builder);

                    retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.AIR_DEWPOINT_TEMPERATURE));
                }
            }

            return retval;
        }

        private void appendValue(final Double v, final StringBuilder builder) {
            if (v < 0.0 || 1.0d / v == Double.NEGATIVE_INFINITY) {
                builder.append("M");
            }
            builder.append(String.format(Locale.US, "%02d", Math.round(Math.abs(v))));
        }
    }
}
