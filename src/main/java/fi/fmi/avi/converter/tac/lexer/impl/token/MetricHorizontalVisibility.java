package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Status;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser.RelationalOperator;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.HorizontalVisibility;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Token parser for horizontal visibility given in meters.
 */
public class MetricHorizontalVisibility extends RegexMatchingLexemeVisitor {

	public static final int MAX_STATUE_MILE_DENOMINATOR = 16;
	
	public enum DirectionValue {
		NORTH("N", 0),
		SOUTH("S", 180),
		EAST("E", 90),
		WEST("W", 270),
		NORTH_EAST("NE", 45),
		NORTH_WEST("NW", 315),
		SOUTH_EAST("SE", 135),
		SOUTH_WEST("SW", 225),
        NO_DIRECTIONAL_VARIATION("NDV",-1);

        private String code;
        private int deg;

        DirectionValue(final String code, final int deg) {
            this.code = code;
            this.deg = deg;
        }

        public static DirectionValue forCode(final String code) {
            for (DirectionValue w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }
        
        public int inDegrees() {
        	return this.deg;
        }

    }
	
    public MetricHorizontalVisibility(final Priority prio) {
        super("^([0-9]{4})([A-Z]{1,2}|NDV)?$", prio);
    }

    @Override
	public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
		String direction = match.group(2);
		double certainty = 0.5;
    	//This is a tricky one, we need to separate the nnnn visibility from a nnnn change group validity time
    	Lexeme l = token.getPrevious();
    	if (l == null) {
    		//Horizontal visibility cannot be the first token:
    		return;
    	}
    	boolean inChangeGroup = false;
    	while (l != null) {
			if (Identity.TAF_FORECAST_CHANGE_INDICATOR == l.getIdentity()) {
				inChangeGroup = true;
				break;
			}
			l = l.getPrevious();
		}
		if (!inChangeGroup) {
			Lexeme prev = token.getPrevious();
    		if (Identity.SURFACE_WIND == prev.getIdentity() || Identity.VARIABLE_WIND_DIRECTION == prev.getIdentity() || Identity.HORIZONTAL_VISIBILITY == prev.getIdentity()) {
    			certainty = 1.0;
    		}
    	} 
    	else {
    		if (Lexeme.Identity.TAF_START == token.getFirst().getIdentity()) {
	    		if (direction == null) {
	    			int startHour = Integer.parseInt(match.group(1).substring(0, 2));
	    			int endHour = Integer.parseInt(match.group(1).substring(3, 4));
	    			if ((startHour <= 24) && (endHour <= 24)) {
	    				boolean hasAnotherVisibility = false;
	    				//we start with the FORECAST_CHANGE_INDICATOR, so skip it:
	    				l = l.getNext();
						while (l != null && Identity.END_TOKEN != l.getIdentity() && Identity.TAF_FORECAST_CHANGE_INDICATOR != l.getIdentity()) {
							if (Identity.HORIZONTAL_VISIBILITY == l.getIdentity() && l != token) {
								hasAnotherVisibility = true;
								break;
							}
							l = l.getNext();
						}
						if (hasAnotherVisibility) {
	    					if (Identity.HORIZONTAL_VISIBILITY == token.getIdentity()) {
	    	    				token.identify(null, Status.UNRECOGNIZED);
	    	    			}
	    					return;
	    				}
	    			} else {
	    				certainty = 1.0;
	    			}
	    		}
    		}
    	}
    	
    	int visibility = Integer.parseInt(match.group(1));
		if (direction != null) {
        	DirectionValue dv = DirectionValue.forCode(direction);
        	certainty = 1.0;
        	if (dv == null) {
        		token.identify(HORIZONTAL_VISIBILITY, Status.SYNTAX_ERROR, "Invalid visibility direction value '" + direction + "'", certainty);
        	} else {
        		token.identify(HORIZONTAL_VISIBILITY, certainty); 
        		token.setParsedValue(DIRECTION, dv);
        	}
        } else {
        	token.identify(HORIZONTAL_VISIBILITY, certainty);
        }

        token.setParsedValue(UNIT, "m");
       
        if (visibility == 9999) {
            token.setParsedValue(VALUE, Double.valueOf(10000d));
            token.setParsedValue(RELATIONAL_OPERATOR, RecognizingAviMessageTokenLexer.RelationalOperator.MORE_THAN);
        } else if (visibility == 0) {
            token.setParsedValue(VALUE, Double.valueOf(50d));
            token.setParsedValue(RELATIONAL_OPERATOR, RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN);
        } else {
            token.setParsedValue(VALUE, Double.valueOf(visibility));
        }
        
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

		@Override
		public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
			List<Lexeme> retval = new ArrayList<>();

            Optional<NumericMeasure> visibility = Optional.empty();
            Optional<RelationalOperator> operator = Optional.empty();

            Optional<NumericMeasure> minimumVisibilityDistance = Optional.empty();
            Optional<NumericMeasure> minimumVisibilityDirection = Optional.empty();

            Optional<TAFBaseForecast> base = ctx.getParameter("forecast", TAFBaseForecast.class);
            if (base.isPresent()) {
                visibility = base.get().getPrevailingVisibility();
                operator = base.get().getPrevailingVisibilityOperator();
            } else {
                Optional<TAFChangeForecast> change = ctx.getParameter("forecast", TAFChangeForecast.class);
                if (change.isPresent()) {
                    visibility = change.get().getPrevailingVisibility();
                    operator = change.get().getPrevailingVisibilityOperator();
                } else {
                    Optional<TrendForecast> metarTrend = ctx.getParameter("trend", TrendForecast.class);
                    if (metarTrend.isPresent()) {
                        visibility = metarTrend.get().getPrevailingVisibility();
                        operator = metarTrend.get().getPrevailingVisibilityOperator();
                    } else if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                        Optional<HorizontalVisibility> vis = ((MeteorologicalTerminalAirReport)msg).getVisibility();
                        if (vis.isPresent()) {
                            visibility = Optional.of(vis.get().getPrevailingVisibility());
                            operator = vis.get().getPrevailingVisibilityOperator();
                            minimumVisibilityDistance = vis.get().getMinimumVisibility();
                            minimumVisibilityDirection = vis.get().getMinimumVisibilityDirection();
                        }
                    }
                }
			}

            if (visibility.isPresent()) {
                String str;

                if ("m".equals(visibility.get().getUom())) {
                    str = createMetricIntegerVisibility(visibility.get(), operator);
                } else if ("sm".equals(visibility.get().getUom())) {
                    str = createStatuteMilesVisibility(visibility.get(), operator);
                } else {
                    throw new SerializingException("Unknown unit of measure '" + visibility.get().getUom() + "' for visibility");
                }
				
				retval.add(this.createLexeme(str, Lexeme.Identity.HORIZONTAL_VISIBILITY));

                if (minimumVisibilityDistance.isPresent() && minimumVisibilityDirection.isPresent()) {
                    String tmp = createMinimumVisibilityString(minimumVisibilityDistance.get(), minimumVisibilityDirection.get());
                    retval.add(createLexeme(" ", Identity.WHITE_SPACE));
					retval.add(this.createLexeme(tmp, Lexeme.Identity.HORIZONTAL_VISIBILITY));
				}
			}
			return retval;
		}

		private String createMinimumVisibilityString(NumericMeasure distance, NumericMeasure direction) throws SerializingException {
			if (distance == null || direction == null) {
				throw new SerializingException("Both visibility and direction need to be set for minimum visibility. Cannot serialize");
			}
			
			if (!"deg".equals(direction.getUom())) {
				throw new SerializingException("Minimum visibility direction must be in degrees, but unit is "+direction.getUom()+" instead");
			}
			
			if (!"m".equals(distance.getUom())) {
				throw new SerializingException("Minimum visibility distance must be in meters, but unit is "+distance.getUom()+" instead");
			}
			
			int meters = distance.getValue().intValue();
			if (meters < 0 || meters >= 10000) {
				throw new SerializingException("Minimum visibility distance must be 0 to 9999 meters, but is "+distance.getValue());
			}
			
			// Allow 5 degrees slack, so 40-50 deg => 45 == NE
			final int slack = 5;
			
			int deg = direction.getValue().intValue();
			if (deg < 0 || deg > 360) {
				throw new SerializingException("Minimum visibilty direction must be within 0...360");
			}
			if (deg >= (360-slack)) {
				deg -= 360;
			}
			
			String compass;
			if (deg <= 0 + slack) {
				compass = "N";
			} else if (deg >= 45-slack && deg <= 45+slack) {
				compass = "NE";
			} else if (deg >= 90-slack && deg <= 90+slack) {
				compass = "E";
			} else if (deg >= 135-slack && deg <= 135+slack) {
				compass = "SE";
			} else if (deg >= 180-slack && deg <= 180+slack) {
				compass = "S";
			} else if (deg >= 225-slack && deg <= 225+slack) {
				compass = "SW";
			} else if (deg >= 270-slack && deg <= 270+slack) {
				compass = "W";
			} else if (deg >= 315-slack && deg <= 315+slack) {
				compass = "NW";
			} else {
				throw new SerializingException("Minimum visibility direction ("+direction.getValue()+") is not within "+slack+" degrees of a cardinal or intercardinal direction");
			}
			
			return String.format("%04d%s", meters, compass);
		}

        private String createMetricIntegerVisibility(NumericMeasure visibility, Optional<RelationalOperator> operator) throws SerializingException {
            String str;

			int meters = visibility.getValue().intValue();
			if (meters < 0) {
				throw new SerializingException("Visibility " + meters + " must be positive");
			}

            if (operator.isPresent() && operator.get() == RelationalOperator.BELOW && meters <= 50) {
                str = "0000";
            } else if (operator.isPresent() && operator.get() == RelationalOperator.ABOVE && meters >= 9999) {
                str = "9999";
			} else {
				str = String.format("%04d", meters);
			}

			return str;
		}

        private String createStatuteMilesVisibility(NumericMeasure visibility, Optional<RelationalOperator> operator) throws SerializingException {
            StringBuilder builder = new StringBuilder();

            if (operator.isPresent() && operator.get() == RelationalOperator.ABOVE) {
                builder.append("P");
            } else if (operator.isPresent() && operator.get() == RelationalOperator.BELOW) {
                builder.append("M");
            }
            
			int integerPart = (int)Math.floor(visibility.getValue());
			
			double parts = visibility.getValue() - (double)integerPart;
			
			if (parts > 1.0/(double)16) {
				if (integerPart > 0) {
					builder.append(String.format("%d ", integerPart));
				}

				builder.append(findClosestFraction(parts, 16));
			} else {
				builder.append(String.format("%d", integerPart));
			}

			builder.append("SM");

			return builder.toString();
		}

		static String findClosestFraction(final double number, final int maxDenominator) {
			if (maxDenominator < 3) {
				throw new IllegalArgumentException("max denominator should be at least 3 to make any sense, you gave me " + maxDenominator);
			}

			if (number >= 1.0 || number <= 0.0) {
				throw new IllegalArgumentException("it only makes sense to find fractions for numbers between 0 and 1 (exclusive)");
			}

			Integer currentBestNumerator = null;
			Integer currentBestDenominator = null;
			Double currentBestDelta = null;

			double doubleEquivalencyFactor = 0.00000001d;

			for (int denominator = 2; denominator <= maxDenominator; denominator++) {

				for (int numerator = 1; numerator < denominator; numerator++) {
					double delta = Math.abs(number - (double) numerator / (double) denominator);

					boolean isNewBest = false;

					if (currentBestDelta == null) {
						isNewBest = true;
					} else if (delta < currentBestDelta && Math.abs(currentBestDelta - delta) > doubleEquivalencyFactor) {
						isNewBest = true;
					}

					if (isNewBest) {
						currentBestNumerator = numerator;
						currentBestDenominator = denominator;
						currentBestDelta = delta;
					}
				}
			}

			return String.format("%d/%d", currentBestNumerator, currentBestDenominator);
		}
	}
}
