package fi.fmi.avi.converter.tac.taf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.IssueList;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetricHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFForecastChangeIndicator;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.CloudLayer;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.SurfaceWind;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.CloudForecastImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.SurfaceWindImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.immutable.TAFAirTemperatureForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import fi.fmi.avi.model.taf.immutable.TAFReferenceImpl;

public abstract class TAFTACParserBase<T extends TAF> extends AbstractTACParser<T> {

    protected static final LexemeIdentity[] zeroOrOneAllowed = {LexemeIdentity.AERODROME_DESIGNATOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.VALID_TIME,
            LexemeIdentity.CORRECTION, LexemeIdentity.AMENDMENT, LexemeIdentity.CANCELLATION, LexemeIdentity.NIL, LexemeIdentity.MIN_TEMPERATURE,
            LexemeIdentity.MAX_TEMPERATURE, LexemeIdentity.REMARKS_START };
    protected AviMessageLexer lexer;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    protected ConversionResult<TAFImpl> convertMessageInternal(final String input, final ConversionHints hints) {
        final ConversionResult<TAFImpl> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        final LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (!checkAndReportLexingResult(lexed, hints, result)) {
            return result;
        }

        final Lexeme firstLexeme = lexed.getFirstLexeme();
        if (LexemeIdentity.TAF_START != firstLexeme.getIdentity()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as TAF"));
            return result;
        } else if (firstLexeme.isSynthetic()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                    "Message does not start with a start token: " + firstLexeme.getTACToken()));
        }

        if (!endsInEndToken(lexed, hints)) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Message does not end in end token"));
            return result;
        }
        final List<ConversionIssue> issues = checkZeroOrOne(lexed, zeroOrOneAllowed);
        if (!issues.isEmpty()) {
            result.addIssue(issues);
            return result;
        }
        final TAFImpl.Builder builder = TAFImpl.builder();
        final Boolean[] missingMessage = { false };

        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }

        withTimeForTranslation(hints, builder::setTranslationTime);

        //Split & filter in the sequences starting with FORECAST_CHANGE_INDICATOR:
        final List<LexemeSequence> subSequences = lexed.splitBy(LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR, LexemeIdentity.REMARKS_START);

        lexed.getFirstLexeme().findNext(LexemeIdentity.CORRECTION, (match) -> {
            final LexemeIdentity[] before = {LexemeIdentity.AERODROME_DESIGNATOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.NIL,
                    LexemeIdentity.VALID_TIME, LexemeIdentity.CANCELLATION, LexemeIdentity.SURFACE_WIND, LexemeIdentity.HORIZONTAL_VISIBILITY,
                    LexemeIdentity.WEATHER, LexemeIdentity.CLOUD, LexemeIdentity.CAVOK, LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE,
                    LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR, LexemeIdentity.REMARKS_START};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setReportStatus(AviationWeatherMessage.ReportStatus.CORRECTION);
            }
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.AMENDMENT, (match) -> {
            final LexemeIdentity[] before = {LexemeIdentity.AERODROME_DESIGNATOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.NIL,
                    LexemeIdentity.VALID_TIME, LexemeIdentity.CANCELLATION, LexemeIdentity.SURFACE_WIND, LexemeIdentity.HORIZONTAL_VISIBILITY,
                    LexemeIdentity.WEATHER, LexemeIdentity.CLOUD, LexemeIdentity.CAVOK, LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE,
                    LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR, LexemeIdentity.REMARKS_START };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setReportStatus(AviationWeatherMessage.ReportStatus.AMENDMENT);
            }
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.AERODROME_DESIGNATOR, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.ISSUE_TIME, LexemeIdentity.NIL, LexemeIdentity.VALID_TIME,
                    LexemeIdentity.CANCELLATION, LexemeIdentity.SURFACE_WIND, LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER,
                    LexemeIdentity.CLOUD, LexemeIdentity.CAVOK, LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE,
                    LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR, LexemeIdentity.REMARKS_START };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setAerodrome(AerodromeImpl.builder().setDesignator(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class)).build());
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Aerodrome designator not given in " + input)));

        result.addIssue(setTAFIssueTime(builder, lexed, hints));

        lexed.getFirstLexeme().findNext(LexemeIdentity.NIL,  (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] {LexemeIdentity.VALID_TIME, LexemeIdentity.CANCELLATION, LexemeIdentity.SURFACE_WIND,
                    LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER, LexemeIdentity.CLOUD, LexemeIdentity.CAVOK,
                    LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE, LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR,
                    LexemeIdentity.REMARKS_START };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                missingMessage[0] = true;
                if (match.getNext() != null) {
                    final LexemeIdentity nextTokenId = match.getNext().getIdentityIfAcceptable();
                    if (LexemeIdentity.END_TOKEN != nextTokenId && LexemeIdentity.REMARKS_START != nextTokenId) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Missing TAF message contains extra tokens after NIL: " + input));
                    }
                }
            }
        });

        for (int i = 1; i < subSequences.size(); i++) {
            final LexemeSequence seq = subSequences.get(i);
            if (LexemeIdentity.REMARKS_START.equals(seq.getFirstLexeme().getIdentity())) {
                final List<String> remarks = getRemarks(seq.getFirstLexeme(), hints);
                if (!remarks.isEmpty()) {
                    builder.setRemarks(remarks);
                }
            }
        }

        if (missingMessage[0]) {
            result.setConvertedMessage(builder.build());
            return result;
        }

        lexed.getFirstLexeme().findNext(LexemeIdentity.CANCELLATION, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.SURFACE_WIND, LexemeIdentity.HORIZONTAL_VISIBILITY,
                    LexemeIdentity.WEATHER, LexemeIdentity.CLOUD, LexemeIdentity.CAVOK, LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE,
                    LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR, LexemeIdentity.REMARKS_START };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setReportStatus(AviationWeatherMessage.ReportStatus.NORMAL);
                builder.setCancelMessage(true);
                if (match.getNext() != null) {
                    final LexemeIdentity nextTokenId = match.getNext().getIdentityIfAcceptable();
                    if (LexemeIdentity.END_TOKEN != nextTokenId && LexemeIdentity.REMARKS_START != nextTokenId) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL, "Cancelled TAF message contains extra tokens after CNL: " + input));
                    }
                }
            }
        });
        Object referencePolicy = ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD;
        if (hints != null) {
            referencePolicy = hints.getOrDefault(ConversionHints.KEY_TAF_REFERENCE_POLICY, ConversionHints
                    .VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD);
        }

        if(!builder.getReportStatus().isPresent()) {
            result.addIssue(setTAFValidTime(builder, lexed, hints));
        } else {
            switch (builder.getReportStatus().get()) {
                case AMENDMENT:
                    if (referencePolicy.equals(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD)) {
                        result.addIssue(setReferredReportValidPeriod(builder, lexed, hints));
                    } else {
                        result.addIssue(setTAFValidTime(builder, lexed, hints));
                    }
                    break;
                case CORRECTION:
                    if (referencePolicy.equals(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD) || referencePolicy.equals(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL)) {
                        result.addIssue(setReferredReportValidPeriod(builder, lexed, hints));
                    } else {
                        result.addIssue(setTAFValidTime(builder, lexed, hints));
                    }
                    break;
                case NORMAL:
                    if(builder.isCancelMessage()) {
                        if (referencePolicy.equals(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD) || referencePolicy
                                .equals(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL) || referencePolicy.equals(
                                ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_CNL)) {
                            result.addIssue(setReferredReportValidPeriod(builder, lexed, hints));
                        } else {
                            result.addIssue(setTAFValidTime(builder, lexed, hints));
                        }
                        break;
                    }
                default:
                    result.addIssue(setTAFValidTime(builder, lexed, hints));

            }
        }

        //End processing here if CNL:
        if (builder.isCancelMessage()) {
            result.setConvertedMessage(builder.build());
            return result;
        }

        //Should always return at least one as long as lexed is not empty, the first one is the base forecast:
        result.addIssue(setBaseForecast(builder, subSequences.get(0).getFirstLexeme(), hints));
        for (int i = 1; i < subSequences.size(); i++) {
            final LexemeSequence seq = subSequences.get(i);
            if (LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR.equals(seq.getFirstLexeme().getIdentity())) {
                result.addIssue(addChangeForecast(builder, subSequences.get(i).getFirstLexeme(), hints));
            }
        }

        result.addIssue(setFromChangeForecastEndTimes(builder));

        try {
            result.setConvertedMessage(builder.build());
        } catch (final IllegalStateException ignored) {
            // The message has an unset mandatory property and cannot be built, omit it from result
        }
        return result;
    }

    protected List<ConversionIssue> setTAFIssueTime(final TAFImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        final LexemeIdentity[] before = new LexemeIdentity[] {LexemeIdentity.NIL, LexemeIdentity.VALID_TIME, LexemeIdentity.CANCELLATION,
                LexemeIdentity.SURFACE_WIND, LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER, LexemeIdentity.CLOUD, LexemeIdentity.CAVOK,
                LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE, LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR,
                LexemeIdentity.REMARKS_START };
        return new ArrayList<>(withFoundIssueTime(lexed, before, hints, builder::setIssueTime));
    }

    protected List<ConversionIssue> setTAFValidTime(final TAFImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        final IssueList result = new IssueList();
        Optional<PartialOrCompleteTimePeriod> validityTime = parseValidityTime(lexed,result);
        if (validityTime.isPresent()) {
            builder.setValidityTime(validityTime);
        } else {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity"));
        }
        return result;
    }

    protected List<ConversionIssue> setReferredReportValidPeriod(final TAFImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        IssueList result = new IssueList();

        final PartialOrCompleteTimePeriod.Builder timePeriodBuilder = PartialOrCompleteTimePeriod.builder();
        Optional<PartialOrCompleteTimePeriod> validityTime = parseValidityTime(lexed,result);

        if (validityTime.isPresent()) {
            builder.setReferredReportValidPeriod(validityTime);
        } else {
            result.add(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Valid time not available in for TAF, unable to construct "
                    + "TAFReference");
        }

        return result;
    }

    private Optional<PartialOrCompleteTimePeriod> parseValidityTime(final LexemeSequence lexed, final IssueList issues) {
        Optional<PartialOrCompleteTimePeriod> retval = Optional.empty();
        Lexeme match = lexed.getFirstLexeme().findNext(LexemeIdentity.VALID_TIME);
        if (match != null) {
            final LexemeIdentity[] before = new LexemeIdentity[] {LexemeIdentity.CANCELLATION, LexemeIdentity.SURFACE_WIND,
                    LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER, LexemeIdentity.CLOUD, LexemeIdentity.CAVOK,
                    LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE, LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR,
                    LexemeIdentity.REMARKS_START };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                issues.add(issue);
            } else {
                final Integer startDay = match.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                final Integer endDay = match.getParsedValue(Lexeme.ParsedValueName.DAY2, Integer.class);
                final Integer startHour = match.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                final Integer endHour = match.getParsedValue(Lexeme.ParsedValueName.HOUR2, Integer.class);
                if (startDay != null && startHour != null && endHour != null) {
                    final PartialDateTime startTime = PartialDateTime.ofDayHour(startDay, startHour);
                    final PartialDateTime endTime = endDay == null ? PartialDateTime.ofHour(endHour) : PartialDateTime.ofDayHour(endDay, endHour);
                    retval = Optional.of(PartialOrCompleteTimePeriod.builder()//
                            .setStartTime(PartialOrCompleteTimeInstant.of(startTime))//
                            .setEndTime(PartialOrCompleteTimeInstant.of(endTime))//
                            .build());
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                            "Must have at least startDay, startHour and endHour of validity " + match.getTACToken()));
                }
            }
        }
        return retval;
    }

    protected List<ConversionIssue> setBaseForecast(final TAFImpl.Builder builder, final Lexeme baseFctToken, final ConversionHints hints) {
        final TAFBaseForecastImpl.Builder baseFct = TAFBaseForecastImpl.builder();

        //noinspection CollectionAddAllCanBeReplacedWithConstructor
        final List<ConversionIssue> result = new ArrayList<>(withForecastSurfaceWind(baseFctToken,
                new LexemeIdentity[] {LexemeIdentity.CAVOK, LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER, LexemeIdentity.CLOUD,
                        LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE }, hints, baseFct::setSurfaceWind));
        if (!baseFct.getSurfaceWind().isPresent()) {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind is missing from TAF base forecast"));
        }
        baseFctToken.findNext(LexemeIdentity.CAVOK, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] {LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER, LexemeIdentity.CLOUD,
                    LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                baseFct.setCeilingAndVisibilityOk(true);
            }
        });

        result.addAll(withVisibility(baseFctToken,
                new LexemeIdentity[] {LexemeIdentity.WEATHER, LexemeIdentity.CLOUD, LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE },
                hints, (measureAndOperator) -> {
                    baseFct.setPrevailingVisibility(measureAndOperator.getMeasure());
                    baseFct.setPrevailingVisibilityOperator(measureAndOperator.getOperator());
                }));
        if (!baseFct.getPrevailingVisibility().isPresent()) {
            if (!baseFct.isCeilingAndVisibilityOk()) {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Visibility or CAVOK is missing from TAF base forecast"));
                return result;
            }
        }

        result.addAll(withWeather(baseFctToken, new LexemeIdentity[] { LexemeIdentity.CLOUD, LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE }, hints,
                baseFct::setForecastWeather));
        //Ensure that forecastWeather is always non-empty for base forecast unless CAVOK:
        if (!baseFct.getForecastWeather().isPresent() && !baseFct.isCeilingAndVisibilityOk()) {
            baseFct.setForecastWeather(new ArrayList<>());
        }

        //NSW is not allowed in base weather
        if (baseFctToken.findNext(LexemeIdentity.NO_SIGNIFICANT_WEATHER) != null){
            result.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "NSW not allowed in TAF base weather"));
        }

        result.addAll(withClouds(baseFctToken, new LexemeIdentity[] { LexemeIdentity.MIN_TEMPERATURE, LexemeIdentity.MAX_TEMPERATURE }, hints, baseFct::setCloud));

        if (!baseFct.getCloud().isPresent() && !baseFct.isCeilingAndVisibilityOk()) {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Cloud or CAVOK is missing from TAF base forecast"));
        }

        result.addAll(updateTemperatures(baseFct, baseFctToken, hints));

        builder.setBaseForecast(baseFct.build());
        return result;
    }

    private List<ConversionIssue> updateTemperatures(final TAFBaseForecastImpl.Builder builder, final Lexeme from, final ConversionHints hints) {
        final List<ConversionIssue> result = new ArrayList<>();
        final List<TAFAirTemperatureForecast> temps = new ArrayList<>();
        TAFAirTemperatureForecastImpl.Builder airTemperatureForecast;

        //Find a pair of max & min temperatures:
        Lexeme maxTempToken = from.findNext(LexemeIdentity.MAX_TEMPERATURE);
        Lexeme minTempToken;

        while (maxTempToken != null) {
            final ConversionIssue issue = checkBeforeAnyOf(maxTempToken, new LexemeIdentity[] { LexemeIdentity.MIN_TEMPERATURE });
            if (issue != null) {
                result.add(issue);
            } else {
                minTempToken = maxTempToken.findNext(LexemeIdentity.MIN_TEMPERATURE);
                if (minTempToken != null) {
                    airTemperatureForecast = TAFAirTemperatureForecastImpl.builder();
                    Integer day = minTempToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    Integer hour = minTempToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    Double value = minTempToken.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);

                    if (day != null && hour != null) {
                        airTemperatureForecast.setMinTemperatureTime(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHour(day, hour)));
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
                        airTemperatureForecast.setMaxTemperatureTime(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHour(day, hour)));
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
                    result.add(
                            new ConversionIssue(ConversionIssue.Type.SYNTAX, "Missing min temperature pair for max temperature " + maxTempToken.getTACToken()));
                }
            }
            maxTempToken = maxTempToken.findNext(LexemeIdentity.MAX_TEMPERATURE);
        }
        if (!temps.isEmpty()) {
            builder.setTemperatures(temps);
        }
        return result;
    }

    protected List<ConversionIssue> addChangeForecast(final TAFImpl.Builder builder, final Lexeme changeFctToken, final ConversionHints hints) {
        final List<ConversionIssue> result = new ArrayList<>();
        final ConversionIssue issue = checkBeforeAnyOf(changeFctToken, new LexemeIdentity[] { LexemeIdentity.REMARKS_START });
        if (issue != null) {
            result.add(issue);
            return result;
        }
        final List<TAFChangeForecast> changeForecasts;
        if (builder.getChangeForecasts().isPresent()) {
            changeForecasts = builder.getChangeForecasts().get();
        } else {
            changeForecasts = new ArrayList<>();
        }

        //PROB30 [TEMPO] or PROB40 [TEMPO] or BECMG or TEMPO or FM
        final TAFForecastChangeIndicator.ForecastChangeIndicatorType type = changeFctToken.getParsedValue(Lexeme.ParsedValueName.TYPE,
                TAFForecastChangeIndicator.ForecastChangeIndicatorType.class);
        if (changeFctToken.hasNext()) {
            final Lexeme next = changeFctToken.getNext();
            if (LexemeIdentity.REMARKS_START != next.getIdentityIfAcceptable() && LexemeIdentity.END_TOKEN != next.getIdentityIfAcceptable()) {
                final TAFChangeForecastImpl.Builder changeFct = TAFChangeForecastImpl.builder();
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
                        final Integer day = changeFctToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                        final Integer hour = changeFctToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                        final Integer minute = changeFctToken.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                        if (hour != null && minute != null) {
                            final PartialDateTime partialDateTime =
                                    day == null ? PartialDateTime.ofHourMinute(hour, minute) : PartialDateTime.ofDayHourMinute(day, hour, minute);
                            changeFct.setPeriodOfChange(PartialOrCompleteTimePeriod.builder()//
                                    .setStartTime(PartialOrCompleteTimeInstant.of(partialDateTime))//
                                    .build());
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

    private List<ConversionIssue> updateChangeForecastContents(final TAFChangeForecastImpl.Builder builder,
            final TAFForecastChangeIndicator.ForecastChangeIndicatorType type, final Lexeme from, final ConversionHints hints) {
        final List<ConversionIssue> result = new ArrayList<>();

        //FM case has already been handled in the calling code:
        if (TAFForecastChangeIndicator.ForecastChangeIndicatorType.FROM != type) {
            final Lexeme timeGroup = from.findNext(LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP);
            if (timeGroup != null) {
                final LexemeIdentity[] before = { LexemeIdentity.SURFACE_WIND, LexemeIdentity.CAVOK, LexemeIdentity.HORIZONTAL_VISIBILITY,
                        LexemeIdentity.WEATHER, LexemeIdentity.NO_SIGNIFICANT_WEATHER, LexemeIdentity.CLOUD };
                final ConversionIssue issue = checkBeforeAnyOf(timeGroup, before);
                if (issue != null) {
                    result.add(issue);
                } else {
                    final Integer startDay = timeGroup.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    final Integer endDay = timeGroup.getParsedValue(Lexeme.ParsedValueName.DAY2, Integer.class);
                    final Integer startHour = timeGroup.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    final Integer endHour = timeGroup.getParsedValue(Lexeme.ParsedValueName.HOUR2, Integer.class);
                    if (startHour != null && endHour != null) {
                        final PartialDateTime startTime = startDay == null ? PartialDateTime.ofHour(startHour) : PartialDateTime.ofDayHour(startDay, startHour);
                        final PartialDateTime endTime = endDay == null ? PartialDateTime.ofHour(endHour) : PartialDateTime.ofDayHour(endDay, endHour);
                        builder.setPeriodOfChange(PartialOrCompleteTimePeriod.builder()//
                                .setStartTime(PartialOrCompleteTimeInstant.of(startTime))//
                                .setEndTime(PartialOrCompleteTimeInstant.of(endTime))//
                                .build());
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing validity day, hour or minute for change group in " + timeGroup.getTACToken()));
                    }
                }
            } else {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity time for change group after " + from.getTACToken()));
            }
        }

        result.addAll(withForecastSurfaceWind(from,
                new LexemeIdentity[] { LexemeIdentity.CAVOK, LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER, LexemeIdentity.CLOUD }, hints,
                builder::setSurfaceWind));


        from.findNext(LexemeIdentity.CAVOK, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.HORIZONTAL_VISIBILITY, LexemeIdentity.WEATHER,
                    LexemeIdentity.NO_SIGNIFICANT_WEATHER, LexemeIdentity.CLOUD };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                builder.setCeilingAndVisibilityOk(true);
            }
        });

        result.addAll(
                withVisibility(from, new LexemeIdentity[] { LexemeIdentity.WEATHER, LexemeIdentity.NO_SIGNIFICANT_WEATHER, LexemeIdentity.CLOUD }, hints,
                        (measureWithOperator -> {
                            builder.setPrevailingVisibility(measureWithOperator.getMeasure());
                            builder.setPrevailingVisibilityOperator(measureWithOperator.getOperator());

                })));

        result.addAll(withWeather(from, new LexemeIdentity[] { LexemeIdentity.NO_SIGNIFICANT_WEATHER, LexemeIdentity.CLOUD }, hints, builder::setForecastWeather));

        from.findNext(LexemeIdentity.NO_SIGNIFICANT_WEATHER, (match) -> {
            final ConversionIssue issue = checkBeforeAnyOf(match, new LexemeIdentity[] { LexemeIdentity.CLOUD });
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

        result.addAll(withClouds(from, new LexemeIdentity[] {}, hints, builder::setCloud));

        //Check that all mandatory properties are given in the FM case:
        if (TAFForecastChangeIndicator.ForecastChangeIndicatorType.FROM == type) {
            if (!builder.getSurfaceWind().isPresent()) {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind is missing from TAF FM change forecast"));
            }
            if (!builder.isCeilingAndVisibilityOk()) {
                if (!builder.getPrevailingVisibility().isPresent()) {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Visibility is missing from TAF FM change forecast"));
                }

                if (!builder.getCloud().isPresent()) {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Cloud is missing from TAF FM change forecast"));
                }
            }
        }
        return result;
    }

    protected List<ConversionIssue> setFromChangeForecastEndTimes(final TAFImpl.Builder builder) {
        final List<ConversionIssue> result = new ArrayList<>();
        final Optional<List<TAFChangeForecast>> changeForecasts = builder.getChangeForecasts();
        if (changeForecasts.isPresent()) {
            TAFChangeForecastImpl.Builder toChange = null;
            int indexOfChange = -1;
            for (int i = 0; i < changeForecasts.get().size(); i++) {
                final TAFChangeForecast fct = changeForecasts.get().get(i);
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
                if (builder.getValidityTime().isPresent()) {
                    toChange.mutatePeriodOfChange(b -> b.setEndTime(builder.getValidityTime().get().getEndTime()));
                    changeForecasts.get().set(indexOfChange, toChange.build());
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Validity time period must be set before amending FM end times"));
                }
            }
        }
        return result;
    }

    private List<ConversionIssue> withForecastSurfaceWind(final Lexeme from, final LexemeIdentity[] before, final ConversionHints hints,
            final Consumer<SurfaceWind> consumer) {
        final List<ConversionIssue> result = new ArrayList<>();
        from.findNext(LexemeIdentity.SURFACE_WIND, (match) -> {
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                final SurfaceWindImpl.Builder wind = SurfaceWindImpl.builder();
                final Object direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
                final Integer meanSpeed = match.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
                final Integer gustSpeed = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                final String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                final AviationCodeListUser.RelationalOperator meanSpeedOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                        AviationCodeListUser.RelationalOperator.class);
                final AviationCodeListUser.RelationalOperator gustOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR2,
                        AviationCodeListUser.RelationalOperator.class);

                if (direction == fi.fmi.avi.converter.tac.lexer.impl.token.SurfaceWind.WindDirection.VARIABLE) {
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

    private List<ConversionIssue> withVisibility(final Lexeme from, final LexemeIdentity[] before, final ConversionHints hints,
            final Consumer<MeasureWithOperator> consumer) {
        final List<ConversionIssue> result = new ArrayList<>();
        from.findNext(LexemeIdentity.HORIZONTAL_VISIBILITY, (match) -> {
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                final MeasureWithOperator value = new MeasureWithOperator();
                final Double distance = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
                final String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                final RecognizingAviMessageTokenLexer.RelationalOperator distanceOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                        RecognizingAviMessageTokenLexer.RelationalOperator.class);
                final MetricHorizontalVisibility.DirectionValue direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION,
                        MetricHorizontalVisibility.DirectionValue.class);
                if (direction != null) {
                    result.add(
                            new ConversionIssue(ConversionIssue.Type.SYNTAX, "Directional horizontal visibility not allowed in TAF: " + match.getTACToken()));
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

    private List<ConversionIssue> withWeather(final Lexeme from, final LexemeIdentity[] before, final ConversionHints hints,
            final Consumer<List<fi.fmi.avi.model.Weather>> consumer) {
        final List<ConversionIssue> result = new ArrayList<>();
        from.findNext(LexemeIdentity.WEATHER, (match) -> {
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                final List<fi.fmi.avi.model.Weather> weather = new ArrayList<>();
                result.addAll(appendWeatherCodes(match, weather, before, hints));
                consumer.accept(weather);
            }
        });
        return result;
    }

    private List<ConversionIssue> withClouds(final Lexeme from, final LexemeIdentity[] before, final ConversionHints hints,
            final Consumer<CloudForecast> consumer) {
        final List<ConversionIssue> result = new ArrayList<>();
        from.findNext(LexemeIdentity.CLOUD, (match) -> {
            ConversionIssue issue;
            final CloudForecastImpl.Builder cloud = CloudForecastImpl.builder();
            final List<CloudLayer> layers = new ArrayList<>();
            while (match != null) {
                issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.add(issue);
                } else {
                    final fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover cover = match.getParsedValue(Lexeme.ParsedValueName.COVER,
                            fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover.class);
                    final Object value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Object.class);
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
                    } else if (fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover.NO_CLOUD_DETECTED == cover) {
                        result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "NCD not allowed in TAF"));
                    } else {
                        final CloudLayer layer = getCloudLayer(match);
                        if (layer != null) {
                            layers.add(layer);
                        } else {
                            result.add(
                                    new ConversionIssue(ConversionIssue.Type.SYNTAX, "Could not parse token " + match.getTACToken() + " as cloud " + "layer"));
                        }
                    }
                }
                match = match.findNext(LexemeIdentity.CLOUD);
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

        public void setMeasure(final NumericMeasure measure) {
            this.measure = measure;
        }

        public Optional<AviationCodeListUser.RelationalOperator> getOperator() {
            if (operator == null) {
                return Optional.empty();
            } else {
                return Optional.of(operator);
            }
        }

        public void setOperator(final AviationCodeListUser.RelationalOperator operator) {
            this.operator = operator;
        }
    }
}
