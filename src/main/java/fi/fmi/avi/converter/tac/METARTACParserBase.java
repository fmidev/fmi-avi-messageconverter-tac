package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import static fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer.*;

import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.*;
import fi.fmi.avi.model.metar.*;
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
import fi.fmi.avi.model.AviationCodeListUser.BreakingAction;
import fi.fmi.avi.model.metar.immutable.HorizontalVisibilityImpl;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.metar.immutable.ObservedCloudsImpl;
import fi.fmi.avi.model.metar.immutable.ObservedSurfaceWindImpl;
import fi.fmi.avi.model.metar.immutable.RunwayStateImpl;
import fi.fmi.avi.model.metar.immutable.RunwayVisualRangeImpl;
import fi.fmi.avi.model.metar.immutable.SeaStateImpl;
import fi.fmi.avi.model.metar.immutable.TrendForecastImpl;
import fi.fmi.avi.model.metar.immutable.TrendForecastSurfaceWindImpl;
import fi.fmi.avi.model.metar.immutable.WindShearImpl;

/**
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public abstract class METARTACParserBase<T extends MeteorologicalTerminalAirReport> extends AbstractTACParser<T> {

    private static final Logger LOG = LoggerFactory.getLogger(METARTACParserBase.class);

    private static Lexeme.Identity[] zeroOrOneAllowed = { Lexeme.Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.AIR_DEWPOINT_TEMPERATURE,
            Identity.AIR_PRESSURE_QNH, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.REMARKS_START, Identity.NIL, Identity.ROUTINE_DELAYED_OBSERVATION };

    private AviMessageLexer lexer;

    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    public ConversionResult<T> convertMessage(final String input, final ConversionHints hints) {
        ConversionResult<T> result = new ConversionResult<>();
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

        if (!endsInEndToken(lexed, hints)) {
            result.addIssue(new ConversionIssue(Type.SYNTAX_ERROR, "Message does not end in end token"));
            return result;
        }

        List<ConversionIssue> issues = checkZeroOrOne(lexed, zeroOrOneAllowed);
        if (!issues.isEmpty()) {
            result.addIssue(issues);
        }

        METARImpl.Builder builder = new METARImpl.Builder();


        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }

        withTimeForTranslation(hints, builder::setTranslationTime);

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
                builder.setStatus(AviationCodeListUser.MetarStatus.CORRECTION);
            }
        }, () -> builder.setStatus(AviationCodeListUser.MetarStatus.NORMAL));

        findNext(Identity.AERODROME_DESIGNATOR, obs.getFirstLexeme(), (match) -> {
            final Identity[] before = new Identity[] { Identity.ISSUE_TIME, Identity.ROUTINE_DELAYED_OBSERVATION, Identity.NIL, Identity.SURFACE_WIND,
                    Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH,
                    Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                    Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setAerodrome(new AerodromeImpl.Builder()
                        .setDesignator(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class))
                        .build());
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Aerodrome designator not given in " + input)));

        result.addIssue(setMETARIssueTime(builder,lexed, hints));

        findNext(Identity.AUTOMATED, obs.getFirstLexeme(), (match) -> {
            final Identity[] before = new Identity[] { Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.CLOUD,
                    Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE,
                    Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setAutomatedStation(true);
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
                builder.setRoutineDelayed(true);
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
                builder.setStatus(AviationCodeListUser.MetarStatus.MISSING);
                if (match.getNext() != null) {
                    Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                    if (Identity.END_TOKEN != nextTokenId && Identity.REMARKS_START != nextTokenId) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL_ERROR,
                                "Missing METAR message contains extra tokens after NIL: " + input));
                    }
                }
            }
        });

        if (AviationCodeListUser.MetarStatus.MISSING == result.getConvertedMessage().getStatus()) {
            return result;
        }

        result.addIssue(setObservedSurfaceWind(builder, obs, hints));

        findNext(Identity.CAVOK, obs.getFirstLexeme(), (match) -> {
            final Identity[] before = new Identity[] { Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE,
                    Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                    Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setCeilingAndVisibilityOk(true);
            }
        });

        result.addIssue(setHorizontalVisibilities(builder, obs, hints));
        result.addIssue(setRVRs(builder, obs, hints));
        result.addIssue(setPresentWeather(builder, obs, hints));
        result.addIssue(setObservedClouds(builder, obs, hints));
        result.addIssue(setTemperatures(builder, obs, hints));
        result.addIssue(setQNH(builder, obs, hints));
        result.addIssue(setRecentWeather(builder, obs, hints));
        result.addIssue(setWindShears(builder, obs, hints));
        result.addIssue(setSeaState(builder, obs, hints));
        result.addIssue(setRunwayStates(builder, obs, hints));
        result.addIssue(setColorState(builder, obs, hints));

        if (subSequences.size() > 0) {
            for (int i = 1; i < subSequences.size(); i++) {
                LexemeSequence seq = subSequences.get(i);
                if (Identity.TREND_CHANGE_INDICATOR == seq.getFirstLexeme().getIdentity()) {
                    result.addIssue(addToTrends(builder, seq.getFirstLexeme(), hints));
                } else if (Identity.REMARKS_START == seq.getFirstLexeme().getIdentity()) {
                    List<String> remarks = getRemarks(seq.getFirstLexeme(), hints);
                    if (!remarks.isEmpty()) {
                        builder.setRemarks(remarks);
                    }
                }
            }
        }
        result.setConvertedMessage(buildUsing(builder));
        return result;
    }

    protected abstract Identity getExpectedFirstTokenIdentity();

    protected abstract T buildUsing(final METARImpl.Builder builder);


    private List<ConversionIssue> setMETARIssueTime(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        Identity[] before = { Identity.ROUTINE_DELAYED_OBSERVATION, Identity.NIL, Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY,
                Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER,
                Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
        retval.addAll(withFoundIssueTime(lexed, before, hints, builder::setIssueTime));
        return retval;
    }
    private static List<ConversionIssue> setObservedSurfaceWind(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        findNext(Identity.SURFACE_WIND, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD,
                    Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE,
                    Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
            } else {
                Object direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
                Integer meanSpeed = match.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
                Integer gust = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

                final ObservedSurfaceWindImpl.Builder wind = new ObservedSurfaceWindImpl.Builder();

                if (direction == SurfaceWind.WindDirection.VARIABLE) {
                    wind.setVariableDirection(true);
                } else if (direction instanceof Integer) {
                    wind.setMeanWindDirection(NumericMeasureImpl.of((Integer) direction,"deg"));
                } else {
                    retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Direction missing for surface wind:" + match.getTACToken()));
                }

                if (meanSpeed != null) {
                    wind.setMeanWindSpeed(NumericMeasureImpl.of(meanSpeed,unit));
                } else {
                    retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean speed missing for surface wind:" + match.getTACToken()));
                }

                if (gust != null) {
                    wind.setWindGust(NumericMeasureImpl.of(gust,unit));
                }

                findNext(Identity.VARIABLE_WIND_DIRECTION, match, (varMatch) -> {
                    ConversionIssue varIssue = checkBeforeAnyOf(varMatch, before);
                    if (varIssue != null) {
                        retval.add(varIssue);
                    } else {
                        Integer maxDirection = varMatch.getParsedValue(Lexeme.ParsedValueName.MAX_DIRECTION, Integer.class);
                        Integer minDirection = varMatch.getParsedValue(Lexeme.ParsedValueName.MIN_DIRECTION, Integer.class);

                        if (minDirection != null) {
                            wind.setExtremeCounterClockwiseWindDirection(NumericMeasureImpl.of(minDirection,"deg"));
                        }
                        if (maxDirection != null) {
                            wind.setExtremeClockwiseWindDirection(NumericMeasureImpl.of(maxDirection, "deg"));
                        }
                    }
                });
                builder.setSurfaceWind(wind.build());
            }
        }, () -> {
            //TODO: cases where it's ok to be missing the surface wind
            retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing surface wind information in " + lexed.getTAC()));
        });
        return retval;
    }

    private static List<ConversionIssue> setHorizontalVisibilities(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        findNext(Identity.HORIZONTAL_VISIBILITY, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.RUNWAY_VISUAL_RANGE, Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH,
                    Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                    Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            HorizontalVisibilityImpl.Builder vis = new HorizontalVisibilityImpl.Builder();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    retval.add(issue);
                } else {
                    if (builder.isCeilingAndVisibilityOk()) {
                        retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with prevailing visibility"));
                        break;
                    }
                    MetricHorizontalVisibility.DirectionValue direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION,
                            MetricHorizontalVisibility.DirectionValue.class);
                    String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                    Double value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
                    RelationalOperator operator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                            RelationalOperator.class);
                    if (direction != null) {
                        if (vis.getMinimumVisibility() != null) {
                            retval.add(
                                    new ConversionIssue(Type.LOGICAL_ERROR, "More than one directional horizontal visibility given: " + match.getTACToken()));
                        } else {
                            vis.setMinimumVisibility(NumericMeasureImpl.of(value,unit));
                            vis.setMinimumVisibilityDirection(NumericMeasureImpl.of(direction.inDegrees(),"deg"));
                        }
                    } else {
                        if (vis.getPrevailingVisibility() != null) {
                            retval.add(
                                    new ConversionIssue(Type.LOGICAL_ERROR, "More than one prevailing horizontal visibility given: " + match.getTACToken()));
                        } else {
                            vis.setPrevailingVisibility(NumericMeasureImpl.of(value,unit));
                            if (RelationalOperator.LESS_THAN == operator) {
                                vis.setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RelationalOperator.MORE_THAN == operator) {
                                vis.setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }
                        }
                    }
                    builder.setVisibility(vis.build());
                }
                match = findNext(Identity.HORIZONTAL_VISIBILITY, match);
            }
        }, () -> {
            // If no horizontal visibility and no CAVOK
            if (!builder.isCeilingAndVisibilityOk()) {
                retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing horizontal visibility / cavok in " + lexed.getTAC()));
            }
        });
        return retval;
    }

    private static List<ConversionIssue> setRVRs(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();

        findNext(Identity.RUNWAY_VISUAL_RANGE, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR,
                    Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            List<RunwayVisualRange> rvrs = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    retval.add(issue);
                } else {
                    if (builder.isCeilingAndVisibilityOk()) {
                        retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with runway visual range"));
                        break;
                    }
                    String rwCode = match.getParsedValue(Lexeme.ParsedValueName.RUNWAY, String.class);
                    if (rwCode == null) {
                        retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing runway code for RVR in " + match.getTACToken()));
                    } else {
                        RunwayDirectionImpl.Builder runway = new RunwayDirectionImpl.Builder();
                        runway.setDesignator(rwCode);
                        runway.setAssociatedAirportHeliport(builder.getAerodrome());

                        Integer minValue = match.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Integer.class);
                        RelationalOperator minValueOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                                RelationalOperator.class);
                        Integer maxValue = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                        RelationalOperator maxValueOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR2,
                                RelationalOperator.class);
                        TendencyOperator tendencyIndicator = match.getParsedValue(Lexeme.ParsedValueName.TENDENCY_OPERATOR,
                                TendencyOperator.class);
                        String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

                        if (minValue == null) {
                            retval.add(
                                    new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing visibility value for RVR in " + match.getTACToken()));
                        }
                        RunwayVisualRangeImpl.Builder rvr = new RunwayVisualRangeImpl.Builder();
                        rvr.setRunwayDirection(runway.build());
                        if (maxValue != null && minValue != null) {
                            rvr.setVaryingRVRMinimum(NumericMeasureImpl.of(minValue, unit));
                            if (RelationalOperator.LESS_THAN == minValueOperator) {
                                rvr.setVaryingRVRMinimumOperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RelationalOperator.MORE_THAN == minValueOperator) {
                                rvr.setVaryingRVRMinimumOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }

                            rvr.setVaryingRVRMaximum(NumericMeasureImpl.of(maxValue, unit));
                            if (RelationalOperator.LESS_THAN == maxValueOperator) {
                                rvr.setVaryingRVRMaximumOperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RelationalOperator.MORE_THAN == maxValueOperator) {
                                rvr.setVaryingRVRMaximumOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }
                        } else if (minValue != null) {
                            rvr.setMeanRVR( NumericMeasureImpl.of(minValue, unit));
                            if (RelationalOperator.LESS_THAN == minValueOperator) {
                                rvr.setMeanRVROperator(AviationCodeListUser.RelationalOperator.BELOW);
                            } else if (RelationalOperator.MORE_THAN == minValueOperator) {
                                rvr.setMeanRVROperator(AviationCodeListUser.RelationalOperator.ABOVE);
                            }
                        }
                        if (TendencyOperator.DOWNWARD == tendencyIndicator) {
                            rvr.setPastTendency(AviationCodeListUser.VisualRangeTendency.DOWNWARD);
                        } else if (TendencyOperator.UPWARD == tendencyIndicator) {
                            rvr.setPastTendency(AviationCodeListUser.VisualRangeTendency.UPWARD);
                        } else if (TendencyOperator.NO_CHANGE == tendencyIndicator) {
                            rvr.setPastTendency(AviationCodeListUser.VisualRangeTendency.NO_CHANGE);
                        }
                        rvrs.add(rvr.build());
                        builder.setRunwayVisualRanges(rvrs);
                    }
                    match = findNext(Identity.RUNWAY_VISUAL_RANGE, match);

                }
            }
        });
        return retval;
    }

    private static List<ConversionIssue> setPresentWeather(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();

        findNext(Identity.WEATHER, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.CLOUD, Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR,
                    Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
            } else {
                if (builder.isCeilingAndVisibilityOk()) {
                    retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with prevailing visibility"));
                } else {
                    List<fi.fmi.avi.model.Weather> weather = new ArrayList<>();
                    retval.addAll(appendWeatherCodes(match, weather, before, hints));
                    if (!weather.isEmpty()) {
                        builder.setPresentWeather(weather);
                    }
                }
            }
        });
        return retval;
    }

    private static List<ConversionIssue> setObservedClouds(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();

        findNext(Identity.CLOUD, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.AIR_DEWPOINT_TEMPERATURE, Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR,
                    Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ObservedCloudsImpl.Builder clouds = new ObservedCloudsImpl.Builder();
            ConversionIssue issue;
            List<fi.fmi.avi.model.CloudLayer> layers = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    retval.add(issue);
                } else {
                    if (builder.isCeilingAndVisibilityOk()) {
                        retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with prevailing visibility"));
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
                            clouds.setVerticalVisibility(NumericMeasureImpl.of(height, unit));
                        } else {
                            fi.fmi.avi.model.CloudLayer layer = getCloudLayer(match);
                            if (layer != null) {
                                layers.add(layer);
                            } else {
                                retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "Could not parse token " + match.getTACToken() + " as cloud layer"));
                            }
                        }
                    } else {
                        retval.add(
                                new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Cloud layer height is not an integer in " + match.getTACToken()));
                    }
                }
                match = findNext(Identity.CLOUD, match);
            }
            if (!layers.isEmpty()) {
                clouds.setLayers(layers);
            }
            builder.setClouds(clouds.build());
        });
        return retval;
    }

    private static List<ConversionIssue> setTemperatures(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();

        findNext(Identity.AIR_DEWPOINT_TEMPERATURE, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.AIR_PRESSURE_QNH, Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE,
                    Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
            } else {
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                Double[] values = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Double[].class);
                if (values == null) {
                    retval.add(
                            new ConversionIssue(Type.MISSING_DATA, "Missing air temperature and dewpoint temperature values in " + match.getTACToken()));
                } else {
                    if (values[0] != null) {
                        builder.setAirTemperature(NumericMeasureImpl.of(values[0], unit));
                    } else {
                        retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "Missing air temperature value in " + match.getTACToken()));
                    }
                    if (values[1] != null) {
                        builder.setDewpointTemperature(NumericMeasureImpl.of(values[1], unit));
                    } else {
                        retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "Missing dewpoint temperature value in " + match.getTACToken()));
                    }
                }
            }
        }, () -> retval.add(new ConversionIssue(Type.MISSING_DATA, "Missing air temperature and dewpoint temperature values in " + lexed.getTAC())));

        return retval;
    }

    private static List<ConversionIssue> setQNH(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();

        findNext(Identity.AIR_PRESSURE_QNH, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.RECENT_WEATHER, Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE,
                    Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
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
                        retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                                "Unknown unit for air pressure: " + unitStr + " in " + match.getTACToken()));
                    }
                    builder.setAltimeterSettingQNH(NumericMeasureImpl.of(value, unitStr));
                } else {
                    retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing air pressure value: " + match.getTACToken()));
                }
            }
        }, () -> retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "QNH missing in " + lexed.getTAC())));

        return retval;
    }

    private static List<ConversionIssue> setRecentWeather(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        findNext(Identity.RECENT_WEATHER, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.WIND_SHEAR, Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR,
                    Identity.REMARKS_START };
            List<fi.fmi.avi.model.Weather> weather = new ArrayList<>();
            retval.addAll(appendWeatherCodes(match, weather, before, hints));
            if (!weather.isEmpty()) {
                builder.setRecentWeather(weather);
            }
        });
        return retval;
    }

    private static List<ConversionIssue> setWindShears(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        findNext(Identity.WIND_SHEAR, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.SEA_STATE, Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            final WindShearImpl.Builder ws = new WindShearImpl.Builder();
            List<RunwayDirection> runways = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    retval.add(issue);
                } else {
                    String rw = match.getParsedValue(Lexeme.ParsedValueName.RUNWAY, String.class);
                    if ("ALL".equals(rw)) {
                        if (!runways.isEmpty()) {
                            retval.add(new ConversionIssue(Type.LOGICAL_ERROR,
                                    "Wind shear reported both to all runways and at least one specific runway: " + match.getTACToken()));
                        } else {
                            ws.setAppliedToAllRunways(true);
                        }
                    } else if (rw != null) {
                        if (ws.isAppliedToAllRunways()) {
                            retval.add(new ConversionIssue(Type.LOGICAL_ERROR,
                                    "Wind shear reported both to all runways and at least one specific runway:" + match.getTACToken()));
                        } else {
                            runways.add(new RunwayDirectionImpl.Builder()
                                    .setDesignator(rw)
                                    .setAssociatedAirportHeliport(builder.getAerodrome())
                                    .build());
                        }
                    }
                }
                match = findNext(Identity.WIND_SHEAR, match);
            }
            if (!runways.isEmpty()) {
                ws.setRunwayDirections(runways);
            }
            builder.setWindShear(ws.build());
        });
        return retval;
    }

    private static List<ConversionIssue> setSeaState(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        findNext(Identity.SEA_STATE, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Identity.RUNWAY_STATE, Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
            } else {
                SeaStateImpl.Builder ss = new SeaStateImpl.Builder();
                Object[] values = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Object[].class);
                if (values[0] instanceof Integer) {
                    String tempUnit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                    ss.setSeaSurfaceTemperature(NumericMeasureImpl.of((Integer) values[0], tempUnit));
                }
                if (values[1] instanceof fi.fmi.avi.converter.tac.lexer.impl.token.SeaState.SeaSurfaceState) {
                    if (values[2] != null) {
                        retval.add(new ConversionIssue(Type.LOGICAL_ERROR,
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
                        retval.add(new ConversionIssue(Type.LOGICAL_ERROR,
                                "Sea state cannot contain both sea surface state and significant wave height:" + match.getTACToken()));
                    } else {
                        String heightUnit = match.getParsedValue(Lexeme.ParsedValueName.UNIT2, String.class);
                        ss.setSignificantWaveHeight(NumericMeasureImpl.of(((Number) values[2]).doubleValue(), heightUnit));
                    }
                }
                builder.setSeaState(ss.build());
            }
        });
        return retval;
    }

    private static List<ConversionIssue> setRunwayStates(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();

        findNext(Identity.RUNWAY_STATE, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Identity.COLOR_CODE, Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue;
            List<RunwayState> states = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    retval.add(issue);
                    match = findNext(Identity.RUNWAY_STATE, match);
                    continue;
                }
                RunwayStateImpl.Builder rws = new RunwayStateImpl.Builder();
                @SuppressWarnings("unchecked")
                Map<RunwayStateReportType, Object> values = match.getParsedValue(ParsedValueName.VALUE, Map.class);

                Boolean repetition = (Boolean) values.get(RunwayStateReportType.REPETITION);
                Boolean allRunways = (Boolean) values.get(RunwayStateReportType.ALL_RUNWAYS);

                String runwayDesignator = match.getParsedValue(ParsedValueName.RUNWAY, String.class);

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
                    rws.setAppliedToAllRunways(true);
                } else if (snowClosure != null && snowClosure.booleanValue()) {
                    rws.setAppliedToAllRunways(true);
                    rws.setSnowClosure(true);
                } else if (runwayDesignator != null) {
                    rws.setRunwayDirection(new RunwayDirectionImpl.Builder()
                            .setDesignator(runwayDesignator)
                            .setAssociatedAirportHeliport(builder.getAerodrome())
                            .build());
                } else {
                    retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "No runway specified for runway state report: " + match.getTACToken()));
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
                        retval.add(
                                new ConversionIssue(Type.LOGICAL_ERROR, "Missing deposit kind but depth given for runway state: " + match.getTACToken()));
                    } else {
                        rws.setDepthOfDeposit(NumericMeasureImpl.of(depthOfDeposit, unitOfDeposit));
                    }
                }

                if (depthModifier != null) {
                    if (depthOfDeposit == null && depthModifier == RunwayStateReportSpecialValue.NOT_MEASURABLE) {
                        rws.setDepthNotMeasurable(true);
                        rws.setDepthOfDeposit(Optional.empty());
                    } else if (depthOfDeposit == null && depthModifier != RunwayStateReportSpecialValue.RUNWAY_NOT_OPERATIONAL) {
                        retval.add(new ConversionIssue(Type.LOGICAL_ERROR,
                                "Missing deposit depth but depth modifier given for runway state: " + match.getTACToken()));
                    } else {
                        switch (depthModifier) {
                            case LESS_THAN_OR_EQUAL:
                                rws.setDepthOperator(AviationCodeListUser.RelationalOperator.BELOW);
                                break;
                            case MEASUREMENT_UNRELIABLE:
                            case NOT_MEASURABLE:
                                retval.add(new ConversionIssue(Type.SYNTAX_ERROR,
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
                        retval.add(new ConversionIssue(Type.LOGICAL_ERROR,
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

                if (frictionCoefficient instanceof Number) {
                    rws.setEstimatedSurfaceFriction(((Number) frictionCoefficient).doubleValue());
                } else if (frictionCoefficient == RunwayStateReportSpecialValue.MEASUREMENT_UNRELIABLE) {
                    rws.setEstimatedSurfaceFrictionUnreliable(true);
                }

                states.add(rws.build());
                match = findNext(Identity.RUNWAY_STATE, match);
            }
            if (!states.isEmpty()) {
                builder.setRunwayStates(states);
            }
        });
        return retval;
    }

    private static List<ConversionIssue> setColorState(final METARImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        findNext(Identity.COLOR_CODE, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Identity.TREND_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
            } else {
                ColorCode.ColorState code = match.getParsedValue(ParsedValueName.VALUE, ColorCode.ColorState.class);
                for (AviationCodeListUser.ColorState state : AviationCodeListUser.ColorState.values()) {
                    if (state.name().equalsIgnoreCase(code.getCode())) {
                        builder.setColorState(state);
                    }
                }
                if (builder.getColorState() == null) {
                    retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "Unknown color state '" + code.getCode() + "'"));
                }
            }
        });
        return retval;
    }

    private static List<ConversionIssue> addToTrends(final METARImpl.Builder builder, final Lexeme changeFctToken, final ConversionHints hints) {
        if (Identity.TREND_CHANGE_INDICATOR != changeFctToken.getIdentity()) {
            throw new IllegalArgumentException("Cannot update Trend, the start lexeme " + changeFctToken + " is not a change forecast start token");
        }
        List<ConversionIssue> retval = new ArrayList<>();
        ConversionIssue issue = checkBeforeAnyOf(changeFctToken, new Identity[] { Identity.REMARKS_START });
        if (issue != null) {
            retval.add(issue);
            return retval;
        }

        List<TrendForecast> trends;
        if (builder.getTrends().isPresent()) {
            trends = builder.getTrends().get();
        } else {
            trends = new ArrayList<>();
        }

        TrendForecastImpl.Builder fct = new TrendForecastImpl.Builder();
        TrendChangeIndicatorType type = changeFctToken.getParsedValue(ParsedValueName.TYPE, TrendChangeIndicatorType.class);
        switch (type) {
            case BECOMING:
                fct.setChangeIndicator(AviationCodeListUser.TrendForecastChangeIndicator.BECOMING);
                retval.addAll(setTrendContents(fct, changeFctToken, hints));
                break;
            case TEMPORARY_FLUCTUATIONS:
                fct.setChangeIndicator(AviationCodeListUser.TrendForecastChangeIndicator.TEMPORARY_FLUCTUATIONS);
                retval.addAll(setTrendContents(fct, changeFctToken, hints));
                break;
            case NO_SIGNIFICANT_CHANGES:
                fct.setChangeIndicator(AviationCodeListUser.TrendForecastChangeIndicator.NO_SIGNIFICANT_CHANGES);
                break;
            default:
                break;
        }
        trends.add(fct.build());
        builder.setTrends(trends);
        return retval;
    }


    private static List<ConversionIssue> setTrendContents(final TrendForecastImpl.Builder fctBuilder, final Lexeme groupStart,
            final ConversionHints hints) {

        List<ConversionIssue> retval = new ArrayList<>();
        //Check for the possibly following FM, TL and AT tokens:
        Lexeme token = parseChangeTimeGroups(fctBuilder, groupStart.getNext(), retval, hints);

        //loop over change group tokens:
        Lexeme.Identity[] before = { Identity.REMARKS_START, Identity.END_TOKEN };
        while (token != null) {
            if (checkBeforeAnyOf(token, before) != null) {
                break;
            }
            Identity id = token.getIdentity();
            if (id != null) {
                switch (id) {
                    case CAVOK:
                        fctBuilder.setCeilingAndVisibilityOk(true);
                        break;
                    case CLOUD: {
                        retval.addAll(addToForecastClouds(fctBuilder, token, hints));
                        break;
                    }
                    case HORIZONTAL_VISIBILITY: {
                        if (!fctBuilder.getPrevailingVisibility().isPresent()) {
                            retval.addAll(setPrevailingVisibility(fctBuilder, token, hints));
                        } else {
                            retval.add(new ConversionIssue(Type.SYNTAX_ERROR,
                                    "More than one visibility token within a trend change group: " + token.getTACToken()));
                        }
                        break;
                    }
                    case SURFACE_WIND: {
                        if (!fctBuilder.getSurfaceWind().isPresent()) {
                            retval.addAll(setForecastWind(fctBuilder, token, hints));
                        } else {
                            retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "More than one wind token within a trend change group"));
                        }
                        break;
                    }
                    case WEATHER: {
                        List<fi.fmi.avi.model.Weather> forecastWeather;
                        if (fctBuilder.getForecastWeather().isPresent()) {
                            forecastWeather = fctBuilder.getForecastWeather().get();
                        } else {
                            forecastWeather = new ArrayList<>();
                        }
                        String code = token.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class);
                        if (code != null) {
                            forecastWeather.add(new WeatherImpl.Builder()
                                    .setCode(code)
                                    .setDescription(Weather.WEATHER_CODES.get(code))
                                    .build());
                            fctBuilder.setForecastWeather(forecastWeather);
                        } else {
                            retval.add(new ConversionIssue(Type.MISSING_DATA, "Weather code not found"));
                        }
                        break;
                    }
                    case NO_SIGNIFICANT_WEATHER:
                        fctBuilder.setNoSignificantWeather(true);
                        break;
                    case COLOR_CODE: {
                        ColorCode.ColorState code = token.getParsedValue(ParsedValueName.VALUE, ColorCode.ColorState.class);
                        for (AviationCodeListUser.ColorState state : AviationCodeListUser.ColorState.values()) {
                            if (state.name().equalsIgnoreCase(code.getCode())) {
                                fctBuilder.setColorState(state);
                            }
                        }
                        if (!fctBuilder.getColorState().isPresent()) {
                            retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "Unknown color state '" + code.getCode() + "'"));
                        }
                        break;
                    }
                    case END_TOKEN: {
                        break;
                    }
                    default:
                        retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "Illegal token " + token.getTACToken() + " within the change forecast group"));
                        break;
                }
            }
            if (Identity.END_TOKEN == token.getIdentity()) {
                break;
            }
            token = token.getNext();
        }
        if (fctBuilder.getCloud().isPresent()) {
            if (fctBuilder.getCloud().get().getLayers().isPresent() && !fctBuilder.getCloud().get().getLayers().get().isEmpty()) {
                if (fctBuilder.getCloud().get().isNoSignificantCloud()) {
                    retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "Cloud layers cannot co-exist with NSC in trend"));
                } else if (fctBuilder.getCloud().get().getVerticalVisibility() != null) {
                    retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "Cloud layers cannot co-exist with vertical visibility in trend"));
                }
            }
        }

        if (fctBuilder.isCeilingAndVisibilityOk()) {
            if (fctBuilder.getCloud().isPresent() || fctBuilder.getPrevailingVisibility().isPresent() || fctBuilder.getForecastWeather().isPresent() || fctBuilder.isNoSignificantWeather()) {
                retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "CAVOK cannot co-exist with cloud, prevailing visibility, weather, NSW in trend"));
            }
        } else {
            if (fctBuilder.isNoSignificantWeather() && fctBuilder.getForecastWeather().isPresent() && !fctBuilder.getForecastWeather().get().isEmpty()) {
                retval.add(new ConversionIssue(Type.LOGICAL_ERROR, "Forecast weather cannot co-exist with NSW in trend"));
            }
        }
        return retval;
    }

    private static Lexeme parseChangeTimeGroups(final TrendForecastImpl.Builder builder, final Lexeme token, final List<ConversionIssue> issues, final ConversionHints hints) {
        Lexeme t = token;
        while (t != null && Identity.TREND_TIME_GROUP == t.getIdentity()) {
            TrendTimePeriodType type = t.getParsedValue(ParsedValueName.TYPE, TrendTimePeriodType.class);
            if (type != null) {
                switch (type) {
                    case AT: {
                        if (builder.getInstantOfChange().isPresent()) {
                            issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "More than one AT token in the same trend forecast"));
                            return t;
                        }
                        Integer fromHour = t.getParsedValue(ParsedValueName.HOUR1, Integer.class);
                        Integer fromMinute = t.getParsedValue(ParsedValueName.MINUTE1, Integer.class);
                        if (fromHour != null && fromMinute != null) {
                            builder.setInstantOfChange(new PartialOrCompleteTimeInstant.Builder()
                                    .setPartialTime(String.format("%02d%02d", fromHour, fromMinute))
                                    .setPartialTimePattern(PartialOrCompleteTimeInstant.HOUR_MINUTE_PATTERN)
                                    .build());
                        } else {
                            issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "Missing hour and/or minute from trend AT group " + token.getTACToken()));
                        }
                        break;
                    }
                    case FROM: {
                        if (builder.getInstantOfChange().isPresent()) {
                            issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "FM token found after AT token in the same trend forecast"));
                            return t;
                        }
                        Optional<PartialOrCompleteTimePeriod> period = builder.getPeriodOfChange();
                        Integer fromHour = t.getParsedValue(ParsedValueName.HOUR1, Integer.class);
                        Integer fromMinute = t.getParsedValue(ParsedValueName.MINUTE1, Integer.class);
                        if (fromHour != null && fromMinute != null) {
                            PartialOrCompleteTimePeriod.Builder pBuilder;
                            if (period.isPresent()) {
                                if (period.get().getStartTime().isPresent()) {
                                    issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "More than one FM token in the same trend forecast"));
                                    return t;
                                }
                                pBuilder = period.get().toBuilder();
                            } else {
                                pBuilder = new PartialOrCompleteTimePeriod.Builder();
                            }
                            pBuilder.setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                .setPartialTime(String.format("%02d%02d", fromHour, fromMinute))
                                .setPartialTimePattern(PartialOrCompleteTimeInstant.HOUR_MINUTE_PATTERN)
                                .build());
                            builder.setPeriodOfChange(pBuilder.build());
                        } else {
                            issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "Missing hour and/or minute from trend FM group " + token.getTACToken()));
                        }
                        break;
                    }
                    case UNTIL: {
                        if (builder.getInstantOfChange().isPresent()) {
                            issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "TL token found after AT token in the same trend forecast"));
                            return t;
                        }
                        Optional<PartialOrCompleteTimePeriod> period = builder.getPeriodOfChange();
                        Integer toHour = t.getParsedValue(ParsedValueName.HOUR1, Integer.class);
                        Integer toMinute = t.getParsedValue(ParsedValueName.MINUTE1, Integer.class);
                        if (toHour != null && toMinute != null) {
                            PartialOrCompleteTimePeriod.Builder pBuilder;
                            if (period.isPresent()) {
                                if (period.get().getEndTime().isPresent()) {
                                    issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "More than one TL token in the same trend forecast"));
                                    return t;
                                }
                                pBuilder = period.get().toBuilder();
                            } else {
                                pBuilder = new PartialOrCompleteTimePeriod.Builder();
                            }
                            pBuilder.setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                    .setPartialTime(String.format("%02d%02d", toHour, toMinute))
                                    .setPartialTimePattern(PartialOrCompleteTimeInstant.HOUR_MINUTE_PATTERN)
                                    .build());
                            builder.setPeriodOfChange(pBuilder.build());
                        } else {
                            issues.add(new ConversionIssue(Type.SYNTAX_ERROR, "Missing hour and/or minute from trend TL group " + token.getTACToken()));
                        }
                        break;
                    }
                    default: {
                        issues.add(
                                new ConversionIssue(Type.SYNTAX_ERROR, "Illegal change group '" + token.getTACToken() + "' after change group start token"));
                        break;
                    }
                }

            }
            t = t.getNext();
        }
        return t;
    }


    private static List<ConversionIssue> addToForecastClouds(final TrendForecastImpl.Builder fctBuilder, final Lexeme token,
            final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        CloudForecastImpl.Builder cloudBuilder;
        if (fctBuilder.getCloud().isPresent()) {
            cloudBuilder = CloudForecastImpl.Builder.from(fctBuilder.getCloud().get());
        } else {
            cloudBuilder = new CloudForecastImpl.Builder();
        }

        List<fi.fmi.avi.model.CloudLayer> cloudLayers;
        if (cloudBuilder.getLayers().isPresent()) {
            cloudLayers = cloudBuilder.getLayers().get();
        } else {
            cloudLayers = new ArrayList<>();
        }
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
                cloudBuilder.setVerticalVisibility(NumericMeasureImpl.of(height, unit));
            } else {
                retval.add(new ConversionIssue(Type.MISSING_DATA, "Missing value for vertical visibility"));
            }
        } else if (CloudCover.NO_SIG_CLOUDS == cover) {
            cloudBuilder.setNoSignificantCloud(true);
        } else {
            fi.fmi.avi.model.CloudLayer layer = getCloudLayer(token);
            if (layer != null) {
                cloudLayers.add(layer);
                cloudBuilder.setLayers(cloudLayers);
            } else {
                retval.add(new ConversionIssue(Type.MISSING_DATA, "Missing base for cloud layer"));
            }
        }
        fctBuilder.setCloud(cloudBuilder.build());
        return retval;
    }

    private static List<ConversionIssue> setPrevailingVisibility(final TrendForecastImpl.Builder fctBuilder, final Lexeme token, final ConversionHints hints) {
        String unit = token.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
        Double value = token.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
        RelationalOperator operator = token.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                RelationalOperator.class);
        NumericMeasure prevailingVisibility = NumericMeasureImpl.of(value, unit);
        AviationCodeListUser.RelationalOperator visibilityOperator = null;
        if (RelationalOperator.LESS_THAN == operator) {
            visibilityOperator = AviationCodeListUser.RelationalOperator.BELOW;
        } else if (RelationalOperator.MORE_THAN == operator) {
            visibilityOperator = AviationCodeListUser.RelationalOperator.ABOVE;
        }
        fctBuilder.setPrevailingVisibility(prevailingVisibility);
        if (visibilityOperator != null) {
            fctBuilder.setPrevailingVisibilityOperator(visibilityOperator);
        }

        return Collections.emptyList();
    }

    private static List<ConversionIssue> setForecastWind(final TrendForecastImpl.Builder fctBuilder, final Lexeme token,
            final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        TrendForecastSurfaceWindImpl.Builder wind = new TrendForecastSurfaceWindImpl.Builder();
        Object direction = token.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
        Integer meanSpeed = token.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
        Integer gust = token.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
        String unit = token.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

        if (direction == SurfaceWind.WindDirection.VARIABLE) {
            retval.add(new ConversionIssue(Type.SYNTAX_ERROR, "Wind cannot be variable in trend: " + token.getTACToken()));
        } else if (direction instanceof Integer) {
            wind.setMeanWindDirection(NumericMeasureImpl.of((Integer) direction, "deg"));
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Direction missing for surface wind:" + token.getTACToken()));
        }

        if (meanSpeed != null) {
            wind.setMeanWindSpeed(NumericMeasureImpl.of(meanSpeed, unit));
        } else {
            retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Mean speed missing for surface wind:" + token.getTACToken()));
        }

        if (gust != null) {
            wind.setWindGust(NumericMeasureImpl.of(gust, unit));
        }
        fctBuilder.setSurfaceWind(wind.build());
        return retval;
    }

}
