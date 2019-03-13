package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VARIABLE_WIND_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MIN_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.ObservedSurfaceWind;

/**
 * Created by rinne on 10/02/17.
 *
 * NOTE: Occurring of multiple VariableSurfaceWinds is not checked
 */
public class VariableSurfaceWind extends RegexMatchingLexemeVisitor {

    public VariableSurfaceWind(final Priority prio) {
        super("^([0-9]{3})V([0-9]{3})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        boolean formatOk = true;
        final int minDirection;
        final int maxDirection;
        minDirection = Integer.parseInt(match.group(1));
        maxDirection = Integer.parseInt(match.group(2));
        if (minDirection < 0 || minDirection > 360 || maxDirection < 0 || maxDirection > 360) {
            formatOk = false;
        }
        if (formatOk) {
            token.identify(VARIABLE_WIND_DIRECTION);
            token.setParsedValue(MIN_DIRECTION, minDirection);
            token.setParsedValue(MAX_DIRECTION, maxDirection);
            token.setParsedValue(UNIT, "deg");
        } else {
            token.identify(VARIABLE_WIND_DIRECTION, Lexeme.Status.SYNTAX_ERROR, "Wind directions invalid");
        }
    }
    
    
    public static class Reconstructor extends FactoryBasedReconstructor {
    	@Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {

            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                final MeteorologicalTerminalAirReport m = (MeteorologicalTerminalAirReport) msg;
                final Optional<ObservedSurfaceWind> wind = m.getSurfaceWind();

                if (wind.isPresent()) {
                    final Optional<NumericMeasure> clockwise = wind.get().getExtremeClockwiseWindDirection();
                    final Optional<NumericMeasure> counter = wind.get().getExtremeCounterClockwiseWindDirection();
                    if (clockwise.isPresent() && counter.isPresent()) {
                        return Optional.of(this.createLexeme(createString(clockwise.get(), counter.get()), VARIABLE_WIND_DIRECTION));
                    }
                }
            }
            return Optional.empty();
        }

        private String createString(final NumericMeasure clockwise, final NumericMeasure counter) throws SerializingException
		{
			// Both must be set
			if (clockwise == null || counter == null) {
        		throw new SerializingException("Only either extreme clockwise or counter-clocwise wind direction given. Unable to serialize token");
        	}
			
			if (!"deg".equals(counter.getUom())) {
				throw new SerializingException("Counter-clockwise extreme wind direction is not in degress (but in '"+counter.getUom()+"'), unable to serialize");
			}

			if (!"deg".equals(clockwise.getUom())) {
                throw new SerializingException("Clockwise extreme wind direction is not in degrees (but in '" + clockwise.getUom() + "'), unable to serialize");
            }

			if (counter.getValue() < 0.0 || counter.getValue() > 360.0) {
				throw new SerializingException("Illegal counter-clockwise extreme wind direction "+counter.getValue()+" "+counter.getUom());
			}

			if (clockwise.getValue() < 0.0 || clockwise.getValue() > (360.0d + Double.MIN_VALUE)) {
				throw new SerializingException("Illegal clockwise extreme wind direction "+clockwise.getValue()+" "+clockwise.getUom());
			}

			
			return String.format("%03dV%03d", counter.getValue().intValue(), clockwise.getValue().intValue());
		}
    }
}
