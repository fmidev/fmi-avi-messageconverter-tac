package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.AtmosphericPressureQNH;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover;
import fi.fmi.avi.converter.tac.lexer.impl.token.ColorCode;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetricHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.RunwayStateContamination;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.RunwayStateDeposit;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.RunwayStateReportSpecialValue;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.RunwayStateReportType;
import fi.fmi.avi.converter.tac.lexer.impl.token.SurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.TrendChangeIndicator.TrendChangeIndicatorType;
import fi.fmi.avi.converter.tac.lexer.impl.token.TrendTimeGroup.TrendTimePeriodType;
import fi.fmi.avi.converter.tac.lexer.impl.token.Weather;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationCodeListUser.BreakingAction;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.RunwayDirection;
import fi.fmi.avi.model.impl.CloudForecastImpl;
import fi.fmi.avi.model.impl.NumericMeasureImpl;
import fi.fmi.avi.model.impl.WeatherImpl;
import fi.fmi.avi.model.metar.HorizontalVisibility;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.ObservedSurfaceWind;
import fi.fmi.avi.model.metar.RunwayState;
import fi.fmi.avi.model.metar.RunwayVisualRange;
import fi.fmi.avi.model.metar.SeaState;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.TrendForecastSurfaceWind;
import fi.fmi.avi.model.metar.WindShear;
import fi.fmi.avi.model.metar.impl.HorizontalVisibilityImpl;
import fi.fmi.avi.model.metar.impl.ObservedCloudsImpl;
import fi.fmi.avi.model.metar.impl.ObservedSurfaceWindImpl;
import fi.fmi.avi.model.metar.impl.RunwayStateImpl;
import fi.fmi.avi.model.metar.impl.RunwayVisualRangeImpl;
import fi.fmi.avi.model.metar.impl.SeaStateImpl;
import fi.fmi.avi.model.metar.impl.TrendForecastImpl;
import fi.fmi.avi.model.metar.impl.TrendForecastSurfaceWindImpl;
import fi.fmi.avi.model.metar.impl.TrendTimeGroupsImpl;
import fi.fmi.avi.model.metar.impl.WindShearImpl;

/**
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public abstract class METARTACParserBase<S extends METAR> extends AbstractTACParser<S> {

    private static final Logger LOG = LoggerFactory.getLogger(METARTACParserBase.class);

    private static Lexeme.Identity[] zeroOrOneAllowed = { Lexeme.Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.AIR_DEWPOINT_TEMPERATURE,
            Identity.AIR_PRESSURE_QNH, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.REMARKS_START, Identity.NIL, Identity.ROUTINE_DELAYED_OBSERVATION };

    private AviMessageLexer lexer;

    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    public ConversionResult<S> convertMessage(final String input, final ConversionHints hints) {
        ConversionResult<S> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }

        LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (!lexingSuccessful(lexed, hints)) {
            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Input message lexing was not fully successful: " + lexed));
            List<Lexeme> errors = lexed.getLexemes().stream().filter(l -> !Lexeme.Status.OK.equals(l.getStatus())).collect(Collectors.toList());
            for (Lexeme l : errors) {
                result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Lexing problem with '" + l.getTACToken() + "': " + l.getLexerMessage()));
            }
            result.setStatus(ConversionResult.Status.WITH_ERRORS);
            return result;
        }

        if (getExpectedFirstTokenIdentity() != lexed.getFirstLexeme().getIdentityIfAcceptable()) {
            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Input message is not recognized as " + getExpectedFirstTokenIdentity()));
            return result;
        }

        if (endsInEndToken(lexed, hints)) {
            List<ConversionIssue> issues = checkZeroOrOne(lexed, zeroOrOneAllowed);
            if (!issues.isEmpty()) {
                result.addIssue(issues);
            }
            result.setConvertedMessage(this.getMessageInstance());
            if (lexed.getTAC() != null) {
                result.getConvertedMessage().setTranslatedTAC(lexed.getTAC());
                result.getConvertedMessage().setTranslationTime(ZonedDateTime.now());
            }

            //Split into obs & trends (+possible remarks)
            List<LexemeSequence> subSequences = lexed.splitBy(Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START);
            LexemeSequence obs = subSequences.get(0);

            findNext(Identity.CORRECTION, obs.getFirstLexeme(), (match) -> {
                final Identity[] before = { Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.ROUTINE_DELAYED_OBSERVATION, Identity.NIL,
                        Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE,
                        Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                        Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    result.getConvertedMessage().setStatus(AviationCodeListUser.MetarStatus.CORRECTION);
                }
            }, () -> result.getConvertedMessage().setStatus(AviationCodeListUser.MetarStatus.NORMAL));

            findNext(Identity.AERODROME_DESIGNATOR, obs.getFirstLexeme(), (match) -> {
                final Identity[] before = new Identity[] { Identity.ISSUE_TIME, Identity.ROUTINE_DELAYED_OBSERVATION, Identity.NIL, Identity.SURFACE_WIND,
                        Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH,
                        Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                        Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    Aerodrome ad = new Aerodrome(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
                    result.getConvertedMessage().setAerodrome(ad);
                }
            }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Aerodrome designator not given in " + input)));

            updateMetarIssueTime(result, obs, hints);

            findNext(Identity.AUTOMATED, obs.getFirstLexeme(), (match) -> {
                final Identity[] before = new Identity[] { Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.CLOUD,
                        Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE,
                        Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    result.getConvertedMessage().setAutomatedStation(true);
                }
            });

            findNext(Identity.ROUTINE_DELAYED_OBSERVATION, obs.getFirstLexeme(), (match) -> {
                final Identity[] before = new Identity[] { Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.CLOUD,
                        Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE,
                        Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    result.getConvertedMessage().setDelayed(true);
                }
            });

            findNext(Identity.NIL, obs.getFirstLexeme(), (match) -> {
                final Identity[] before = new Identity[] { Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.CLOUD,
                        Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE,
                        Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    result.getConvertedMessage().setStatus(AviationCodeListUser.MetarStatus.MISSING);
                    if (match.getNext() != null) {
                        Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                        if (Identity.END_TOKEN != nextTokenId && Identity.REMARKS_START != nextTokenId) {
                            result.addIssue(
                                    new ConversionIssue(ConversionIssue.Type.LOGICAL_ERROR, "Missing METAR message contains extra tokens after NIL: " + input));
                        }
                    }
                }
            });

            if (AviationCodeListUser.MetarStatus.MISSING == result.getConvertedMessage().getStatus()) {
                return result;
            }

            updateObservedSurfaceWind(result, obs, hints);

            findNext(Identity.CAVOK, obs.getFirstLexeme(), (match) -> {
                final Identity[] before = new Identity[] { Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE,
                        Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                        Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    result.getConvertedMessage().setCeilingAndVisibilityOk(true);
                }
            });

            updateHorizontalVisibility(result, obs, hints);
            updateRVR(result, obs, hints);
            updatePresentWeather(result, obs, hints);
            updateObservedClouds(result, obs, hints);
            updateTemperatures(result, obs, hints);
            updateQNH(result, obs, hints);
            updateRecentWeather(result, obs, hints);
            updateWindShear(result, obs, hints);
            updateSeaState(result, obs, hints);
            updateRunwayStates(result, obs, hints);
            updateColorState(result, obs, hints);

            if (subSequences.size() > 0) {
                for (int i = 1; i < subSequences.size(); i++) {
                    LexemeSequence seq = subSequences.get(i);
                    if (Identity.TREND_CHANGE_INDICATOR == seq.getFirstLexeme().getIdentity()) {
                        updateTrend(result, seq.getFirstLexeme(), hints);
                    } else if (Identity.REMARKS_START == seq.getFirstLexeme().getIdentity()) {
                        updateRemarks(result, seq.getFirstLexeme(), hints);
                    }
                }
            }
        } else {
            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Message does not end in end token"));
        }
        return result;
    }

    protected abstract S getMessageInstance();

    protected abstract Lexeme.Identity getExpectedFirstTokenIdentity();

    private static void updateMetarIssueTime(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();
        Identity[] before = { Identity.ROUTINE_DELAYED_OBSERVATION, Identity.NIL, Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY,
                Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER,
                Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
        result.addIssue(updateIssueTime(msg, lexed, before, hints));
    }

    private static void updateObservedSurfaceWind(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();

        findNext(Identity.SURFACE_WIND, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD,
                    Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE,
                    Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                Object direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
                Integer meanSpeed = match.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
                Integer gust = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

                final ObservedSurfaceWind wind = new ObservedSurfaceWindImpl();

                if (direction == SurfaceWind.WindDirection.VARIABLE) {
                    wind.setVariableDirection(true);
                } else if (direction != null && direction instanceof Integer) {
                    wind.setMeanWindDirection(new NumericMeasureImpl((Integer) direction, "deg"));
                } else {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Direction missing for surface wind:" + match.getTACToken()));
                }

                if (meanSpeed != null) {
                    wind.setMeanWindSpeed(new NumericMeasureImpl(meanSpeed, unit));
                } else {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean speed missing for surface wind:" + match.getTACToken()));
                }

                if (gust != null) {
                    wind.setWindGust(new NumericMeasureImpl(gust, unit));
                }

                findNext(Identity.VARIABLE_WIND_DIRECTION, match, (varMatch) -> {
                    ConversionIssue varIssue = checkBeforeAnyOf(varMatch, before);
                    if (varIssue != null) {
                        result.addIssue(varIssue);
                    } else {
                        Integer maxDirection = varMatch.getParsedValue(Lexeme.ParsedValueName.MAX_DIRECTION, Integer.class);
                        Integer minDirection = varMatch.getParsedValue(Lexeme.ParsedValueName.MIN_DIRECTION, Integer.class);

                        if (minDirection != null) {
                            wind.setExtremeCounterClockwiseWindDirection(new NumericMeasureImpl(minDirection, "deg"));
                        }
                        if (maxDirection != null) {
                            wind.setExtremeClockwiseWindDirection(new NumericMeasureImpl(maxDirection, "deg"));
                        }
                    }
                });
                msg.setSurfaceWind(wind);
            }
        }, () -> {
            //TODO: cases where it's ok to be missing the surface wind
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing surface wind information in " + lexed.getTAC()));
        });
    }

    private static void updateHorizontalVisibility(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();

        findNext(Identity.HORIZONTAL_VISIBILITY, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH,
                    Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                    Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            HorizontalVisibility vis = new HorizontalVisibilityImpl();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    if (msg.isCeilingAndVisibilityOk()) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with prevailing visibility"));
                        break;
                    }
                    MetricHorizontalVisibility.DirectionValue direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION,
                            MetricHorizontalVisibility.DirectionValue.class);
                    String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                    Double value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
                    RecognizingAviMessageTokenLexer.RelationalOperator operator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                            RecognizingAviMessageTokenLexer.RelationalOperator.class);
                    if (direction != null) {
                        if (vis.getMinimumVisibility() != null) {
                            result.addIssue(
                                    new ConversionIssue(Type.LOGICAL_ERROR, "More than one directional horizontal visibility given: " + match.getTACToken()));
                        } else {
                            vis.setMinimumVisibility(new NumericMeasureImpl(value, unit));
                            vis.setMinimumVisibilityDirection(new NumericMeasureImpl(direction.inDegrees(), "deg"));
                        }
                    } else {
                        if (vis.getPrevailingVisibility() != null) {
                            result.addIssue(
                                    new ConversionIssue(Type.LOGICAL_ERROR, "More than one prevailing horizontal visibility given: " + match.getTACToken()));
                        } else {
                            vis.setPrevailingVisibility(new NumericMeasureImpl(value, unit));
                            if (RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN == operator) {
                                vis.setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RecognizingAviMessageTokenLexer.RelationalOperator.MORE_THAN == operator) {
                                vis.setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }
                        }
                    }
                    msg.setVisibility(vis);
                }
                match = findNext(Identity.HORIZONTAL_VISIBILITY, match);
            }
        }, () -> {
            // If no horizontal visibility and no CAVOK
            if (!result.getConvertedMessage().isCeilingAndVisibilityOk()) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing horizontal visibility / cavok in " + lexed.getTAC()));
            }
        });
    }

    private static void updateRVR(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();

        findNext(Identity.RUNWAY_VISUAL_RANGE, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR,
                    Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            List<RunwayVisualRange> rvrs = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    if (msg.isCeilingAndVisibilityOk()) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with runway visual range"));
                        break;
                    }
                    String rwCode = match.getParsedValue(Lexeme.ParsedValueName.RUNWAY, String.class);
                    if (rwCode == null) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing runway code for RVR in " + match.getTACToken()));
                    } else {
                        RunwayDirection runway = new RunwayDirection(rwCode);
                        runway.setAssociatedAirportHeliport(msg.getAerodrome());

                        Integer minValue = match.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Integer.class);
                        RecognizingAviMessageTokenLexer.RelationalOperator minValueOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                                RecognizingAviMessageTokenLexer.RelationalOperator.class);
                        Integer maxValue = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                        RecognizingAviMessageTokenLexer.RelationalOperator maxValueOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR2,
                                RecognizingAviMessageTokenLexer.RelationalOperator.class);
                        RecognizingAviMessageTokenLexer.TendencyOperator tendencyIndicator = match.getParsedValue(Lexeme.ParsedValueName.TENDENCY_OPERATOR,
                                RecognizingAviMessageTokenLexer.TendencyOperator.class);
                        String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

                        if (minValue == null) {
                            result.addIssue(
                                    new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing visibility value for RVR in " + match.getTACToken()));
                        }
                        RunwayVisualRange rvr = new RunwayVisualRangeImpl();
                        rvr.setRunwayDirection(runway);
                        if (maxValue != null && minValue != null) {
                            rvr.setVaryingRVRMinimum(new NumericMeasureImpl(minValue, unit));
                            if (RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN == minValueOperator) {
                                rvr.setVaryingRVRMinimumOperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RecognizingAviMessageTokenLexer.RelationalOperator.MORE_THAN == minValueOperator) {
                                rvr.setVaryingRVRMinimumOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }

                            rvr.setVaryingRVRMaximum(new NumericMeasureImpl(maxValue, unit));
                            if (RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN == maxValueOperator) {
                                rvr.setVaryingRVRMaximumOperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RecognizingAviMessageTokenLexer.RelationalOperator.MORE_THAN == maxValueOperator) {
                                rvr.setVaryingRVRMaximumOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }
                        } else if (minValue != null) {
                            rvr.setMeanRVR(new NumericMeasureImpl(minValue, unit));
                            if (RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN == minValueOperator) {
                                rvr.setMeanRVROperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RecognizingAviMessageTokenLexer.RelationalOperator.MORE_THAN == minValueOperator) {
                                rvr.setMeanRVROperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }
                        }
                        if (RecognizingAviMessageTokenLexer.TendencyOperator.DOWNWARD == tendencyIndicator) {
                            rvr.setPastTendency(AviationCodeListUser.VisualRangeTendency.DOWNWARD);
                        } else if (RecognizingAviMessageTokenLexer.TendencyOperator.UPWARD == tendencyIndicator) {
                            rvr.setPastTendency(AviationCodeListUser.VisualRangeTendency.UPWARD);
                        } else if (RecognizingAviMessageTokenLexer.TendencyOperator.NO_CHANGE == tendencyIndicator) {
                            rvr.setPastTendency(AviationCodeListUser.VisualRangeTendency.NO_CHANGE);
                        }
                        rvrs.add(rvr);
                        msg.setRunwayVisualRanges(rvrs);
                    }
                    match = findNext(Identity.RUNWAY_VISUAL_RANGE, match);

                }
            }
        });
    }

    private static void updatePresentWeather(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();

        findNext(Identity.WEATHER, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR,
                    Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                if (msg.isCeilingAndVisibilityOk()) {
                    result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with prevailing visibility"));
                } else {
                    List<fi.fmi.avi.model.Weather> weather = new ArrayList<>();
                    result.addIssue(appendWeatherCodes(match, weather, before, hints));
                    if (!weather.isEmpty()) {
                        msg.setPresentWeather(weather);
                    }
                }
            }
        });
    }

    private static void updateObservedClouds(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();

        findNext(Identity.CLOUD, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR,
                    Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ObservedClouds clouds = new ObservedCloudsImpl();
            ConversionIssue issue;
            List<fi.fmi.avi.model.CloudLayer> layers = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    if (msg.isCeilingAndVisibilityOk()) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with prevailing visibility"));
                        break;
                    }
                    CloudLayer.CloudCover cover = match.getParsedValue(Lexeme.ParsedValueName.COVER, CloudLayer.CloudCover.class);
                    Object value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Object.class);
                    String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

                    if (CloudLayer.SpecialValue.AMOUNT_AND_HEIGHT_UNOBSERVABLE_BY_AUTO_SYSTEM == value) {
                        clouds.setAmountAndHeightUnobservableByAutoSystem(true);
                    } else if (CloudCover.NO_SIG_CLOUDS == cover || CloudCover.SKY_CLEAR == cover || CloudCover.NO_LOW_CLOUDS == cover
                            || CloudCover.NO_CLOUD_DETECTED == cover) {
                        clouds.setNoSignificantCloud(true);
                    } else if (value instanceof Integer) {
                        if (CloudLayer.CloudCover.SKY_OBSCURED == cover) {
                            int height = ((Integer) value);
                            if ("hft".equals(unit)) {
                                height = height * 100;
                                unit = "[ft_i]";
                            }
                            clouds.setVerticalVisibility(new NumericMeasureImpl(height, unit));
                        } else {
                            fi.fmi.avi.model.CloudLayer layer = getCloudLayer(match);
                            if (layer != null) {
                                layers.add(layer);
                            } else {
                                result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Could not parse token " + match.getTACToken() + " as cloud layer"));
                            }
                        }
                    } else {
                        result.addIssue(
                                new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Cloud layer height is not an integer in " + match.getTACToken()));
                    }
                }
                match = findNext(Identity.CLOUD, match);
            }
            if (!layers.isEmpty()) {
                clouds.setLayers(layers);
            }
            msg.setClouds(clouds);
        });

    }

    private static void updateTemperatures(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();
        findNext(Identity.AIR_DEWPOINT_TEMPERATURE, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE,
                    Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                Double[] values = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Double[].class);
                if (values == null) {
                    result.addIssue(
                            new ConversionIssue(Type.MISSING_DATA, "Missing air temperature and dewpoint temperature values in " + match.getTACToken()));
                } else {
                    if (values[0] != null) {
                        msg.setAirTemperature(new NumericMeasureImpl(values[0], unit));
                    } else {
                        result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Missing air temperature value in " + match.getTACToken()));
                    }
                    if (values[1] != null) {
                        msg.setDewpointTemperature(new NumericMeasureImpl(values[1], unit));
                    } else {
                        result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Missing dewpoint temperature value in " + match.getTACToken()));
                    }
                }
            }
        }, () -> result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Missing air temperature and dewpoint temperature values in " + lexed.getTAC())));

    }

    private static void updateQNH(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();
        findNext(Identity.AIR_PRESSURE_QNH, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                    Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                AtmosphericPressureQNH.PressureMeasurementUnit unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT,
                        AtmosphericPressureQNH.PressureMeasurementUnit.class);
                Integer value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Integer.class);
                if (value != null) {
                    String unitStr = "";
                    if (unit == AtmosphericPressureQNH.PressureMeasurementUnit.HECTOPASCAL) {
                        unitStr = "hPa";
                    } else if (unit == AtmosphericPressureQNH.PressureMeasurementUnit.INCHES_OF_MERCURY) {
                        unitStr = "in Hg";
                    } else {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                                "Unknown unit for air pressure: " + unitStr + " in " + match.getTACToken()));
                    }
                    msg.setAltimeterSettingQNH(new NumericMeasureImpl(value, unitStr));
                } else {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing air pressure value: " + match.getTACToken()));
                }
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "QNH missing in " + lexed.getTAC())));
    }

    private static void updateRecentWeather(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();
        findNext(Identity.RECENT_WEATHER, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR,
                    Identity.REMARKS_START };
            List<fi.fmi.avi.model.Weather> weather = new ArrayList<>();
            result.addIssue(appendWeatherCodes(match, weather, before, hints));
            if (!weather.isEmpty()) {
                msg.setRecentWeather(weather);
            }
        });
    }

    private static void updateWindShear(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();
        findNext(Identity.WIND_SHEAR, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            final WindShear ws = new WindShearImpl();
            List<RunwayDirection> runways = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    String rw = match.getParsedValue(Lexeme.ParsedValueName.RUNWAY, String.class);
                    if ("ALL".equals(rw)) {
                        if (!runways.isEmpty()) {
                            result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR,
                                    "Wind shear reported both to all runways and at least one specific runway: " + match.getTACToken()));
                        } else {
                            ws.setAllRunways(true);
                        }
                    } else if (rw != null) {
                        if (ws.isAllRunways()) {
                            result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR,
                                    "Wind shear reported both to all runways and at least one specific runway:" + match.getTACToken()));
                        } else {
                            RunwayDirection rwd = new RunwayDirection(rw);
                            rwd.setAssociatedAirportHeliport(msg.getAerodrome());
                            runways.add(rwd);
                        }
                    }
                }
                match = findNext(Identity.WIND_SHEAR, match);
            }
            if (!runways.isEmpty()) {
                ws.setRunwayDirections(runways);
            }
            msg.setWindShear(ws);
        });
    }

    private static void updateSeaState(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();
        findNext(Identity.SEA_STATE, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                SeaState ss = new SeaStateImpl();
                Object[] values = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Object[].class);
                if (values[0] instanceof Integer) {
                    String tempUnit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                    ss.setSeaSurfaceTemperature(new NumericMeasureImpl((Integer) values[0], tempUnit));
                }
                if (values[1] instanceof fi.fmi.avi.converter.tac.lexer.impl.token.SeaState.SeaSurfaceState) {
                    if (values[2] != null) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR,
                                "Sea state cannot contain both sea surface state and significant wave height:" + match.getTACToken()));
                    } else {
                        switch ((fi.fmi.avi.converter.tac.lexer.impl.token.SeaState.SeaSurfaceState) values[1]) {
                            case CALM_GLASSY:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.CALM_GLASSY);
                                break;
                            case CALM_RIPPLED:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.CALM_RIPPLED);
                                break;
                            case SMOOTH_WAVELETS:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.SMOOTH_WAVELETS);
                                break;
                            case SLIGHT:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.SLIGHT);
                                break;
                            case MODERATE:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.MODERATE);
                                break;
                            case ROUGH:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.ROUGH);
                                break;
                            case VERY_ROUGH:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.VERY_ROUGH);
                                break;
                            case HIGH:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.HIGH);
                                break;
                            case VERY_HIGH:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.VERY_HIGH);
                                break;
                            case PHENOMENAL:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.PHENOMENAL);
                                break;
                            case MISSING:
                                ss.setSeaSurfaceState(AviationCodeListUser.SeaSurfaceState.MISSING_VALUE);
                                break;
                        }
                    }
                }
                if (values[2] instanceof Number) {
                    if (values[1] != null) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR,
                                "Sea state cannot contain both sea surface state and significant wave height:" + match.getTACToken()));
                    } else {
                        String heightUnit = match.getParsedValue(Lexeme.ParsedValueName.UNIT2, String.class);
                        ss.setSignificantWaveHeight(new NumericMeasureImpl(((Number) values[2]).doubleValue(), heightUnit));
                    }
                }
                msg.setSeaState(ss);
            }
        });
    }

    private static void updateRunwayStates(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();

        findNext(Identity.RUNWAY_STATE, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            List<RunwayState> states = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                    match = findNext(Identity.RUNWAY_STATE, match);
                    continue;
                }
                RunwayStateImpl rws = new RunwayStateImpl();
                @SuppressWarnings("unchecked")
                Map<RunwayStateReportType, Object> values = match.getParsedValue(ParsedValueName.VALUE, Map.class);

                Boolean repetition = (Boolean) values.get(RunwayStateReportType.REPETITION);
                Boolean allRunways = (Boolean) values.get(RunwayStateReportType.ALL_RUNWAYS);
                RunwayDirection runway = new RunwayDirection(match.getParsedValue(ParsedValueName.RUNWAY, String.class));
                runway.setAssociatedAirportHeliport(msg.getAerodrome());
                RunwayStateDeposit deposit = (RunwayStateDeposit) values.get(RunwayStateReportType.DEPOSITS);
                RunwayStateContamination contamination = (RunwayStateContamination) values.get(RunwayStateReportType.CONTAMINATION);
                Integer depthOfDeposit = (Integer) values.get(RunwayStateReportType.DEPTH_OF_DEPOSIT);
                String unitOfDeposit = (String) values.get(RunwayStateReportType.UNIT_OF_DEPOSIT);
                RunwayStateReportSpecialValue depthModifier = (RunwayStateReportSpecialValue) values.get(RunwayStateReportType.DEPTH_MODIFIER);
                Boolean cleared = (Boolean) values.get(RunwayStateReportType.CLEARED);

                Object breakingAction = values.get(RunwayStateReportType.BREAKING_ACTION);
                Object frictionCoefficient = values.get(RunwayStateReportType.FRICTION_COEFFICIENT);

                Boolean snowClosure = (Boolean) values.get(RunwayStateReportType.SNOW_CLOSURE);

                // Runway direction is missing if repetition, allRunways or SnoClo:
                if (repetition != null && repetition) {
                    rws.setRepetition(true);
                } else if (allRunways != null && allRunways) {
                    rws.setAllRunways(true);
                } else if (snowClosure != null && snowClosure.booleanValue()) {
                    rws.setAllRunways(true);
                    rws.setSnowClosure(true);
                } else if (runway.getDesignator() != null) {
                    rws.setRunwayDirection(runway);
                } else {
                    result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "No runway specified for runway state report: " + match.getTACToken()));
                }
                if (deposit != null) {
                    AviationCodeListUser.RunwayDeposit value = fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.convertRunwayStateDepositToAPI(deposit);
                    if (value != null) {
                        rws.setDeposit(value);
                    }
                }

                if (contamination != null) {
                    AviationCodeListUser.RunwayContamination value = fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.convertRunwayStateContaminationToAPI(
                            contamination);
                    if (value != null) {
                        rws.setContamination(value);
                    }
                }

                if (depthOfDeposit != null) {
                    if (deposit == null) {
                        result.addIssue(
                                new ConversionIssue(Type.LOGICAL_ERROR, "Missing deposit kind but depth given for runway state: " + match.getTACToken()));
                    } else {
                        rws.setDepthOfDeposit(new NumericMeasureImpl(depthOfDeposit, unitOfDeposit));
                    }
                }

                if (depthModifier != null) {
                    if (depthOfDeposit == null && depthModifier == RunwayStateReportSpecialValue.NOT_MEASURABLE) {
                        rws.setDepthNotMeasurable(true);
                        rws.setDepthOfDeposit(null);
                    } else if (depthOfDeposit == null && depthModifier != RunwayStateReportSpecialValue.RUNWAY_NOT_OPERATIONAL) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR,
                                "Missing deposit depth but depth modifier given for runway state: " + match.getTACToken()));
                    } else {
                        switch (depthModifier) {
                            case LESS_THAN_OR_EQUAL:
                                rws.setDepthOperator(AviationCodeListUser.RelationalOperator.BELOW);
                                break;
                            case MEASUREMENT_UNRELIABLE:
                            case NOT_MEASURABLE:
                                result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR,
                                        "Illegal modifier for depth of deposit for runway state:" + match.getTACToken()));
                                break;
                            case MORE_THAN_OR_EQUAL:
                                rws.setDepthOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                                break;
                            case RUNWAY_NOT_OPERATIONAL:
                                rws.setRunwayNotOperational(true);
                                break;
                        }
                    }
                }
                if (cleared != null && cleared) {
                    if (deposit != null || contamination != null || depthOfDeposit != null) {
                        result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR,
                                "Runway state cannot be both cleared and contain deposit or contamination info: " + match.getTACToken()));
                    } else {
                        rws.setCleared(true);
                    }
                }

                if (breakingAction instanceof fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.BreakingAction) {
                    BreakingAction action = fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.convertBreakingActionToAPI(
                            (fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState.BreakingAction) breakingAction);

                    rws.setBreakingAction(action);
                } else if (breakingAction instanceof RunwayStateReportSpecialValue) {
                    switch ((RunwayStateReportSpecialValue) breakingAction) {
                        case RUNWAY_NOT_OPERATIONAL:
                            rws.setRunwayNotOperational(true);
                            break;
                        case MEASUREMENT_UNRELIABLE:
                            rws.setEstimatedSurfaceFrictionUnreliable(true);
                            break;
                        case MORE_THAN_OR_EQUAL:
                        case LESS_THAN_OR_EQUAL:
                        case NOT_MEASURABLE:
                            // TODO: no idea what we should do here
                            break;
                    }
                }

                if (frictionCoefficient != null && frictionCoefficient instanceof Number) {
                    rws.setEstimatedSurfaceFriction(((Number) frictionCoefficient).doubleValue());
                } else if (frictionCoefficient == RunwayStateReportSpecialValue.MEASUREMENT_UNRELIABLE) {
                    rws.setEstimatedSurfaceFrictionUnreliable(true);
                }

                states.add(rws);
                match = findNext(Identity.RUNWAY_STATE, match);
            }
            if (!states.isEmpty()) {
                msg.setRunwayStates(states);
            }
        });
    }

    private static void updateColorState(final ConversionResult<? extends METAR> result, final LexemeSequence lexed, final ConversionHints hints) {
        final METAR msg = result.getConvertedMessage();
        findNext(Identity.COLOR_CODE, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                ColorCode.ColorState code = match.getParsedValue(ParsedValueName.VALUE, ColorCode.ColorState.class);
                for (AviationCodeListUser.ColorState state : AviationCodeListUser.ColorState.values()) {
                    if (state.name().equalsIgnoreCase(code.getCode())) {
                        msg.setColorState(state);
                    }
                }
                if (msg.getColorState() == null) {
                    result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Unknown color state '" + code.getCode() + "'"));
                }
            }
        });
    }

    private static void updateTrend(final ConversionResult<? extends METAR> result, final Lexeme changeFctToken, final ConversionHints hints) {
        if (Identity.TREND_CHANGE_INDICATOR != changeFctToken.getIdentity()) {
            throw new IllegalArgumentException("Cannot update Trend, the start lexeme " + changeFctToken + " is not a change forecast start token");
        }
        ConversionIssue issue = checkBeforeAnyOf(changeFctToken, new Identity[] { Identity.REMARKS_START });
        if (issue != null) {
            result.addIssue(issue);
            return;
        }
        METAR metar = result.getConvertedMessage();
        List<TrendForecast> trends = metar.getTrends();
        if (trends == null) {
            trends = new ArrayList<>();
            metar.setTrends(trends);
        }

        TrendForecast fct = new TrendForecastImpl();
        TrendChangeIndicatorType type = changeFctToken.getParsedValue(ParsedValueName.TYPE, TrendChangeIndicatorType.class);
        switch (type) {
            case BECOMING:
                fct.setChangeIndicator(AviationCodeListUser.TrendForecastChangeIndicator.BECOMING);
                updateTrendContents(result, fct, changeFctToken, hints);
                break;
            case TEMPORARY_FLUCTUATIONS:
                fct.setChangeIndicator(AviationCodeListUser.TrendForecastChangeIndicator.TEMPORARY_FLUCTUATIONS);
                updateTrendContents(result, fct, changeFctToken, hints);
                break;
            case NO_SIGNIFICANT_CHANGES:
                fct.setChangeIndicator(AviationCodeListUser.TrendForecastChangeIndicator.NO_SIGNIFICANT_CHANGES);
                break;
            default:
                break;
        }
        trends.add(fct);
    }

    private static void updateTrendContents(final ConversionResult<? extends METAR> result, final TrendForecast fct, final Lexeme groupStart,
            final ConversionHints hints) {
        //Check for the possibly following FM, TL and AT tokens:
        Lexeme token = groupStart.getNext();
        TrendTimeGroups timeGroups = parseChangeTimeGroups(result, token, hints);
        if (timeGroups != null) {
            fct.setTimeGroups(timeGroups);
            token = token.getNext();
        }

        //loop over change group tokens:
        List<fi.fmi.avi.model.Weather> forecastWeather = null;
        Lexeme.Identity[] before = { Identity.REMARKS_START, Identity.END_TOKEN };
        while (token != null) {
            if (checkBeforeAnyOf(token, before) != null) {
                break;
            }
            Identity id = token.getIdentity();
            if (id != null) {
                switch (id) {
                    case CAVOK:
                        fct.setCeilingAndVisibilityOk(true);
                        break;
                    case CLOUD: {
                        updateForecastCloud(result, fct, token, hints);
                        break;
                    }
                    case HORIZONTAL_VISIBILITY: {
                        if (fct.getPrevailingVisibility() == null) {
                            updatePrevailingVisibility(fct, token, hints);
                        } else {
                            result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR,
                                    "More than one visibility token within a trend change group: " + token.getTACToken()));
                        }
                        break;
                    }
                    case SURFACE_WIND: {
                        if (fct.getSurfaceWind() == null) {
                            updateForecastWind(result, fct, token, hints);
                        } else {
                            result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "More than one wind token within a trend change group"));
                        }
                        break;
                    }
                    case WEATHER: {
                        if (forecastWeather == null) {
                            forecastWeather = new ArrayList<>();
                            fct.setForecastWeather(forecastWeather);
                        }
                        String code = token.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class);
                        if (code != null) {
                            fi.fmi.avi.model.Weather weather = new WeatherImpl();
                            weather.setCode(code);
                            weather.setDescription(Weather.WEATHER_CODES.get(code));
                            forecastWeather.add(weather);
                        } else {
                            result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Weather code not found"));
                        }
                        break;
                    }
                    case NO_SIGNIFICANT_WEATHER:
                        fct.setNoSignificantWeather(true);
                        break;
                    case COLOR_CODE: {
                        ColorCode.ColorState code = token.getParsedValue(ParsedValueName.VALUE, ColorCode.ColorState.class);
                        for (AviationCodeListUser.ColorState state : AviationCodeListUser.ColorState.values()) {
                            if (state.name().equalsIgnoreCase(code.getCode())) {
                                fct.setColorState(state);
                            }
                        }
                        if (fct.getColorState() == null) {
                            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Unknown color state '" + code.getCode() + "'"));
                        }
                        break;
                    }
                    case END_TOKEN: {
                        break;
                    }
                    default:
                        result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Illegal token " + token.getTACToken() + " within the change forecast group"));
                        break;
                }
            }
            if (Identity.END_TOKEN == token.getIdentity()) {
                break;
            }
            token = token.getNext();
        }
        if (fct.getCloud() != null) {
            if (fct.getCloud().getLayers() != null && !fct.getCloud().getLayers().isEmpty()) {
                if (fct.getCloud().isNoSignificantCloud()) {
                    result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "Cloud layers cannot co-exist with NSC in trend"));
                } else if (fct.getCloud().getVerticalVisibility() != null) {
                    result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "Cloud layers cannot co-exist with vertical visibility in trend"));
                }
            }
        }

        if (fct.isCeilingAndVisibilityOk()) {
            if (fct.getCloud() != null || fct.getPrevailingVisibility() != null || forecastWeather != null || fct.isNoSignificantWeather()) {
                result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with cloud, prevailing visibility, weather, NSW " + "in trend"));
            }
        } else {
            if (fct.isNoSignificantWeather() && forecastWeather != null && !forecastWeather.isEmpty()) {
                result.addIssue(new ConversionIssue(Type.LOGICAL_ERROR, "Forecast weather cannot co-exist with NSW in trend"));
            }

        }
    }

    private static void updateForecastCloud(final ConversionResult<? extends METAR> result, final TrendForecast fct, final Lexeme token,
            final ConversionHints hints) {
        CloudForecast cloud;
        List<fi.fmi.avi.model.CloudLayer> cloudLayers;
        if (fct.getCloud() == null) {
            fct.setCloud(new CloudForecastImpl());
        }
        cloud = fct.getCloud();

        Object value = token.getParsedValue(Lexeme.ParsedValueName.VALUE, Object.class);
        String unit = token.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
        CloudLayer.CloudCover cover = token.getParsedValue(Lexeme.ParsedValueName.COVER, CloudLayer.CloudCover.class);
        if (CloudLayer.CloudCover.SKY_OBSCURED == cover) {
            if (value instanceof Integer) {
                int height = (Integer) value;
                if ("hft".equals(unit)) {
                    height = height * 100;
                    unit = "[ft_i]";
                }
                cloud.setVerticalVisibility(new NumericMeasureImpl(height, unit));
            } else {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Missing value for vertical visibility"));
            }
        } else if (CloudCover.NO_SIG_CLOUDS == cover) {
            cloud.setNoSignificantCloud(true);
        } else {
            fi.fmi.avi.model.CloudLayer layer = getCloudLayer(token);
            if (layer != null) {
                if (cloud.getLayers() == null) {
                    cloud.setLayers(new ArrayList<>());
                }
                cloudLayers = cloud.getLayers();
                cloudLayers.add(layer);
            } else {
                result.addIssue(new ConversionIssue(Type.MISSING_DATA, "Missing base for cloud layer"));
            }
        }
    }

    private static void updatePrevailingVisibility(final TrendForecast fct, final Lexeme token, final ConversionHints hints) {
        String unit = token.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
        Double value = token.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
        RecognizingAviMessageTokenLexer.RelationalOperator operator = token.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                RecognizingAviMessageTokenLexer.RelationalOperator.class);
        NumericMeasure prevailingVisibility = new NumericMeasureImpl(value, unit);
        AviationCodeListUser.RelationalOperator visibilityOperator = null;
        if (RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN == operator) {
            visibilityOperator = AviationCodeListUser.RelationalOperator.BELOW;
        } else if (RecognizingAviMessageTokenLexer.RelationalOperator.MORE_THAN == operator) {
            visibilityOperator = AviationCodeListUser.RelationalOperator.ABOVE;
        }
        fct.setPrevailingVisibility(prevailingVisibility);
        if (visibilityOperator != null) {
            fct.setPrevailingVisibilityOperator(visibilityOperator);
        }
    }

    private static void updateForecastWind(final ConversionResult<? extends METAR> result, final TrendForecast fct, final Lexeme token,
            final ConversionHints hints) {
        if (fct.getSurfaceWind() == null) {
            fct.setSurfaceWind(new TrendForecastSurfaceWindImpl());
        }
        TrendForecastSurfaceWind wind = fct.getSurfaceWind();
        Object direction = token.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
        Integer meanSpeed = token.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
        Integer gust = token.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
        String unit = token.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

        if (direction == SurfaceWind.WindDirection.VARIABLE) {
            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Wind cannot be variable in trend: " + token.getTACToken()));
        } else if (direction != null && direction instanceof Integer) {
            wind.setMeanWindDirection(new NumericMeasureImpl((Integer) direction, "deg"));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Direction missing for surface wind:" + token.getTACToken()));
        }

        if (meanSpeed != null) {
            wind.setMeanWindSpeed(new NumericMeasureImpl(meanSpeed, unit));
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean speed missing for surface wind:" + token.getTACToken()));
        }

        if (gust != null) {
            wind.setWindGust(new NumericMeasureImpl(gust, unit));
        }
    }

    private static TrendTimeGroups parseChangeTimeGroups(final ConversionResult<? extends METAR> result, final Lexeme token, final ConversionHints hints) {
        TrendTimeGroups timeGroups = null;
        if (Identity.TREND_TIME_GROUP == token.getIdentity()) {
            TrendTimePeriodType type = token.getParsedValue(ParsedValueName.TYPE, TrendTimePeriodType.class);
            if (type != null) {
                timeGroups = new TrendTimeGroupsImpl();
                switch (type) {
                    case AT: {
                        Integer fromHour = token.getParsedValue(ParsedValueName.HOUR1, Integer.class);
                        Integer fromMinute = token.getParsedValue(ParsedValueName.MINUTE1, Integer.class);
                        if (fromHour != null && fromMinute != null) {
                            timeGroups.setPartialStartTime(-1, fromHour, fromMinute);
                        } else {
                            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Missing hour and/or minute from trend AT group " + token.getTACToken()));
                        }
                        timeGroups.setSingleInstance(true);
                        break;
                    }
                    case FROM: {
                        Integer fromHour = token.getParsedValue(ParsedValueName.HOUR1, Integer.class);
                        Integer fromMinute = token.getParsedValue(ParsedValueName.MINUTE1, Integer.class);
                        if (fromHour != null && fromMinute != null) {
                            timeGroups.setPartialStartTime(-1, fromHour, fromMinute);
                        } else {
                            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Missing hour and/or minute from trend FM group " + token.getTACToken()));
                        }
                        break;
                    }
                    case UNTIL: {
                        Integer toHour = token.getParsedValue(ParsedValueName.HOUR1, Integer.class);
                        Integer toMinute = token.getParsedValue(ParsedValueName.MINUTE1, Integer.class);
                        if (toHour != null && toMinute != null) {
                            timeGroups.setPartialEndTime(-1, toHour, toMinute);
                        } else {
                            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Missing hour and/or minute from trend TL group " + token.getTACToken()));
                        }
                        break;
                    }
                    default: {
                        result.addIssue(
                                new ConversionIssue(Type.SYNTAX_ERROR, "Illegal change group '" + token.getTACToken() + "' after change group start token"));
                        break;
                    }
                }

            }
        }
        return timeGroups;
    }

}
