package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetricHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.SurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFForecastChangeIndicator;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFSurfaceWind;
import fi.fmi.avi.model.taf.immutable.*;


/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class TAFTACParser extends AbstractTACParser<TAF> {

    private static Identity[] zeroOrOneAllowed = { Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.VALID_TIME, Identity.CORRECTION, Identity.AMENDMENT, Identity.CANCELLATION, Identity.NIL, Identity.MIN_TEMPERATURE,
            Identity.MAX_TEMPERATURE, Identity.REMARKS_START };

    private AviMessageLexer lexer;

    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public ConversionResult<TAF> convertMessage(final String input, final ConversionHints hints) {
        ConversionResult<TAF> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (!lexingSuccessful(lexed, hints)) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Input message lexing was not fully successful: " + lexed));
            List<Lexeme> errors = lexed.getLexemes().stream().filter(l -> !Lexeme.Status.OK.equals(l.getStatus())).collect(Collectors.toList());
            for (Lexeme l:errors) {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Lexing problem with '" + l.getTACToken() + "': " + l.getLexerMessage
                        ()));
            }
            return result;
        }

        if (Identity.TAF_START != lexed.getFirstLexeme().getIdentityIfAcceptable()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "The input message is not recognized as TAF"));
            return result;
        }

        if (!endsInEndToken(lexed, hints)) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Message does not end in end token"));
            return result;
        }
        List<ConversionIssue> issues = checkZeroOrOne(lexed, zeroOrOneAllowed);
        if (!issues.isEmpty()) {
            result.addIssue(issues);
            return result;
        }
        TAFImpl.Builder builder = new TAFImpl.Builder();

        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }

        withTimeForTranslation(hints, builder::setTranslationTime);

        //Split & filter in the sequences starting with FORECAST_CHANGE_INDICATOR:
        List<LexemeSequence> subSequences = lexed.splitBy(Identity.TAF_FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START);

        findNext(Identity.CORRECTION, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION,
                    Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE,
                    Identity.MAX_TEMPERATURE, Identity.TAF_FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setStatus(AviationCodeListUser.TAFStatus.CORRECTION);
            }
        });

        findNext(Identity.AMENDMENT, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = { Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION,
                    Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE,
                    Identity.MAX_TEMPERATURE, Identity.TAF_FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                TAF.TAFStatus status = builder.getStatus();
                if (status != null) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                            "TAF cannot be both " + TAF.TAFStatus.AMENDMENT + " and " + status + " at " + "the same time"));
                } else {
                    builder.setStatus(AviationCodeListUser.TAFStatus.AMENDMENT);
                }
            }
        });

        if (builder.getStatus() == null) {
            builder.setStatus(AviationCodeListUser.TAFStatus.NORMAL);
        }

        findNext(Identity.AERODROME_DESIGNATOR, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = new Identity[] { Identity.ISSUE_TIME, Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION, Identity.SURFACE_WIND,
                    Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE,
                    Identity.TAF_FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setAerodrome(new AerodromeImpl.Builder()
                        .setDesignator(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class))
                        .build());
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Aerodrome designator not given in " + input)));

        result.addIssue(setTAFIssueTime(builder, lexed, hints));

        findNext(Identity.NIL, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = new Identity[] { Identity.VALID_TIME, Identity.CANCELLATION, Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY,
                    Identity.WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE,
                    Identity.TAF_FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setStatus(AviationCodeListUser.TAFStatus.MISSING);
                if (match.getNext() != null) {
                    Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                    if (Identity.END_TOKEN != nextTokenId && Identity.REMARKS_START != nextTokenId) {
                        result.addIssue(
                                new ConversionIssue(ConversionIssue.Type.LOGICAL_ERROR, "Missing TAF message contains extra tokens after NIL: " + input));
                    }
                }
            }
        });

        for (int i = 1; i < subSequences.size(); i++) {
            LexemeSequence seq = subSequences.get(i);
            if (Identity.REMARKS_START == seq.getFirstLexeme().getIdentity()) {
                List<String> remarks = getRemarks(seq.getFirstLexeme(), hints);
                if (!remarks.isEmpty()) {
                    builder.setRemarks(remarks);
                }
            }
        }

        //End processing here if NIL:
        if (AviationCodeListUser.TAFStatus.MISSING == builder.getStatus()) {
            return result;
        }

        result.addIssue(setTAFValidTime(builder, lexed, hints));

        findNext(Identity.CANCELLATION, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = new Identity[] { Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK,
                    Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE, Identity.TAF_FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setStatus(AviationCodeListUser.TAFStatus.CANCELLATION);
                if (match.getNext() != null) {
                    Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                    if (Identity.END_TOKEN != nextTokenId && Identity.REMARKS_START != nextTokenId) {
                        result.addIssue(
                                new ConversionIssue(ConversionIssue.Type.LOGICAL_ERROR, "Cancelled TAF message contains extra tokens after CNL: " + input));
                    }
                }
            }
        });

        //End processing here if CNL:
        if (AviationCodeListUser.TAFStatus.CANCELLATION == builder.getStatus()) {
            return result;
        }

        //Should always return at least one as long as lexed is not empty, the first one is the base forecast:
        result.addIssue(setBaseForecast(builder, subSequences.get(0).getFirstLexeme(), hints));
        for (int i = 1; i < subSequences.size(); i++) {
            LexemeSequence seq = subSequences.get(i);
            if (Identity.TAF_FORECAST_CHANGE_INDICATOR == seq.getFirstLexeme().getIdentity()) {
                result.addIssue(addChangeForecast(builder, subSequences.get(i).getFirstLexeme(), hints));
            }
        }

        result.setConvertedMessage(builder.build());
        return result;
    }

    private List<ConversionIssue> setTAFIssueTime(final TAFImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        Identity[] before = new Identity[] { Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION, Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY,
                Identity.WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE, Identity.TAF_FORECAST_CHANGE_INDICATOR,
                Identity.REMARKS_START };
        retval.addAll(withFoundIssueTime(lexed, before, hints, builder::setIssueTime));
        return retval;
    }

    private List<ConversionIssue> setTAFValidTime(final TAFImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.VALID_TIME, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = new Identity[] { Identity.CANCELLATION, Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD,
                    Identity.CAVOK, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE, Identity.TAF_FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                Integer startDay = match.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                Integer endDay = match.getParsedValue(Lexeme.ParsedValueName.DAY2, Integer.class);
                Integer startHour = match.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                Integer endHour = match.getParsedValue(Lexeme.ParsedValueName.HOUR2, Integer.class);
                if (startDay != null && startHour != null && endHour != null) {
                    if (endDay != null) {
                        builder.setValidityTime(new PartialOrCompleteTimePeriod.Builder()
                                .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                        .setPartialTime(String.format("%02d%02d", startDay, startHour))
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                        .build())
                                .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                        .setPartialTime(String.format("%02d%02d", endDay, endHour))
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                        .build())
                                .build());
                    } else {
                        builder.setValidityTime(new PartialOrCompleteTimePeriod.Builder()
                                .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                        .setPartialTime(String.format("%02d%02d", startDay, startHour))
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                        .build())
                                .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                        .setPartialTime(String.format("%02d%02d", startDay, endHour))
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                        .build())
                                .build());
                    }
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                            "Must have at least startDay, startHour and endHour of validity " + match.getTACToken()));
                }
            }
        }, () -> result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity")));
        return result;
    }

    private List<ConversionIssue> setBaseForecast(final TAFImpl.Builder builder, final Lexeme baseFctToken, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        TAFBaseForecastImpl.Builder baseFct = new TAFBaseForecastImpl.Builder();

        result.addAll(withForecastSurfaceWind(baseFctToken,
                new Identity[] { Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.MIN_TEMPERATURE,
                        Identity.MAX_TEMPERATURE }, hints, baseFct::setSurfaceWind));
        if (baseFct.getSurfaceWind() == null) {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind is missing from TAF base forecast"));
            return result;
        }
        findNext(Identity.CAVOK, baseFctToken, (match) -> {
            Identity[] before = new Identity[] { Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.MIN_TEMPERATURE,
                    Identity.MAX_TEMPERATURE };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                baseFct.setCeilingAndVisibilityOk(true);
            }
        });

        result.addAll(withVisibility(baseFctToken, new Identity[] { Identity.WEATHER, Identity.CLOUD, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE },
                        hints, (measureAndOperator) -> {
                            baseFct.setPrevailingVisibility(measureAndOperator.getMeasure());
                            if (measureAndOperator.getOperator() != null) {
                                baseFct.setPrevailingVisibilityOperator(measureAndOperator.getOperator());
                            }
                        }));
        if (baseFct.getPrevailingVisibility() == null) {

        }
        /*
        if (fct instanceof TAFBaseForecast) {
            if (!fct.isCeilingAndVisibilityOk()) {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Visibility or CAVOK is missing from TAF base forecast"));
            }
        }
        */
        result.addAll(updateWeather(baseFct, baseFctToken, new Identity[] { Identity.CLOUD, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE }, hints));

        result.addAll(updateClouds(baseFct, baseFctToken, new Identity[] { Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE }, hints));
        result.addAll(updateTemperatures(baseFct, baseFctToken, hints));
        builder.setBaseForecast(baseFct.build());
        return result;
    }

    private List<ConversionIssue> updateTemperatures(final TAFBaseForecastImpl.Builder builder, final Lexeme from, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        List<TAFAirTemperatureForecast> temps = new ArrayList<>();
        TAFAirTemperatureForecastImpl.Builder airTemperatureForecast;

        //Find a pair of max & min temperatures:
        Lexeme maxTempToken = findNext(Identity.MAX_TEMPERATURE, from);
        Lexeme minTempToken;

        while (maxTempToken != null) {
            ConversionIssue issue = checkBeforeAnyOf(maxTempToken, new Identity[] { Identity.MIN_TEMPERATURE });
            if (issue != null) {
                result.add(issue);
            } else {
                minTempToken = findNext(Identity.MIN_TEMPERATURE, maxTempToken);
                if (minTempToken != null) {
                    airTemperatureForecast = new TAFAirTemperatureForecastImpl.Builder();
                    Integer day = minTempToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    Integer hour = minTempToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    Double value = minTempToken.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);

                    if (day != null && hour != null) {
                        airTemperatureForecast.setMinTemperatureTime(new PartialOrCompleteTimeInstant.Builder()
                                .setPartialTime(String.format("%02d%02d", day, hour))
                                .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                .build());
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing day of month and/or hour of day for min forecast temperature: " + minTempToken.getTACToken()));
                    }

                    if (value != null) {
                        airTemperatureForecast.setMinTemperature(NumericMeasureImpl.of(value, "degC"));
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing value for min forecast temperature: " + minTempToken.getTACToken()));
                    }

                    day = maxTempToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    hour = maxTempToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    value = maxTempToken.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);

                    if (day != null && hour != null) {
                        airTemperatureForecast.setMaxTemperatureTime(new PartialOrCompleteTimeInstant.Builder()
                                .setPartialTime(String.format("%02d%02d", day, hour))
                                .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                .build());
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing day of month and/or hour of day for max forecast temperature: " + maxTempToken.getTACToken()));
                    }

                    if (value != null) {
                        airTemperatureForecast.setMaxTemperature(NumericMeasureImpl.of(value, "degC"));
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing value for max forecast temperature: " + maxTempToken.getTACToken()));
                    }
                    temps.add(airTemperatureForecast.build());

                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                            "Missing min temperature pair for max temperature " + maxTempToken.getTACToken()));
                }
            }
            maxTempToken = findNext(Identity.MAX_TEMPERATURE, maxTempToken);
        }
        if (!temps.isEmpty()) {
            builder.setTemperatures(temps);
        }
        return result;
    }

    private List<ConversionIssue> addChangeForecast(final TAFImpl.Builder builder, final Lexeme changeFctToken, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        ConversionIssue issue = checkBeforeAnyOf(changeFctToken, new Identity[] { Identity.REMARKS_START });
        if (issue != null) {
            result.add(issue);
            return result;
        }
        List<TAFChangeForecast> changeForecasts;
        if (builder.getChangeForecasts().isPresent()) {
            changeForecasts = builder.getChangeForecasts().get();
        } else {
            changeForecasts = new ArrayList<>();
        }

        //PROB30 [TEMPO] or PROB40 [TEMPO] or BECMG or TEMPO or FM
        TAFForecastChangeIndicator.ForecastChangeIndicatorType type = changeFctToken.getParsedValue(Lexeme.ParsedValueName.TYPE,
                TAFForecastChangeIndicator.ForecastChangeIndicatorType.class);
        if (changeFctToken.hasNext()) {
            Lexeme next = changeFctToken.getNext();
            if (Identity.REMARKS_START != next.getIdentityIfAcceptable() && Identity.END_TOKEN != next.getIdentityIfAcceptable()) {
                TAFChangeForecastImpl.Builder changeFct = new TAFChangeForecastImpl.Builder();
                switch (type) {
                    case TEMPORARY_FLUCTUATIONS:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.TEMPORARY_FLUCTUATIONS);
                        result.addAll(updateChangeForecastContents(changeFct, type, changeFctToken, hints));
                        break;
                    case BECOMING:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.BECOMING);
                        result.addAll(updateChangeForecastContents(changeFct, type, changeFctToken, hints));
                        break;
                    case FROM:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.FROM);
                        Integer day = changeFctToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                        Integer hour = changeFctToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                        Integer minute = changeFctToken.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                        if (hour != null && minute != null) {
                            if (day != null) {
                                changeFct.setValidityTime(new PartialOrCompleteTimePeriod.Builder()
                                        .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                                .setPartialTime(String.format("%02d%02d%02d", day, hour, minute))
                                                .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_MINUTE_TZ_PATTERN)
                                                .build())
                                        .build());
                            } else {
                                changeFct.setValidityTime(new PartialOrCompleteTimePeriod.Builder()
                                        .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                                .setPartialTime(String.format("%02d%02d", hour, minute))
                                                .setPartialTimePattern(PartialOrCompleteTimeInstant.HOUR_MINUTE_PATTERN)
                                                .build())
                                        .build());
                            }
                        } else {
                            result.add(
                                    new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity start hour or minute in " + next.getTACToken()));
                        }
                        result.addAll(updateChangeForecastContents(changeFct, type, changeFctToken, hints));
                        break;
                    case WITH_40_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_40);
                        result.addAll(updateChangeForecastContents(changeFct, type, changeFctToken, hints));
                        break;
                    case WITH_30_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_30);
                        result.addAll(updateChangeForecastContents(changeFct, type, changeFctToken, hints));
                        break;
                    case TEMPO_WITH_30_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_30_TEMPORARY_FLUCTUATIONS);
                        result.addAll(updateChangeForecastContents(changeFct, type, changeFctToken, hints));
                        break;
                    case TEMPO_WITH_40_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_40_TEMPORARY_FLUCTUATIONS);
                        result.addAll(updateChangeForecastContents(changeFct, type, changeFctToken, hints));
                        break;
                    default:
                        result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Unknown change group " + type));
                        break;
                }
                changeForecasts.add(changeFct.build());
            } else {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing change group content"));
            }
        } else {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing change group content"));
        }
        return result;
    }

    private List<ConversionIssue> updateChangeForecastContents(final TAFChangeForecastImpl.Builder builder, final TAFForecastChangeIndicator.ForecastChangeIndicatorType type,
            final Lexeme from, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();

        //FM case has already been handled in the calling code:
        if (TAFForecastChangeIndicator.ForecastChangeIndicatorType.FROM != type) {
            Lexeme timeGroup = findNext(Identity.TAF_CHANGE_FORECAST_TIME_GROUP, from);
            if (timeGroup != null) {
                Identity[] before = { Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.NO_SIGNIFICANT_WEATHER,
                        Identity.CLOUD };
                ConversionIssue issue = checkBeforeAnyOf(timeGroup, before);
                if (issue != null) {
                    result.add(issue);
                } else {
                    Integer startDay = timeGroup.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    Integer endDay = timeGroup.getParsedValue(Lexeme.ParsedValueName.DAY2, Integer.class);
                    Integer startHour = timeGroup.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    Integer endHour = timeGroup.getParsedValue(Lexeme.ParsedValueName.HOUR2, Integer.class);
                    if (startHour != null && endHour != null) {
                        if (startDay != null && endDay != null) {
                            builder.setValidityTime(new PartialOrCompleteTimePeriod.Builder()
                                    .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d%02d", startDay, startHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                            .build())
                                    .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d%02d", endDay, endHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.DAY_HOUR_PATTERN)
                                            .build())
                                    .build());
                        } else {
                            builder.setValidityTime(new PartialOrCompleteTimePeriod.Builder()
                                    .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d", startHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.HOUR_PATTERN)
                                            .build())
                                    .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d", endHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.HOUR_PATTERN)
                                            .build())
                                    .build());
                        }
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing validity day, hour or minute for change group in " + timeGroup.getTACToken()));
                    }
                }
            } else {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity time for change group after " + from.getTACToken()));
            }
        }

        result.addAll(withForecastSurfaceWind(from, new Identity[] { Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD },
                hints, builder::setSurfaceWind));

        findNext(Identity.CAVOK, from, (match) -> {
            Identity[] before = new Identity[] { Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.NO_SIGNIFICANT_WEATHER, Identity.CLOUD };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                builder.setCeilingAndVisibilityOk(true);
            }
        });
        result.addAll(updateVisibility(fct, from, new Identity[] { Identity.WEATHER, Identity.NO_SIGNIFICANT_WEATHER, Identity.CLOUD }, hints));
        result.addAll(updateWeather(fct, from, new Identity[] { Identity.NO_SIGNIFICANT_WEATHER, Identity.CLOUD }, hints));

        findNext(Identity.NO_SIGNIFICANT_WEATHER, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, new Identity[] { Identity.CLOUD });
            if (issue != null) {
                result.add(issue);
            } else {
                if (fct.getForecastWeather() != null && !fct.getForecastWeather().isEmpty()) {
                    result.add(new ConversionIssue(ConversionIssue.Type.LOGICAL_ERROR, "Cannot have both NSW and weather in the same change forecast"));
                } else {
                    fct.setNoSignificantWeather(true);
                }
            }
        });

        result.addAll(updateClouds(fct, from, new Identity[] {}, hints));

        return result;
    }

    private List<ConversionIssue> withForecastSurfaceWind(final Lexeme from, final Identity[] before, final ConversionHints hints, final Consumer<TAFSurfaceWind> consumer) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.SURFACE_WIND, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                TAFSurfaceWindImpl.Builder wind = new TAFSurfaceWindImpl.Builder();
                Object direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
                Integer meanSpeed = match.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
                Integer gustSpeed = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

                if (direction == SurfaceWind.WindDirection.VARIABLE) {
                    wind.setVariableDirection(true);
                } else if (direction instanceof Integer) {
                    wind.setMeanWindDirection(NumericMeasureImpl.of((Integer) direction, "deg"));
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind direction is missing: " + match.getTACToken()));
                }

                if (meanSpeed != null) {
                    wind.setMeanWindSpeed(NumericMeasureImpl.of(meanSpeed, unit));
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind mean speed is missing: " + match.getTACToken()));
                }

                if (gustSpeed != null) {
                    wind.setWindGust(NumericMeasureImpl.of(gustSpeed, unit));
                }
                consumer.accept(wind.build());
            }
        });
        return result;
    }

    private List<ConversionIssue> withVisibility(final Lexeme from, final Identity[] before, final ConversionHints hints, final Consumer<MeasureWithOperator> consumer) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.HORIZONTAL_VISIBILITY, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                MeasureWithOperator value = new MeasureWithOperator();
                Double distance = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                RecognizingAviMessageTokenLexer.RelationalOperator distanceOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                        RecognizingAviMessageTokenLexer.RelationalOperator.class);
                MetricHorizontalVisibility.DirectionValue direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION,
                        MetricHorizontalVisibility.DirectionValue.class);
                if (direction != null) {
                    result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                            "Directional horizontal visibility not allowed in TAF: " + match.getTACToken()));
                }
                if (distanceOperator != null) {
                    if (RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN == distanceOperator) {
                        value.setOperator(AviationCodeListUser.RelationalOperator.BELOW);
                    } else {
                        value.setOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                    }
                }
                if (distance != null && unit != null) {
                    value.setMeasure(NumericMeasureImpl.of(distance, unit));
                    consumer.accept(value);
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing visibility value or unit: " + match.getTACToken()));
                }
            }
        });
        return result;
    }

    private List<ConversionIssue> updateWeather(final TAFForecast fct, final Lexeme from, final Identity[] before, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.WEATHER, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                List<fi.fmi.avi.model.Weather> weather = new ArrayList<>();
                result.addAll(appendWeatherCodes(match, weather, before, hints));
                fct.setForecastWeather(weather);
            }
        });
        return result;
    }

    private List<ConversionIssue> updateClouds(final TAFForecast fct, final Lexeme from, final Identity[] before, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.CLOUD, from, (match) -> {
            ConversionIssue issue;
            CloudForecast cloud = new CloudForecastImpl();
            List<fi.fmi.avi.model.CloudLayer> layers = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.add(issue);
                } else {
                    CloudLayer.CloudCover cover = match.getParsedValue(Lexeme.ParsedValueName.COVER, CloudLayer.CloudCover.class);
                    Object value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Object.class);
                    String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                    if (CloudLayer.CloudCover.SKY_OBSCURED == cover) {
                        Integer height;
                        if (value instanceof Integer) {
                            height = (Integer) value;
                            if ("hft".equals(unit)) {
                                height = height * 100;
                                unit = "[ft_i]";
                            }
                            cloud.setVerticalVisibility(new NumericMeasureImpl(height, unit));
                        } else {
                            result.add(
                                    new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Cloud layer height is not an integer in " + match.getTACToken()));
                        }

                    } else if (CloudCover.NO_SIG_CLOUDS == cover) {
                        cloud.setNoSignificantCloud(true);
                    } else {
                        fi.fmi.avi.model.CloudLayer layer = getCloudLayer(match);
                        if (layer != null) {
                            layers.add(layer);
                        } else {
                            result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                                    "Could not parse token " + match.getTACToken() + " as cloud " + "layer"));
                        }
                    }
                }
                match = findNext(Identity.CLOUD, match);
            }
            if (!layers.isEmpty()) {
                cloud.setLayers(layers);
            }
            fct.setCloud(cloud);
        }, () -> {
            if (fct instanceof TAFBaseForecast) {
                if (!fct.isCeilingAndVisibilityOk()) {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Cloud or CAVOK is missing from TAF base forecast"));
                }
            }
        });
        return result;
    }

    private class MeasureWithOperator {
        private NumericMeasure measure;
        private AviationCodeListUser.RelationalOperator operator;

        public NumericMeasure getMeasure() {
            return measure;
        }

        public AviationCodeListUser.RelationalOperator getOperator() {
            return operator;
        }

        public void setMeasure(NumericMeasure measure) {
            this.measure = measure;
        }

        public void setOperator(AviationCodeListUser.RelationalOperator operator) {
            this.operator = operator;
        }
    }

}
