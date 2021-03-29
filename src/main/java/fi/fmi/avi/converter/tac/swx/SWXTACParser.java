package fi.fmi.avi.converter.tac.swx;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXPhenomena;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.AdvisoryNumber;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.immutable.IssuingCenterImpl;
import fi.fmi.avi.model.swx.immutable.NextAdvisoryImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryAnalysisImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherRegionImpl;

public class SWXTACParser extends AbstractTACParser<SpaceWeatherAdvisory> {

    /**
     * The vertical distance is measured with an altimeter set to the standard atmosphere.
     * See
     * <a href="http://aixm.aero/sites/aixm.aero/files/imce/AIXM511HTML/AIXM/DataType_CodeVerticalReferenceType.html">AIXM 5.1.1 CodeVerticalReferenceType</a>.
     */
    private static final String STANDARD_ATMOSPHERE = "STD";
    private static final Set<LexemeIdentity> SWX_LEXEME_IDENTITIES = Collections.unmodifiableSet(new HashSet<>(
            Arrays.asList(LexemeIdentity.ADVISORY_STATUS_LABEL, LexemeIdentity.ADVISORY_STATUS, LexemeIdentity.DTG_ISSUE_TIME_LABEL, LexemeIdentity.ISSUE_TIME,
                    LexemeIdentity.SWX_CENTRE_LABEL, LexemeIdentity.SWX_CENTRE, LexemeIdentity.ADVISORY_NUMBER_LABEL, LexemeIdentity.ADVISORY_NUMBER,
                    LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, LexemeIdentity.REPLACE_ADVISORY_NUMBER, LexemeIdentity.SWX_EFFECT_LABEL,
                    LexemeIdentity.SWX_EFFECT, LexemeIdentity.ADVISORY_PHENOMENA_LABEL, LexemeIdentity.REMARKS_START, LexemeIdentity.NEXT_ADVISORY_LABEL,
                    LexemeIdentity.NEXT_ADVISORY)));

    private final LexemeIdentity[] oneRequired = new LexemeIdentity[] { LexemeIdentity.ISSUE_TIME, LexemeIdentity.SWX_CENTRE, LexemeIdentity.ADVISORY_NUMBER,
            LexemeIdentity.SWX_EFFECT_LABEL, LexemeIdentity.NEXT_ADVISORY, LexemeIdentity.REMARKS_START };
    private AviMessageLexer lexer;

    private static Optional<PartialOrCompleteTimeInstant> createPartialTimeInstant(final Lexeme lexeme) {
        final Integer day = lexeme.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
        final Integer minute = lexeme.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
        final Integer hour = lexeme.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);

        if (day != null && minute != null && hour != null) {
            return Optional.of(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHourMinuteZone(day, hour, minute, ZoneId.of("Z"))));
        }
        return Optional.empty();
    }

    private static Optional<PartialOrCompleteTimeInstant> createCompleteTimeInstant(final Lexeme lexeme) {
        final Integer year = lexeme.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class);
        final Integer month = lexeme.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class);
        final Integer day = lexeme.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
        final Integer minute = lexeme.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
        final Integer hour = lexeme.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);

        if (year != null && month != null && day != null && minute != null && hour != null) {
            return Optional.of(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Z"))));
        }
        return Optional.empty();
    }

    private static void processLexeme(final ConversionResult<SpaceWeatherAdvisory> result, final Lexeme previousLexeme,
            final Set<LexemeIdentity> remainingLexemeIdentities, final LexemeIdentity lexemeIdentity) {
        processLexeme(result, previousLexeme, remainingLexemeIdentities, lexemeIdentity, lexeme -> {
        });
    }

    private static void processLexeme(final ConversionResult<SpaceWeatherAdvisory> result, final Lexeme previousLexeme,
            final Set<LexemeIdentity> remainingLexemeIdentities, final LexemeIdentity lexemeIdentity, final Consumer<Lexeme> lexemeHandler) {
        processLexeme(result, previousLexeme, remainingLexemeIdentities, lexemeIdentity, lexemeHandler, null);
    }

    private static void processLexeme(final ConversionResult<SpaceWeatherAdvisory> result, final Lexeme previousLexeme,
            final Set<LexemeIdentity> remainingLexemeIdentities, final LexemeIdentity lexemeIdentity, final Consumer<Lexeme> lexemeHandler,
            final Lexeme.LexemeParsingNotifyer notFound) {
        previousLexeme.findNext(lexemeIdentity, (match) -> {
            remainingLexemeIdentities.remove(lexemeIdentity);
            final ConversionIssue issue = checkBeforeAnyOf(match, remainingLexemeIdentities);
            if (issue != null) {
                result.addIssue(issue);
            }
            lexemeHandler.accept(match);
        }, notFound);
    }

    private static void checkLexemeOrder(final List<ConversionIssue> issues, final Lexeme lexeme, final LexemeIdentity... prependingIdentities) {
        final ConversionIssue issue = checkBeforeAnyOf(lexeme, prependingIdentities);
        if (issue != null) {
            issues.add(issue);
        }
    }

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public ConversionResult<SpaceWeatherAdvisory> convertMessage(final String input, final ConversionHints hints) {
        final ConversionResult<SpaceWeatherAdvisory> retval = new ConversionResult<>();

        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }

        final LexemeSequence lexed = this.lexer.lexMessage(input);
        final Lexeme firstLexeme = lexed.getFirstLexeme();

        if (!LexemeIdentity.SPACE_WEATHER_ADVISORY_START.equals(firstLexeme.getIdentity())) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as Space Weather Advisory"));
            return retval;
        } else if (firstLexeme.isSynthetic()) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                    "Message does not start with a start token: " + firstLexeme.getTACToken()));
        }

        if (!endsInEndToken(lexed, hints)) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Message does not end in end token"));
            return retval;
        } else if (firstLexeme.findNext(LexemeIdentity.END_TOKEN).hasNext()) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Message has an extra end token"));
            return retval;
        }

        final List<ConversionIssue> conversionIssues = checkExactlyOne(firstLexeme.getTailSequence(), oneRequired);

        final SpaceWeatherAdvisoryImpl.Builder builder = SpaceWeatherAdvisoryImpl.builder();

        checkAndReportLexingResult(lexed, hints, retval);

        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }

        final Set<LexemeIdentity> remainingLexemeIdentities = new HashSet<>(SWX_LEXEME_IDENTITIES);

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ADVISORY_STATUS_LABEL, (match) -> {
            builder.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
            final Lexeme value = match.findNext(LexemeIdentity.ADVISORY_STATUS);
            if (value == null) {
                conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Advisory status label was found, but the status could not be parsed in message\n" + input));
            } else {
                builder.setPermissibleUsageReason(value.getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class));
            }
        }, () -> builder.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.OPERATIONAL));

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ADVISORY_STATUS, (match) -> {
            builder.setPermissibleUsageReason(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class));
        });

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.DTG_ISSUE_TIME_LABEL);

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ISSUE_TIME, (match) -> {
            final Optional<PartialOrCompleteTimeInstant> completeTimeInstant = createCompleteTimeInstant(match);
            if (completeTimeInstant.isPresent()) {
                builder.setIssueTime(completeTimeInstant.get());
            } else {
                conversionIssues.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Invalid issue time"));
            }
        });

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.SWX_CENTRE_LABEL);

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.SWX_CENTRE, (match) -> {
            final IssuingCenterImpl.Builder issuingCenter = IssuingCenterImpl.builder();
            issuingCenter.setName(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            issuingCenter.setType("OTHER:SWXC");
            builder.setIssuingCenter(issuingCenter.build());
        }, () -> conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "The name of the issuing space weather center is missing")));

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ADVISORY_NUMBER_LABEL);

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ADVISORY_NUMBER, (match) -> {
            builder.setAdvisoryNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class));
        });

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, (match) -> {
            processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.REPLACE_ADVISORY_NUMBER, (advisoryNumberMatch) -> {
                builder.setReplaceAdvisoryNumber(advisoryNumberMatch.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class));
            }, () -> conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Replace advisory number is missing")));
        });

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.SWX_EFFECT_LABEL);

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.SWX_EFFECT, (match) -> {
            final List<SpaceWeatherPhenomenon> phenomena = new ArrayList<>();
            while (match != null) {
                final SpaceWeatherPhenomenon phenomenon = match.getParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherPhenomenon.class);
                phenomena.add(phenomenon);
                match = match.findNext(LexemeIdentity.SWX_EFFECT);
            }
            //TODO: add warning if multiple effects are found
            builder.addAllPhenomena(phenomena);
        }, () -> conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                "At least 1 valid space weather effect is required.")));

        conversionIssues.addAll(checkPhenomenaLabelOrder(firstLexeme, remainingLexemeIdentities));
        final List<LexemeSequence> analysisList = lexed.splitBy(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        conversionIssues.addAll(checkPhenomenaLabels(analysisList));
        final List<SpaceWeatherAdvisoryAnalysis> analyses = new ArrayList<>();

        analysisList.forEach(analysisSequence -> {
            final Lexeme analysis = analysisSequence.getFirstLexeme();
            if (LexemeIdentity.ADVISORY_PHENOMENA_LABEL.equals(analysis.getIdentity())) {
                final SpaceWeatherAdvisoryAnalysis processedAnalysis = processAnalysis(analysisSequence.getFirstLexeme(), conversionIssues);
                if (processedAnalysis != null) {
                    analyses.add(processedAnalysis);
                }
            }
        });

        if (analyses.size() != 5) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING,
                    "Advisories should contain 5 observation/forecasts but " + analyses.size() + " were found in message:\n" + lexed.getTAC()));
        }

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.REMARKS_START, (match) -> {
            final List<String> remarks = getRemarks(match, hints);
            if (!remarks.isEmpty() && (remarks.size() != 1 || !remarks.get(0).equalsIgnoreCase("NIL"))) {
                builder.setRemarks(remarks);
            }
        });

        firstLexeme.findNext(LexemeIdentity.NEXT_ADVISORY, (match) -> {
            final NextAdvisoryImpl.Builder nxt = NextAdvisoryImpl.builder();
            nxt.setTimeSpecifier(match.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));

            if (nxt.getTimeSpecifier() != NextAdvisory.Type.NO_FURTHER_ADVISORIES) {
                final Optional<PartialOrCompleteTimeInstant> completeTimeInstant = createCompleteTimeInstant(match);
                if (completeTimeInstant.isPresent()) {
                    nxt.setTime(completeTimeInstant.get());
                } else {
                    conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing at least some of the next advisory time components"));
                }
            }
            builder.setNextAdvisory(nxt.build());
        }, () -> conversionIssues.add(
                new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Next advisory is required but was missing in message\n" + input)));

        try {
            builder.addAllAnalyses(analyses);
            retval.setConvertedMessage(builder.build());
        } catch (final IllegalStateException exception) {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, exception.getMessage()));
        }

        retval.addIssue(conversionIssues);
        return retval;
    }

    private Collection<ConversionIssue> checkPhenomenaLabelOrder(final Lexeme firstLexeme, final Set<LexemeIdentity> remainingLexemeIdentities) {
        final List<ConversionIssue> issues = new ArrayList<>();
        remainingLexemeIdentities.remove(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        Lexeme phenomenaLabel = firstLexeme.findNext(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        while (phenomenaLabel != null) {
            final ConversionIssue issue = checkBeforeAnyOf(phenomenaLabel, remainingLexemeIdentities);
            if (issue != null) {
                issues.add(issue);
            }
            phenomenaLabel = phenomenaLabel.findNext(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        }
        return issues;
    }

    private Collection<ConversionIssue> checkPhenomenaLabels(final List<LexemeSequence> analysisList) {
        final List<ConversionIssue> issues = new ArrayList<>();
        int previousHour = 0;
        for (LexemeSequence lexemeSequence : analysisList) {
            final Lexeme analysis = lexemeSequence.getFirstLexeme();
            if (LexemeIdentity.ADVISORY_PHENOMENA_LABEL.equals(analysis.getIdentity())) {
                if (analysis.getParsedValue(Lexeme.ParsedValueName.TYPE, SWXPhenomena.Type.class) == SWXPhenomena.Type.OBS && previousHour > 0) {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "Invalid token order: observation after forecast(s)"));
                } else {
                    @Nullable
                    final Integer hour = analysis.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    if (hour != null) {
                        if (hour < 6 || hour > 24 || hour % 6 != 0) {
                            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                    "Invalid forecast hour: +" + hour + " HR"));
                        }
                        if (hour < previousHour) {
                            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                                    "Invalid token order: forecast +" + hour + " HR after forecast +" + previousHour + " HR"));
                        }
                        previousHour = hour;
                    }
                }
            }
        }
        return issues;
    }

    private SpaceWeatherAdvisoryAnalysis processAnalysis(final Lexeme lexeme, final List<ConversionIssue> issues) {
        final SpaceWeatherAdvisoryAnalysisImpl.Builder builder = SpaceWeatherAdvisoryAnalysisImpl.builder();
        final List<ConversionIssue> analysisLexemeCountIssues = checkAnalysisLexemes(lexeme);
        if (analysisLexemeCountIssues.size() > 0) {
            issues.addAll(analysisLexemeCountIssues);
        }

        if (lexeme.getParsedValue(Lexeme.ParsedValueName.TYPE, SWXPhenomena.Type.class) == SWXPhenomena.Type.OBS) {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        } else {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        }

        Lexeme analysisLexeme = lexeme.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
        if (analysisLexeme != null) {
            final Optional<PartialOrCompleteTimeInstant> analysisTime = createPartialTimeInstant(analysisLexeme);
            if (analysisTime.isPresent()) {
                builder.setTime(analysisTime.get());
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Invalid analysis time in " + lexeme.getTACToken()));
                return null;
            }
        } else {
            issues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Analysis time missing in " + lexeme.getTACToken()));
            return null;
        }

        analysisLexeme = lexeme.findNext(LexemeIdentity.SWX_NOT_EXPECTED);
        if (analysisLexeme != null) {
            builder.setNilPhenomenonReason(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED);
            return builder.build();
        }
        analysisLexeme = lexeme.findNext(LexemeIdentity.SWX_NOT_AVAILABLE);
        if (analysisLexeme != null) {
            builder.setNilPhenomenonReason(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE);
            return builder.build();
        }

        final List<SpaceWeatherRegion> regionList = handleRegion(lexeme, issues);
        builder.addAllRegions(regionList);
        return builder.build();
    }

    private List<ConversionIssue> checkAnalysisLexemes(final Lexeme lexeme) {
        final List<ConversionIssue> conversionIssues = new ArrayList<>();
        final List<ConversionIssue> exactlyOne = checkExactlyOne(lexeme.getTailSequence(),
                new LexemeIdentity[] { LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP });
        if (exactlyOne.size() > 0) {
            conversionIssues.addAll(exactlyOne);
        }

        final LexemeIdentity[] zeroOrOne = new LexemeIdentity[] { LexemeIdentity.SWX_NOT_EXPECTED, LexemeIdentity.SWX_NOT_AVAILABLE };
        final List<ConversionIssue> issues = checkZeroOrOne(lexeme.getTailSequence(), zeroOrOne);
        if (issues.size() > 0) {
            conversionIssues.addAll(issues);
        }

        return conversionIssues;
    }

    protected List<SpaceWeatherRegion> handleRegion(final Lexeme lexeme, final List<ConversionIssue> issues) {
        //1. Discover limits
        Optional<PolygonGeometry> polygonLimit = Optional.empty();
        Optional<Double> minLongitude = Optional.empty();
        Optional<Double> maxLongitude = Optional.empty();
        Optional<NumericMeasure> lowerLimit = Optional.empty();
        Optional<NumericMeasure> upperLimit = Optional.empty();
        Optional<AviationCodeListUser.RelationalOperator> verticalLimitOperator = Optional.empty();

        Lexeme l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        if (l != null) {
            checkLexemeOrder(issues, l, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
            minLongitude = Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Double.class));
            maxLongitude = Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Double.class));
        }

        l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
        if (l != null) {
            checkLexemeOrder(issues, l, LexemeIdentity.POLYGON_COORDINATE_PAIR);
            final Integer minValue = l.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Integer.class);
            final Integer maxValue = l.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
            final String unit = l.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
            if (unit != null) {
                if (minValue != null) {
                    lowerLimit = Optional.of(NumericMeasureImpl.builder().setValue(minValue.doubleValue()).setUom(unit).build());
                }
                if (maxValue != null) {
                    upperLimit = Optional.of(NumericMeasureImpl.builder().setValue(maxValue.doubleValue()).setUom(unit).build());
                }
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "Missing vertical limit unit for airspace volume"));
            }
            verticalLimitOperator = Optional.ofNullable(
                    l.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.class));
        }

        l = lexeme.findNext(LexemeIdentity.POLYGON_COORDINATE_PAIR);
        if (l != null) {
            final PolygonGeometryImpl.Builder polyBuilder = PolygonGeometryImpl.builder()//
                    .setCrs(CoordinateReferenceSystemImpl.wgs84());
            while (l != null) {
                polyBuilder.addExteriorRingPositions(l.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class));
                polyBuilder.addExteriorRingPositions(l.getParsedValue(Lexeme.ParsedValueName.VALUE2, Double.class));
                if (l.hasNext() && LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR.equals(l.getNext().getIdentity())) {
                    if (l.getNext().hasNext() && LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(l.getNext().getNext().getIdentity())) {
                        l = l.getNext().getNext();
                    } else {
                        l = null;
                    }
                } else {
                    l = null;
                }
            }
            polygonLimit = Optional.of(polyBuilder.build());
        }

        //2. Create regions applying limits
        final List<SpaceWeatherRegion> regionList = new ArrayList<>();
        SpaceWeatherRegionImpl.Builder regionBuilder;

        if (polygonLimit.isPresent()) {
            //Explicit polygon limit provided, create a single airspace volume with no location indicator:
            final AirspaceVolume volume = buildAirspaceVolume(polygonLimit.get(), lowerLimit, upperLimit, verticalLimitOperator, issues);
            regionList.add(SpaceWeatherRegionImpl.builder().setAirSpaceVolume(volume).build());
        } else {
            //Create regions from each preset location (if any)
            l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
            if (l == null) {
                if (minLongitude.isPresent() && maxLongitude.isPresent()) {
                    // No latitude bands given, but polygon(s) can be created based on the given longitudes
                    issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                            "Missing latitude band(s) in " + lexeme.getFirst().getTACToken()));
                } else {
                    // No polygon or latitude bands given and longitudes are completely or partially missing
                    issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                            "Missing effect extent in " + lexeme.getFirst().getTACToken()));
                }
                final Geometry geometry = buildGeometry(-90d, minLongitude.orElse(-180d), 90d, maxLongitude.orElse(180d));
                final AirspaceVolume volume = buildAirspaceVolume(geometry, lowerLimit, upperLimit, verticalLimitOperator, issues);
                regionList.add(SpaceWeatherRegionImpl.builder().setAirSpaceVolume(volume).build());
            }
            while (l != null) {
                final Optional<SpaceWeatherRegion.SpaceWeatherLocation> location = Optional.ofNullable(
                        l.getParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.class));
                if (location.isPresent()) {
                    checkLexemeOrder(issues, l, LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
                    regionBuilder = SpaceWeatherRegionImpl.builder();
                    regionBuilder.setLocationIndicator(location);
                    if (minLongitude.isPresent()) {
                        regionBuilder.setLongitudeLimitMinimum(minLongitude.get());
                    }
                    if (maxLongitude.isPresent()) {
                        regionBuilder.setLongitudeLimitMaximum(maxLongitude.get());
                    }

                    if (!location.get().equals(SpaceWeatherRegion.SpaceWeatherLocation.DAYLIGHT_SIDE)) {
                        final Geometry geometry = buildGeometry(location.get().getLatitudeBandMinCoordinate().get(), minLongitude.orElse(-180d),
                                location.get().getLatitudeBandMaxCoordinate().get(), maxLongitude.orElse(180d));

                        final AirspaceVolume volume = buildAirspaceVolume(geometry, lowerLimit, upperLimit, verticalLimitOperator, issues);
                        regionBuilder.setAirSpaceVolume(volume);
                    }
                    regionList.add(regionBuilder.build());
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER,
                            "Location indicator not available in Lexeme " + l + ", strange"));
                }
                l = l.findNext(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
            }
        }
        return regionList;
    }

    private Geometry buildGeometry(final double minLatitude, final double minLongitude, final double maxLatitude, final double maxLongitude) {
        final double absMinLongitude = Math.abs(minLongitude);
        final double absMaxLongitude = Math.abs(maxLongitude);
        return PolygonGeometryImpl.builder()//
                .setCrs(CoordinateReferenceSystemImpl.wgs84())//
                .mutateExteriorRingPositions(coordinates -> {
                    if (absMinLongitude == 180d && absMaxLongitude == 180d) {
                        addExteriorRingPositions(coordinates, minLatitude, -180d, maxLatitude, 180d);
                    } else if (absMinLongitude == 180d) {
                        addExteriorRingPositions(coordinates, minLatitude, -180d, maxLatitude, maxLongitude);
                    } else if (absMaxLongitude == 180d) {
                        addExteriorRingPositions(coordinates, minLatitude, minLongitude, maxLatitude, 180d);
                    } else {
                        addExteriorRingPositions(coordinates, minLatitude, minLongitude, maxLatitude, maxLongitude);
                    }
                })//
                .build();
    }

    private void addExteriorRingPositions(final List<Double> coordinates, final double minLat, final double minLon, final double maxLat, final double maxLon) {
        //Upper left corner:
        coordinates.add(minLat);
        coordinates.add(minLon);

        //Lower left corner:
        coordinates.add(maxLat);
        coordinates.add(minLon);

        //lower right corner:
        coordinates.add(maxLat);
        coordinates.add(maxLon);

        //Upper right corner:
        coordinates.add(minLat);
        coordinates.add(maxLon);

        //Upper left corner (again, to close the ring):
        coordinates.add(minLat);
        coordinates.add(minLon);
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private AirspaceVolume buildAirspaceVolume(final Geometry geometry, final Optional<NumericMeasure> lowerLimit, final Optional<NumericMeasure> upperLimit,
            final Optional<AviationCodeListUser.RelationalOperator> verticalLimitOperator, final List<ConversionIssue> issues) {
        final AirspaceVolumeImpl.Builder volumeBuilder = AirspaceVolumeImpl.builder()//
                .setHorizontalProjection(geometry);
        if (lowerLimit.isPresent()) {
            volumeBuilder.setLowerLimitReference(STANDARD_ATMOSPHERE);
            if (upperLimit.isPresent()) {
                volumeBuilder.setUpperLimitReference(STANDARD_ATMOSPHERE);
                volumeBuilder.setLowerLimit(lowerLimit);
                volumeBuilder.setUpperLimit(upperLimit);
            } else if (verticalLimitOperator.isPresent() && AviationCodeListUser.RelationalOperator.ABOVE == verticalLimitOperator.get()) {
                volumeBuilder.setLowerLimit(lowerLimit);
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "Airspace lower limit given, but missing the relational operator " + AviationCodeListUser.RelationalOperator.ABOVE));
            }
        } else {
            if (upperLimit.isPresent()) {
                if (verticalLimitOperator.isPresent() && AviationCodeListUser.RelationalOperator.BELOW == verticalLimitOperator.get()) {
                    volumeBuilder.setUpperLimitReference(STANDARD_ATMOSPHERE);
                    volumeBuilder.setUpperLimit(upperLimit);
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                            "Airspace upper limit given, but missing the relational operator " + AviationCodeListUser.RelationalOperator.BELOW));
                }
            }
        }
        return volumeBuilder.build();
    }
}
