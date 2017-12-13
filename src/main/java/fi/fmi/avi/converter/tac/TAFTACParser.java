package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.Aerodrome;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.impl.CloudForecastImpl;
import fi.fmi.avi.model.impl.NumericMeasureImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;
import fi.fmi.avi.model.taf.TAFForecast;
import fi.fmi.avi.model.taf.TAFSurfaceWind;
import fi.fmi.avi.model.taf.impl.TAFAirTemperatureForecastImpl;
import fi.fmi.avi.model.taf.impl.TAFBaseForecastImpl;
import fi.fmi.avi.model.taf.impl.TAFChangeForecastImpl;
import fi.fmi.avi.model.taf.impl.TAFImpl;
import fi.fmi.avi.model.taf.impl.TAFSurfaceWindImpl;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer;
import fi.fmi.avi.converter.tac.lexer.impl.token.ForecastChangeIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetricHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.SurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover;

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
        LexemeSequence lexed = null;
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        lexed = this.lexer.lexMessage(input, hints);

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

        if (endsInEndToken(lexed, hints)) {
            result.addIssue(checkZeroOrOne(lexed, zeroOrOneAllowed));
            TAF taf = new TAFImpl();
            result.setConvertedMessage(taf);

            if (lexed.getTAC() != null) {
                taf.setTranslatedTAC(lexed.getTAC());
                taf.setTranslationTime(ZonedDateTime.now());
            }

            findNext(Identity.CORRECTION, lexed.getFirstLexeme(), (match) -> {
                Identity[] before = { Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION, Identity
                        .SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK,
                        Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE, Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    taf.setStatus(AviationCodeListUser.TAFStatus.CORRECTION);
                }
            });

            findNext(Identity.AMENDMENT, lexed.getFirstLexeme(), (match) -> {
                Identity[] before = { Identity.AERODROME_DESIGNATOR, Identity.ISSUE_TIME, Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION, Identity
                        .SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK,
                        Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE, Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    TAF.TAFStatus status = taf.getStatus();
                    if (status != null) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
                                "TAF cannot be both " + TAF.TAFStatus.AMENDMENT + " and " + status + " at " + "the same time"));
                    } else {
                        taf.setStatus(AviationCodeListUser.TAFStatus.AMENDMENT);
                    }
                }
            });

            if (taf.getStatus() == null) {
                taf.setStatus(AviationCodeListUser.TAFStatus.NORMAL);
            }



           findNext(Identity.AERODROME_DESIGNATOR, lexed.getFirstLexeme(), (match) -> {
                Identity[] before = new Identity[] { Identity.ISSUE_TIME, Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION, Identity
                    .SURFACE_WIND,Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE,
                    Identity.MAX_TEMPERATURE, Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    Aerodrome ad = new Aerodrome(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
                    taf.setAerodrome(ad);
                }
                }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Aerodrome designator not given in " + input))
            );

            result.addIssue(updateTAFIssueTime(taf, lexed, hints));


            findNext(Identity.NIL, lexed.getFirstLexeme(), (match) -> {
                Identity[] before = new Identity[] { Identity.VALID_TIME, Identity.CANCELLATION, Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity
                        .WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE,
                        Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    taf.setStatus(AviationCodeListUser.TAFStatus.MISSING);
                    if (match.getNext() != null) {
                        Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                        if (Identity.END_TOKEN != nextTokenId && Identity.REMARKS_START != nextTokenId) {
                            result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL_ERROR, "Missing TAF message contains extra tokens after NIL: " + input));
                        }
                    }
                }
            });

            //End processing here if NIL:
            if (AviationCodeListUser.TAFStatus.MISSING == taf.getStatus()) {
                return result;
            }

            result.addIssue(updateTAFValidTime(taf, lexed, hints));


            findNext(Identity.CANCELLATION, lexed.getFirstLexeme(), (match) -> {
                Identity[] before = new Identity[] { Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.CAVOK,
                        Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE, Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
                ConversionIssue issue = checkBeforeAnyOf(match, before);
                if (issue != null) {
                    result.addIssue(issue);
                } else {
                    taf.setStatus(AviationCodeListUser.TAFStatus.CANCELLATION);
                    if (match.getNext() != null) {
                        Identity nextTokenId = match.getNext().getIdentityIfAcceptable();
                        if (Identity.END_TOKEN != nextTokenId && Identity.REMARKS_START != nextTokenId) {
                            result.addIssue(new ConversionIssue(ConversionIssue.Type.LOGICAL_ERROR, "Cancelled TAF message contains extra tokens after CNL: " + input));
                        }
                    }
                }
            });
            
            updateRemarks(result, lexed, hints);

            //End processing here if CNL:
            if (AviationCodeListUser.TAFStatus.CANCELLATION == taf.getStatus()) {
                return result;
            }

            //Split & filter in the sequences starting with FORECAST_CHANGE_INDICATOR:
            List<LexemeSequence> subSequences = lexed.splitBy(Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START)
                    .stream().filter((seq) -> Identity.FORECAST_CHANGE_INDICATOR == seq.getFirstLexeme().getIdentity()).collect(Collectors.toList());

            //Should always return at least one as long as lexed is not empty, the first one is the base forecast:
            result.addIssue(updateBaseForecast(taf, subSequences.get(0).getFirstLexeme(), hints));
            for (int i=1; i<subSequences.size(); i++) {
                result.addIssue(updateChangeForecast(taf, subSequences.get(i).getFirstLexeme(), hints));
            }

        } else {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Message does not end in end token"));
        }
        return result;
    }

    private List<ConversionIssue> updateTAFIssueTime(final TAF fct, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> retval = new ArrayList<>();
        Identity[] before = new Identity[] { Identity.NIL, Identity.VALID_TIME, Identity.CANCELLATION, Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY,
                Identity.WEATHER, Identity.CLOUD, Identity.CAVOK, Identity.MIN_TEMPERATURE,
                Identity.MAX_TEMPERATURE, Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
        retval.addAll(updateIssueTime(fct, lexed, before, hints));
        return retval;
    }

    private List<ConversionIssue> updateTAFValidTime(final TAF fct, final LexemeSequence lexed, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.VALID_TIME, lexed.getFirstLexeme(), (match) -> {
            Identity[] before = new Identity[] { Identity.CANCELLATION, Identity.SURFACE_WIND, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD,
                    Identity.CAVOK, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE,
                    Identity.FORECAST_CHANGE_INDICATOR, Identity.REMARKS_START };
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
                        fct.setPartialValidityTimePeriod(startDay, endDay, startHour, endHour);
                    } else {
                        fct.setPartialValidityTimePeriod(startDay, startHour, endHour);
                    }
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Must have at least startDay, startHour and endHour of validity " + match.getTACToken()));
                }
            }
        }, () ->
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity"))
        );
        return result;
    }

    private List<ConversionIssue> updateBaseForecast(final TAF fct, final Lexeme baseFctToken, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        TAFBaseForecast baseFct = new TAFBaseForecastImpl();

        result.addAll(updateForecastSurfaceWind(
                baseFct,
                baseFctToken,
                new Identity[]{ Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD, Identity.MIN_TEMPERATURE,
                        Identity.MAX_TEMPERATURE },
                hints)
        );

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

        result.addAll(updateVisibility(
                baseFct,
                baseFctToken,
                new Identity[] { Identity.WEATHER, Identity.CLOUD, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE },
                hints)
        );

        result.addAll(updateWeather(
                baseFct,
                baseFctToken,
                new Identity[] { Identity.CLOUD, Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE },
                hints)
        );

        result.addAll(updateClouds(
                baseFct,
                baseFctToken,
                new Identity[] { Identity.MIN_TEMPERATURE, Identity.MAX_TEMPERATURE },
                hints)
        );
        result.addAll(updateTemperatures(baseFct, baseFctToken, hints));
        fct.setBaseForecast(baseFct);
        return result;
    }

    private List<ConversionIssue> updateTemperatures(final TAFBaseForecast baseFct, final Lexeme from, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        List<TAFAirTemperatureForecast> temps = new ArrayList<>();
        TAFAirTemperatureForecast airTemperatureForecast;

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
                    airTemperatureForecast = new TAFAirTemperatureForecastImpl();
                    Integer day = minTempToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    Integer hour = minTempToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    Double value = minTempToken.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);

                    if (day != null && hour != null) {
                        airTemperatureForecast.setPartialMinTemperatureTime(day, hour);
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing day of month and/or hour of day for min forecast temperature: " + minTempToken.getTACToken()));
                    }

                    if (value != null) {
                        airTemperatureForecast.setMinTemperature(new NumericMeasureImpl(value, "degC"));
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing value for min forecast temperature: " + minTempToken.getTACToken()));
                    }

                    day = maxTempToken.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    hour = maxTempToken.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    value = maxTempToken.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);

                    if (day != null && hour != null) {
                        airTemperatureForecast.setPartialMaxTemperatureTime(day, hour);
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                "Missing day of month and/or hour of day for max forecast temperature: " + maxTempToken.getTACToken()));
                    }

                    if (value != null) {
                        airTemperatureForecast.setMaxTemperature(new NumericMeasureImpl(value, "degC"));
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing value for max forecast temperature: " + maxTempToken.getTACToken()));
                    }
                    temps.add(airTemperatureForecast);

                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Missing min temperature pair for max temperature "
                            +maxTempToken.getTACToken()));
                }
            }
            maxTempToken = findNext(Identity.MAX_TEMPERATURE, maxTempToken);
        }
        if (!temps.isEmpty()) {
            baseFct.setTemperatures(temps);
        }
        return result;
    }

    private List<ConversionIssue> updateChangeForecast(final TAF fct, final Lexeme changeFctToken, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        ConversionIssue issue = checkBeforeAnyOf(changeFctToken, new Identity[] { Identity.REMARKS_START });
        if (issue != null) {
            result.add(issue);
            return result;
        }
        List<TAFChangeForecast> changeForecasts = fct.getChangeForecasts();
        if (changeForecasts == null) {
            changeForecasts = new ArrayList<>();
            fct.setChangeForecasts(changeForecasts);
        }

        //PROB30 [TEMPO] or PROB40 [TEMPO] or BECMG or TEMPO or FM
        ForecastChangeIndicator.ForecastChangeIndicatorType type = changeFctToken.getParsedValue(Lexeme.ParsedValueName.TYPE,
                ForecastChangeIndicator.ForecastChangeIndicatorType.class);
        if (changeFctToken.hasNext()) {
            Lexeme next = changeFctToken.getNext();
            if (Identity.REMARKS_START != next.getIdentityIfAcceptable() && Identity.END_TOKEN != next.getIdentityIfAcceptable()) {
                TAFChangeForecast changeFct = new TAFChangeForecastImpl();
                switch (type) {
                    case TEMPORARY_FLUCTUATIONS:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.TEMPORARY_FLUCTUATIONS);
                        updateChangeForecastContents(changeFct, type, next, hints);
                        break;
                    case BECOMING:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.BECOMING);
                        updateChangeForecastContents(changeFct, type, next, hints);
                        break;
                    case FROM:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.FROM);
                        Integer day = next.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                        Integer hour = next.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                        Integer minute = next.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                        if (hour != null && minute != null) {
                            if (day != null) {
                                changeFct.setPartialValidityStartTime(day, hour, minute);
                            } else {
                                changeFct.setPartialValidityStartTime(-1, hour, minute);
                            }
                        } else {
                            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                                    "Missing validity start hour or minute in " + next.getTACToken()));
                        }
                        updateChangeForecastContents(changeFct, type, next, hints);
                        break;
                    case WITH_40_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_40);
                        updateChangeForecastContents(changeFct, type, next, hints);
                        break;
                    case WITH_30_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_30);
                        updateChangeForecastContents(changeFct, type, next, hints);
                        break;
                    case TEMPO_WITH_30_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_30_TEMPORARY_FLUCTUATIONS);
                        updateChangeForecastContents(changeFct, type, next, hints);
                        break;
                    case TEMPO_WITH_40_PCT_PROBABILITY:
                        changeFct.setChangeIndicator(AviationCodeListUser.TAFChangeIndicator.PROBABILITY_40_TEMPORARY_FLUCTUATIONS);
                        updateChangeForecastContents(changeFct, type, next, hints);
                        break;
                    case AT:
                    case UNTIL:
                        result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Change group " + type + " is not allowed in TAF"));
                        break;
                    default:
                        result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Unknown change group " + type));
                        break;
                }
                changeForecasts.add(changeFct);
            } else {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing change group content"));
            }
        } else {
            result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing change group content"));
        }
        return result;
    }

    private List<ConversionIssue> updateChangeForecastContents(final TAFChangeForecast fct, final ForecastChangeIndicator.ForecastChangeIndicatorType type,
            final Lexeme from, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();

        //FM case has already been handled in the calling code:
        if (ForecastChangeIndicator.ForecastChangeIndicatorType.FROM != type) {
            Lexeme timeGroup = findNext(Identity.CHANGE_FORECAST_TIME_GROUP, from);
            if (timeGroup != null) {
                Identity[] before = { Identity.SURFACE_WIND, Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER,
                        Identity.NO_SIGNIFICANT_WEATHER, Identity.CLOUD };
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
                            fct.setPartialValidityTimePeriod(startDay, endDay, startHour, endHour);
                        } else {
                            fct.setPartialValidityTimePeriod(startHour, endHour);
                        }
                    } else {
                        result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity day, hour or minute for change group in " + timeGroup.getTACToken()));
                    }
                }
            } else {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing validity time for change group after " + from.getTACToken()));
            }
        }

        result.addAll(updateForecastSurfaceWind(fct, from, new Identity[] { Identity.CAVOK, Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.CLOUD }, hints));

        findNext(Identity.CAVOK, from, (match) -> {
            Identity[] before = new Identity[] { Identity.HORIZONTAL_VISIBILITY, Identity.WEATHER, Identity.NO_SIGNIFICANT_WEATHER, Identity.CLOUD };
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                fct.setCeilingAndVisibilityOk(true);
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

        result.addAll(updateClouds(fct, from, new Identity[]{}, hints));

        return result;
    }

    private List<ConversionIssue> updateForecastSurfaceWind(final TAFForecast fct, final Lexeme from, final Identity[] before, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.SURFACE_WIND, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                TAFSurfaceWind wind = new TAFSurfaceWindImpl();
                Object direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION, Object.class);
                Integer meanSpeed = match.getParsedValue(Lexeme.ParsedValueName.MEAN_VALUE, Integer.class);
                Integer gustSpeed = match.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);

                if (direction == SurfaceWind.WindDirection.VARIABLE) {
                    wind.setVariableDirection(true);
                } else if (direction instanceof Integer) {
                    wind.setMeanWindDirection(new NumericMeasureImpl((Integer) direction, "deg"));
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind direction is missing: " + match.getTACToken()));
                }

                if (meanSpeed != null) {
                    wind.setMeanWindSpeed(new NumericMeasureImpl(meanSpeed, unit));
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind mean speed is missing: " + match.getTACToken()));
                }

                if (gustSpeed != null) {
                    wind.setWindGust(new NumericMeasureImpl(gustSpeed, unit));
                }
                fct.setSurfaceWind(wind);
            }
        }, () -> {
            if (fct instanceof TAFBaseForecast) {
                result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Surface wind is missing from TAF base forecast"));
            }
        });
        return result;
    }

    private List<ConversionIssue> updateVisibility(final TAFForecast fct, final Lexeme from, final Identity[] before, final ConversionHints hints) {
        List<ConversionIssue> result = new ArrayList<>();
        findNext(Identity.HORIZONTAL_VISIBILITY, from, (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.add(issue);
            } else {
                Double distance = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
                String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
                RecognizingAviMessageTokenLexer.RelationalOperator distanceOperator = match.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                        RecognizingAviMessageTokenLexer.RelationalOperator.class);
                MetricHorizontalVisibility.DirectionValue direction = match.getParsedValue(Lexeme.ParsedValueName.DIRECTION, MetricHorizontalVisibility.DirectionValue.class);
                if (direction != null) {
                    result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Directional horizontal visibility not allowed in TAF: " + match.getTACToken()));
                }
                if (distance != null && unit != null) {
                    fct.setPrevailingVisibility(new NumericMeasureImpl(distance, unit));
                } else {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing visibility value or unit: " + match.getTACToken()));
                }
                if (distanceOperator != null) {
                    if (RecognizingAviMessageTokenLexer.RelationalOperator.LESS_THAN == distanceOperator) {
                        fct.setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.BELOW);
                    } else {
                        fct.setPrevailingVisibilityOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                    }
                }
            }
        }, () -> {
            if (fct instanceof TAFBaseForecast) {
                if (!fct.isCeilingAndVisibilityOk()) {
                    result.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Visibility or CAVOK is missing from TAF base forecast"));
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
                        } else {
                            result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Cloud layer height is not an integer in " + match.getTACToken()));
                            height = null;
                        }
                        cloud.setVerticalVisibility(new NumericMeasureImpl(height, unit));
                    } else if (CloudCover.NO_SIG_CLOUDS == cover) {
                        cloud.setNoSignificantCloud(true);
                    } else {
                        fi.fmi.avi.model.CloudLayer layer = getCloudLayer(match);
                        if (layer != null) {
                            layers.add(layer);
                        } else {
                            result.add(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR, "Could not parse token " + match.getTACToken() + " as cloud " + "layer"));
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

}
