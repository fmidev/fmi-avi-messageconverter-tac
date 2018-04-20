package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.RUNWAY_VISUAL_RANGE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MIN_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RUNWAY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TENDENCY_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser.RelationalOperator;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.RunwayDirection;

/**
 * Created by rinne on 10/02/17.
 */
public class RunwayVisualRange extends RegexMatchingLexemeVisitor {

    public RunwayVisualRange(final Priority prio) {
        super("^R([0-9]{2}[LRC]?)/([MP])?([0-9]{4})(V([MP])?([0-9]{4}))?([UDN])?(FT)?$", prio);
    }

    @Override
	public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
		String runway = match.group(1);
		RecognizingAviMessageTokenLexer.RelationalOperator belowAboveIndicator = RecognizingAviMessageTokenLexer.RelationalOperator.forCode(match.group(2));
		int visibility = Integer.parseInt(match.group(3));
		token.identify(RUNWAY_VISUAL_RANGE);
		token.setParsedValue(RUNWAY, runway);
		token.setParsedValue(MIN_VALUE, visibility);
		if (belowAboveIndicator != null) {
			token.setParsedValue(RELATIONAL_OPERATOR, belowAboveIndicator);
		}
		String variablePart = match.group(4);
		if (variablePart != null) {
			belowAboveIndicator = RecognizingAviMessageTokenLexer.RelationalOperator.forCode(match.group(5));
			if (belowAboveIndicator != null) {
				token.setParsedValue(RELATIONAL_OPERATOR2, belowAboveIndicator);
			}
			int variableVis = Integer.parseInt(match.group(6));
			token.setParsedValue(MAX_VALUE, variableVis);
		}
		RecognizingAviMessageTokenLexer.TendencyOperator tendencyIndicator = RecognizingAviMessageTokenLexer.TendencyOperator.forCode(match.group(7));
		if (tendencyIndicator != null) {
			token.setParsedValue(TENDENCY_OPERATOR, tendencyIndicator);
		}
		String unit = match.group(8);
		if (unit != null) {
			token.setParsedValue(UNIT, "[ft_i]");
		} else {
			token.setParsedValue(UNIT, "m");
		}

	}

	public static class Reconstructor extends FactoryBasedReconstructor {

		@Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
                throws SerializingException {
			Lexeme retval = null;

            Optional<fi.fmi.avi.model.metar.RunwayVisualRange> rvr = getAs(specifier, fi.fmi.avi.model.metar.RunwayVisualRange.class);
            if (rvr.isPresent()) {
                StringBuilder builder = new StringBuilder();
                RunwayDirection rwd = rvr.get().getRunwayDirection();

				builder.append("R");
				builder.append(rwd.getDesignator());
				builder.append("/");

                Optional<NumericMeasure> meanRvr = rvr.get().getMeanRVR();
                String appendUnit;
                if (meanRvr.isPresent()) {
                    Optional<RelationalOperator> operator = rvr.get().getMeanRVROperator();
                    operator.ifPresent(op -> appendOperator(op, builder));
                    appendValue(meanRvr.get(), builder);
                    appendUnit = meanRvr.get().getUom().equals("[ft_i]") ? "FT" : null;

				} else {
                    Optional<NumericMeasure> min = rvr.get().getVaryingRVRMinimum();
                    Optional<NumericMeasure> max = rvr.get().getVaryingRVRMaximum();

                    if (!max.isPresent()) {
                        throw new SerializingException("Cannot tokenize varying RunwayVisualRange with missing max RVR");
                    }
                    if (!min.isPresent()) {
                        throw new SerializingException("Cannot tokenize varying RunwayVisualRange with missing min RVR");
                    }
                    String minUnit = min.get().getUom();
                    String maxUnit = max.get().getUom();
                    if (!minUnit.equals(maxUnit)) {
                        throw new SerializingException(
                                "Cannot tokenize RunwayVisualRange with inconsistent unit of measure for varying RVR: '" + minUnit + "' for min, '" + maxUnit
                                        + "' for max");
                    }
                    Optional<RelationalOperator> operator = rvr.get().getVaryingRVRMinimumOperator();
                    operator.ifPresent(op -> appendOperator(op, builder));
                    appendValue(min.get(), builder);
                    appendUnit = minUnit.equals("[ft_i]") ? "FT" : null;

                    builder.append("V");
                    operator = rvr.get().getVaryingRVRMaximumOperator();
                    operator.ifPresent(op -> appendOperator(op, builder));
                    appendValue(max.get(), builder);

				}

                if (rvr.get().getPastTendency().isPresent()) {
                    switch (rvr.get().getPastTendency().get()) {
                        case DOWNWARD:
							builder.append("D");
							break;

						case UPWARD:
							builder.append("U");
							break;

						case NO_CHANGE:
							builder.append("N");
							break;
					}
				}

				if (appendUnit != null) {
					builder.append(appendUnit);
				}

                return Optional.of(this.createLexeme(builder.toString(), Identity.RUNWAY_VISUAL_RANGE));
            }

            return Optional.empty();
        }

        private void appendOperator(final RelationalOperator operator, final StringBuilder builder) {
            if (operator != null) {
                switch (operator) {
                    case ABOVE:
                        builder.append("P");
                        break;
                    case BELOW:
                        builder.append("M");
                        break;
                }
            }
        }

        private void appendValue(NumericMeasure measure, final StringBuilder builder) throws SerializingException {
            Double value = measure.getValue();
            if (value == null) {
				throw new SerializingException("Missing value for RunwayVisualRange.meanRVR");
			}
			builder.append(String.format("%04d", value.intValue()));
            if (!"[ft_i]".equals(measure.getUom()) && !"m".equals(measure.getUom())) {
                throw new SerializingException("Unknown unit of measure '" + measure.getUom() + "' for RunwayVisualRange, allowed are 'm' and '[ft_i]'");
            }
		}

    }
}
