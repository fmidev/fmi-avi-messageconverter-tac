package fi.fmi.avi.converter.tac;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetricHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.SurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFForecastChangeIndicator;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFSurfaceWind;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import fi.fmi.avi.model.taf.immutable.TAFSurfaceWindImpl;

public abstract class TAFTACParserBase<T extends TAF> extends AbstractTACParser<T> {

    protected static Lexeme.Identity[] zeroOrOneAllowed = { Lexeme.Identity.AERODROME_DESIGNATOR, Lexeme.Identity.ISSUE_TIME, Lexeme.Identity.VALID_TIME, Lexeme.Identity.CORRECTION, Lexeme.Identity.AMENDMENT, Lexeme.Identity.CANCELLATION, Lexeme.Identity.NIL, Lexeme.Identity.MIN_TEMPERATURE,
            Lexeme.Identity.MAX_TEMPERATURE, Lexeme.Identity.REMARKS_START };
    protected AviMessageLexer lexer;

    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    protected ConversionResult<TAFImpl> convertMessageInternal(final String input, final ConversionHints hints) {
        ConversionResult<TAFImpl> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (!checkAndReportLexingResult(lexed, hints, result)) {
            return result;
        }

        if (Lexeme.Identity.TAF_START != lexed.getFirstLexeme().getIdentityIfAcceptable()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as TAF"));
            return result;
        }

        if (!endsInEndToken(lexed, hints)) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Message does not end in end token"));
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
        List<LexemeSequence> subSequences = lexed.splitBy(Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Identity.REMARKS_START);

        findNext(Lexeme.Identity.CORRECTION, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Lexeme.Identity.AERODROME_DESIGNATOR, Lexeme.Identity.ISSUE_TIME, Lexeme.Identity.NIL, Lexeme.Identity.VALID_TIME, Lexeme.Identity.CANCELLATION,
                    Lexeme.Identity.SURFACE_WIND, Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.CAVOK, Lexeme.Identity.MIN_TEMPERATURE,
                    Lexeme.Identity.MAX_TEMPERATURE, Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setStatus(AviationCodeListUser.TAFStatus.CORRECTION);
            }
        });

        findNext(Lexeme.Identity.AMENDMENT, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = { Lexeme.Identity.AERODROME_DESIGNATOR, Lexeme.Identity.ISSUE_TIME, Lexeme.Identity.NIL, Lexeme.Identity.VALID_TIME, Lexeme.Identity.CANCELLATION,
                    Lexeme.Identity.SURFACE_WIND, Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.CAVOK, Lexeme.Identity.MIN_TEMPERATURE,
                    Lexeme.Identity.MAX_TEMPERATURE, Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setStatus(AviationCodeListUser.TAFStatus.AMENDMENT);
            }
        });

        findNext(Lexeme.Identity.AERODROME_DESIGNATOR, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = new Lexeme.Identity[] { Lexeme.Identity.ISSUE_TIME, Lexeme.Identity.NIL, Lexeme.Identity.VALID_TIME, Lexeme.Identity.CANCELLATION, Lexeme.Identity.SURFACE_WIND,
                    Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.CAVOK, Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE,
                    Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setAerodrome(new AerodromeImpl.Builder()
                        .setDesignator(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class))
                        .build());
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Aerodrome designator not given in " + input)));

        result.addIssue(setTAFIssueTime(builder, lexed, hints));

        findNext(Lexeme.Identity.NIL, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = new Lexeme.Identity[] { Lexeme.Identity.VALID_TIME, Lexeme.Identity.CANCELLATION, Lexeme.Identity.SURFACE_WIND, Lexeme.Identity.HORIZONTAL_VISIBILITY,
                    Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.CAVOK, Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE,
                    Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setStatus(AviationCodeListUser.TAFStatus.MISSING);
                if (match.getNext() != null) {
                    Lexeme.Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                    if (Lexeme.Identity.END_TOKEN != nextTokenId && Lexeme.Identity.REMARKS_START != nextTokenId) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Missing TAF message contains extra tokens after NIL: " + input));
                    }
                }
            }
        });

        for (int i = 1; i < subSequences.size(); i++) {
            LexemeSequence seq = subSequences.get(i);
            if (Lexeme.Identity.REMARKS_START == seq.getFirstLexeme().getIdentity()) {
                List<String> remarks = getRemarks(seq.getFirstLexeme(), hints);
                if (!remarks.isEmpty()) {
                    builder.setRemarks(remarks);
                }
            }
        }

        //End processing here if NIL:
        if (AviationCodeListUser.TAFStatus.MISSING == builder.getStatus()) {
            result.setConvertedMessage(builder.build());
            return result;
        }

        result.addIssue(setTAFValidTime(builder, lexed, hints));

        findNext(Lexeme.Identity.CANCELLATION, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = new Lexeme.Identity[] { Lexeme.Identity.SURFACE_WIND, Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.CAVOK,
                    Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE, Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Identity.REMARKS_START };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setStatus(AviationCodeListUser.TAFStatus.CANCELLATION);
                if (match.getNext() != null) {
                    Lexeme.Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                    if (Lexeme.Identity.END_TOKEN != nextTokenId && Lexeme.Identity.REMARKS_START != nextTokenId) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Cancelled TAF message contains extra tokens after CNL: " + input));
                    }
                }
            }
        });

        //End processing here if CNL:
        if (AviationCodeListUser.TAFStatus.CANCELLATION == builder.getStatus()) {
            result.setConvertedMessage(builder.build());
            return result;
        }

        //Should always return at least one as long as lexed is not empty, the first one is the base forecast:
        result.addIssue(setBaseForecast(builder, subSequences.get(0).getFirstLexeme(), hints));
        for (int i = 1; i < subSequences.size(); i++) {
            LexemeSequence seq = subSequences.get(i);
            if (Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR == seq.getFirstLexeme().getIdentity()) {
                result.addIssue(addChangeForecast(builder, subSequences.get(i).getFirstLexeme(), hints));
            }
        }
        if (builder.getValidityTime().isPresent()) {
            setFromChangeForecastEndTimes(builder);
        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time period must be set before amending FM end times"));
        }

        result.setConvertedMessage(builder.build());
        return result;
    }

    protected List<ConversionIssue> setTAFIssueTime(final TAFImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        Lexeme.Identity[] before = new Lexeme.Identity[] { Lexeme.Identity.NIL, Lexeme.Identity.VALID_TIME, Lexeme.Identity.CANCELLATION, Lexeme.Identity.SURFACE_WIND, Lexeme.Identity.HORIZONTAL_VISIBILITY,
                Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.CAVOK, Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE, Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR,
                Lexeme.Identity.REMARKS_START };
        retval.addAll(withFoundIssueTime(lexed, before, hints, builder::setIssueTime));
        return retval;
    }

    protected List<ConversionIssue> setTAFValidTime(final TAFImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Lexeme.Identity.VALID_TIME, lexed.getFirstLexeme(), (match) -> {
            Lexeme.Identity[] before = new Lexeme.Identity[] { Lexeme.Identity.CANCELLATION, Lexeme.Identity.SURFACE_WIND, Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD,
                    Lexeme.Identity.CAVOK, Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE, Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, Lexeme.Identity.REMARKS_START };
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
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
                                        .build())
                                .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                        .setPartialTime(String.format("%02d%02d", endDay, endHour))
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
                                        .build())
                                .build());
                    } else {
                        builder.setValidityTime(new PartialOrCompleteTimePeriod.Builder()
                                .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                        .setPartialTime(String.format("%02d%02d", startDay, startHour))
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
                                        .build())
                                .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                        .setPartialTime(String.format("%02d", endHour))
                                        .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.Hour)
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

    protected List<ConversionIssue> setBaseForecast(final TAFImpl.Builder builder, final Lexeme baseFctToken, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        TAFBaseForecastImpl.Builder baseFct = new TAFBaseForecastImpl.Builder();

        result.addAll(withForecastSurfaceWind(baseFctToken,
                new Lexeme.Identity[] { Lexeme.Identity.CAVOK, Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.MIN_TEMPERATURE,
                        Lexeme.Identity.MAX_TEMPERATURE }, hints, baseFct::setSurfaceWind));
        if (!baseFct.getSurfaceWind().isPresent()) {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind is missing from TAF base forecast"));
        }
        findNext(Lexeme.Identity.CAVOK, baseFctToken, (match) -> {
            Lexeme.Identity[] before = new Lexeme.Identity[] { Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.MIN_TEMPERATURE,
                    Lexeme.Identity.MAX_TEMPERATURE };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                baseFct.setCeilingAndVisibilityOk(true);
            }
        });

        result.addAll(withVisibility(baseFctToken, new Lexeme.Identity[] { Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD, Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE },
                        hints, (measureAndOperator) -> {
                            baseFct.setPrevailingVisibility(measureAndOperator.getMeasure());
                    baseFct.setPrevailingVisibilityOperator(measureAndOperator.getOperator());
                        }));
        if (baseFct.getPrevailingVisibility() == null) {
            if (!baseFct.isCeilingAndVisibilityOk()) {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Visibility or CAVOK is missing from TAF base forecast"));
                return result;
            }
        }

        result.addAll(withWeather(baseFctToken, new Lexeme.Identity[] { Lexeme.Identity.CLOUD, Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE }, hints,
                baseFct::setForecastWeather));
        result.addAll(withClouds(baseFctToken, new Lexeme.Identity[] { Lexeme.Identity.MIN_TEMPERATURE, Lexeme.Identity.MAX_TEMPERATURE }, hints, baseFct::setCloud));

        if (!baseFct.getCloud().isPresent() && !baseFct.isCeilingAndVisibilityOk()) {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Cloud or CAVOK is missing from TAF base forecast"));
        }

        result.addAll(updateTemperatures(baseFct, baseFctToken, hints));

        builder.setBaseForecast(baseFct.build());
        return result;
    }

    private List<ConversionIssue> updateTemperatures(final TAFBaseForecastImpl.Builder builder, final Lexeme from, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        List<TAFAirTemperatureForecast> temps = new ArrayList<>();
        TAFAirTemperatureForecastImpl.Builder airTemperatureForecast;

        //Find a pair of max & min temperatures:
        Lexeme maxTempToken = findNext(Lexeme.Identity.MAX_TEMPERATURE, from);
        Lexeme minTempToken;

        while (maxTempToken != null) {
            ConversionIssue issue = checkBeforeAnyOf(maxTempToken, new Lexeme.Identity[] { Lexeme.Identity.MIN_TEMPERATURE });
            if (issue != null) {
                result.add(issue);
            } else {
                minTempToken = findNext(Lexeme.Identity.MIN_TEMPERATURE, maxTempToken);
                if (minTempToken != null) {
                    airTemperatureForecast = new TAFAirTemperatureForecastImpl.Builder();
                    Integer day = minTempToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    Integer hour = minTempToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    Double value = minTempToken.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);

                    if (day != null && hour != null) {
                        airTemperatureForecast.setMinTemperatureTime(new PartialOrCompleteTimeInstant.Builder()
                                .setPartialTime(String.format("%02d%02d", day, hour))
                                .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
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
                                .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
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
                    result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                            "Missing min temperature pair for max temperature " + maxTempToken.getTACToken()));
                }
            }
            maxTempToken = findNext(Lexeme.Identity.MAX_TEMPERATURE, maxTempToken);
        }
        if (!temps.isEmpty()) {
            builder.setTemperatures(temps);
        }
        return result;
    }

    protected List<ConversionIssue> addChangeForecast(final TAFImpl.Builder builder, final Lexeme changeFctToken, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        ConversionIssue issue = checkBeforeAnyOf(changeFctToken, new Lexeme.Identity[] { Lexeme.Identity.REMARKS_START });
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
            if (Lexeme.Identity.REMARKS_START != next.getIdentityIfAcceptable() && Lexeme.Identity.END_TOKEN != next.getIdentityIfAcceptable()) {
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
                                changeFct.setPeriodOfChange(new PartialOrCompleteTimePeriod.Builder()
                                        .setStartTime(new PartialOrCompleteTimeInstant.Builder().setPartialTime(
                                                String.format("%02d%02d%02d", day, hour, minute))
                                                .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHourMinute)
                                                .build())
                                        .build());
                            } else {
                                changeFct.setPeriodOfChange(new PartialOrCompleteTimePeriod.Builder()
                                        .setStartTime(new PartialOrCompleteTimeInstant.Builder().setPartialTime(String.format("%02d%02d", hour, minute))
                                                .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.HourMinute)
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
                        result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Unknown change group " + type));
                        break;
                }

                changeForecasts.add(changeFct.build());
            } else {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing change group content"));
            }
        } else {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing change group content"));
        }
        builder.setChangeForecasts(changeForecasts);
        return result;
    }

    private List<ConversionIssue> updateChangeForecastContents(final TAFChangeForecastImpl.Builder builder, final TAFForecastChangeIndicator.ForecastChangeIndicatorType type,
                                                               final Lexeme from, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();

        //FM case has already been handled in the calling code:
        if (TAFForecastChangeIndicator.ForecastChangeIndicatorType.FROM != type) {
            Lexeme timeGroup = findNext(Lexeme.Identity.TAF_CHANGE_FORECAST_TIME_GROUP, from);
            if (timeGroup != null) {
                Lexeme.Identity[] before = { Lexeme.Identity.SURFACE_WIND, Lexeme.Identity.CAVOK, Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.NO_SIGNIFICANT_WEATHER,
                        Lexeme.Identity.CLOUD };
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

                            builder.setPeriodOfChange(new PartialOrCompleteTimePeriod.Builder()
                                    .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d%02d", startDay, startHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
                                            .build())
                                    .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d%02d", endDay, endHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
                                            .build())
                                    .build());
                        } else {
                            builder.setPeriodOfChange(new PartialOrCompleteTimePeriod.Builder()
                                    .setStartTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d", startHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.Hour)
                                            .build())
                                    .setEndTime(new PartialOrCompleteTimeInstant.Builder()
                                            .setPartialTime(String.format("%02d", endHour))
                                            .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.Hour)
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

        result.addAll(withForecastSurfaceWind(from, new Lexeme.Identity[] { Lexeme.Identity.CAVOK, Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.CLOUD },
                hints, builder::setSurfaceWind));

        findNext(Lexeme.Identity.CAVOK, from, (match) -> {
            Lexeme.Identity[] before = new Lexeme.Identity[] { Lexeme.Identity.HORIZONTAL_VISIBILITY, Lexeme.Identity.WEATHER, Lexeme.Identity.NO_SIGNIFICANT_WEATHER, Lexeme.Identity.CLOUD };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                builder.setCeilingAndVisibilityOk(true);
            }
        });
        result.addAll(
                withVisibility(from, new Lexeme.Identity[] { Lexeme.Identity.WEATHER, Lexeme.Identity.NO_SIGNIFICANT_WEATHER, Lexeme.Identity.CLOUD }, hints, (measureWithOperator -> {
                    builder.setPrevailingVisibility(measureWithOperator.getMeasure());
                    builder.setPrevailingVisibilityOperator(measureWithOperator.getOperator());

                })));
        result.addAll(withWeather(from, new Lexeme.Identity[] { Lexeme.Identity.NO_SIGNIFICANT_WEATHER, Lexeme.Identity.CLOUD }, hints, builder::setForecastWeather));

        findNext(Lexeme.Identity.NO_SIGNIFICANT_WEATHER, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, new Lexeme.Identity[] { Lexeme.Identity.CLOUD });
            if (issue != null) {
                result.add(issue);
            } else {
                if (builder.getForecastWeather().isPresent()) {
                    result.add(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Cannot have both NSW and weather in the same change forecast"));
                } else {
                    builder.setNoSignificantWeather(true);
                }
            }
        });

        result.addAll(withClouds(from, new Lexeme.Identity[] {}, hints, builder::setCloud));

        return result;
    }

    protected List<ConversionIssue> setFromChangeForecastEndTimes(final TAFImpl.Builder builder) {
        List<ConversionIssue> result = new ArrayList<>();
        Optional<List<TAFChangeForecast>> changeForecasts = builder.getChangeForecasts();
        if (changeForecasts.isPresent()) {
            TAFChangeForecastImpl.Builder toChange = null;
            int indexOfChange = -1;
            for (int i = 0; i < changeForecasts.get().size(); i++) {
                TAFChangeForecast fct = changeForecasts.get().get(i);
                if (AviationCodeListUser.TAFChangeIndicator.FROM == fct.getChangeIndicator() && fct.getPeriodOfChange().getStartTime().isPresent()) {
                    if (toChange != null) {
                        toChange.mutatePeriodOfChange(b -> b.setEndTime(fct.getPeriodOfChange().getStartTime().get()));
                        changeForecasts.get().set(indexOfChange, toChange.build());
                    }
                    toChange = TAFChangeForecastImpl.immutableCopyOf(fct).toBuilder();
                    indexOfChange = i;
                }
            }
            if (toChange != null) {
                toChange.mutatePeriodOfChange(b -> b.setEndTime(builder.getValidityTime().get().getEndTime()));
                changeForecasts.get().set(indexOfChange, toChange.build());
            }
        }
        return result;
    }

    private List<ConversionIssue> withForecastSurfaceWind(final Lexeme from, final Lexeme.Identity[] before, final ConversionHints hints, final Consumer<TAFSurfaceWind> consumer) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Lexeme.Identity.SURFACE_WIND, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                TAFSurfaceWindImpl.Builder wind = new TAFSurfaceWindImpl.Builder();
                Object direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
                Integer meanSpeed = match.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
                Integer gustSpeed = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                AviationCodeListUser.RelationalOperator meanSpeedOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                        AviationCodeListUser.RelationalOperator.class);
                AviationCodeListUser.RelationalOperator gustOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR2,
                        AviationCodeListUser.RelationalOperator.class);

                if (direction == SurfaceWind.WindDirection.VARIABLE) {
                    wind.setVariableDirection(true);
                } else if (direction instanceof Integer) {
                    wind.setMeanWindDirection(NumericMeasureImpl.of((Integer) direction, "deg"));
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind direction is missing: " + match.getTACToken()));
                }

                if (meanSpeed != null) {
                    wind.setMeanWindSpeed(NumericMeasureImpl.of(meanSpeed, unit));
                    if (meanSpeedOperator != null) {
                        wind.setMeanWindSpeedOperator(meanSpeedOperator);
                    }
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind mean speed is missing: " + match.getTACToken()));
                }

                if (gustSpeed != null) {
                    wind.setWindGust(NumericMeasureImpl.of(gustSpeed, unit));
                    if (gustOperator != null) {
                        wind.setWindGustOperator(gustOperator);
                    }
                }
                consumer.accept(wind.build());
            }
        });
        return result;
    }

    private List<ConversionIssue> withVisibility(final Lexeme from, final Lexeme.Identity[] before, final ConversionHints hints, final Consumer<MeasureWithOperator> consumer) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Lexeme.Identity.HORIZONTAL_VISIBILITY, from, (match) -> {
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
                    result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
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

    private List<ConversionIssue> withWeather(final Lexeme from, final Lexeme.Identity[] before, final ConversionHints hints,
                                              final Consumer<List<fi.fmi.avi.model.Weather>> consumer) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Lexeme.Identity.WEATHER, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                List<fi.fmi.avi.model.Weather> weather = new ArrayList<>();
                result.addAll(appendWeatherCodes(match, weather, before, hints));
                consumer.accept(weather);
            }
        });
        return result;
    }

    private List<ConversionIssue> withClouds(final Lexeme from, final Lexeme.Identity[] before, final ConversionHints hints, Consumer<CloudForecast> consumer) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Lexeme.Identity.CLOUD, from, (match) -> {
            ConversionIssue issue;
            CloudForecastImpl.Builder cloud = new CloudForecastImpl.Builder();
            List<CloudLayer> layers = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.add(issue);
                } else {
                    fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover cover = match.getParsedValue(Lexeme.ParsedValueName.COVER, fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover.class);
                    Object value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Object.class);
                    String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                    if (fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover.SKY_OBSCURED == cover) {
                        Integer height;
                        if (value instanceof Integer) {
                            height = (Integer) value;
                            if ("hft".equals(unit)) {
                                height = height * 100;
                                unit = "[ft_i]";
                            }
                            cloud.setVerticalVisibility(NumericMeasureImpl.of(height, unit));
                        } else {
                            result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Cloud layer height is not an integer in " + match.getTACToken()));
                        }

                    } else if (fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover.NO_SIG_CLOUDS == cover) {
                        cloud.setNoSignificantCloud(true);
                    } else {
                        CloudLayer layer = getCloudLayer(match);
                        if (layer != null) {
                            layers.add(layer);
                        } else {
                            result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX,
                                    "Could not parse token " + match.getTACToken() + " as cloud " + "layer"));
                        }
                    }
                }
                match = findNext(Lexeme.Identity.CLOUD, match);
            }
            if (!layers.isEmpty()) {
                cloud.setLayers(layers);
            }
            consumer.accept(cloud.build());
        });
        return result;
    }

    private class MeasureWithOperator {
        private NumericMeasure measure;
        private AviationCodeListUser.RelationalOperator operator;

        public NumericMeasure getMeasure() {
            return measure;
        }

        public Optional<AviationCodeListUser.RelationalOperator> getOperator() {
            if (operator == null) {
                return Optional.empty();
            } else {
                return Optional.of(operator);
            }
        }

        public void setMeasure(NumericMeasure measure) {
            this.measure = measure;
        }

        public void setOperator(AviationCodeListUser.RelationalOperator operator) {
            this.operator = operator;
        }
    }
}
