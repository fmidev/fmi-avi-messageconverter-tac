package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIR_PRESSURE_QNH;

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
public class AtmosphericPressureQNH extends RegexMatchingLexemeVisitor {

    public AtmosphericPressureQNH(final OccurrenceFrequency prio) {
        super("^([AQ])([0-9]{4}|////)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final PressureMeasurementUnit unit = PressureMeasurementUnit.forCode(match.group(1));
        Integer value = null;
        if (!"////".equals(match.group(2))) {
            value = Integer.valueOf(match.group(2));
        }
        if (value != null) {
            token.identify(AIR_PRESSURE_QNH);
            token.setParsedValue(UNIT, unit);
            token.setParsedValue(VALUE, value);
        } else {
            token.identify(AIR_PRESSURE_QNH, Lexeme.Status.WARNING, "Missing value for air pressure");
        }
    }

    public enum PressureMeasurementUnit {
        HECTOPASCAL("Q"), INCHES_OF_MERCURY("A");

        private final String code;

        PressureMeasurementUnit(final String code) {
            this.code = code;
        }

        public static PressureMeasurementUnit forCode(final String code) {
            for (final PressureMeasurementUnit w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            final NumericMeasure altimeter;

            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                final Optional<NumericMeasure> qnh = ((MeteorologicalTerminalAirReport) msg).getAltimeterSettingQNH();
                if (qnh.isPresent()) {
                    altimeter = qnh.get();
                    if (altimeter.getValue() == null) {
                        throw new SerializingException("AltimeterSettingQNH is missing the value");
                    }

                    final String unit;
                    if ("hPa".equals(altimeter.getUom())) {
                        unit = "Q";
                    } else if ("in Hg".equals(altimeter.getUom())) {
                        unit = "A";
                    } else {
                        throw new SerializingException("Unknown unit of measure in AltimeterSettingQNH '" + altimeter.getUom() + "'");
                    }

                    final String content = unit + String.format("%04d", altimeter.getValue().intValue());
                    retval = Optional.of(this.createLexeme(content, LexemeIdentity.AIR_DEWPOINT_TEMPERATURE));

                }
            }

            return retval;
        }
    }
}
