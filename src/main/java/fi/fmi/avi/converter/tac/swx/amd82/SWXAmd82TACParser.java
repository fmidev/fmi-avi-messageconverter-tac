package fi.fmi.avi.converter.tac.swx.amd82;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXPhenomena;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.VerticalLimits;
import fi.fmi.avi.model.swx.VerticalLimitsImpl;
import fi.fmi.avi.model.swx.amd82.*;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion.SpaceWeatherLocation;
import fi.fmi.avi.model.swx.amd82.immutable.*;

import javax.annotation.Nullable;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SWXAmd82TACParser extends AbstractTACParser<SpaceWeatherAdvisoryAmd82> {

    private static final Set<LexemeIdentity> SWX_LEXEME_IDENTITIES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    LexemeIdentity.ADVISORY_STATUS_LABEL,
                    LexemeIdentity.ADVISORY_STATUS,
                    LexemeIdentity.DTG_ISSUE_TIME_LABEL,
                    LexemeIdentity.ISSUE_TIME,
                    LexemeIdentity.SWX_CENTRE_LABEL,
                    LexemeIdentity.SWX_CENTRE,
                    LexemeIdentity.SWX_EFFECT_LABEL,
                    LexemeIdentity.SWX_EFFECT,
                    LexemeIdentity.ADVISORY_NUMBER_LABEL,
                    LexemeIdentity.ADVISORY_NUMBER,
                    LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL,
                    LexemeIdentity.REPLACE_ADVISORY_NUMBER,
                    LexemeIdentity.ADVISORY_PHENOMENA_LABEL,
                    LexemeIdentity.REMARKS_START,
                    LexemeIdentity.NEXT_ADVISORY_LABEL,
                    LexemeIdentity.NEXT_ADVISORY
            )));
    private static final int MAX_ADVISORIES_TO_REPLACE = 4;

    private final LexemeIdentity[] oneRequired = new LexemeIdentity[]{
            LexemeIdentity.ISSUE_TIME,
            LexemeIdentity.SWX_CENTRE,
            LexemeIdentity.SWX_EFFECT,
            LexemeIdentity.ADVISORY_NUMBER,
            LexemeIdentity.SWX_EFFECT_LABEL,
            LexemeIdentity.NEXT_ADVISORY,
            LexemeIdentity.REMARKS_START
    };
    private final Set<SpaceWeatherLocation> DAY_AND_NIGHTSIDE = Collections.unmodifiableSet(EnumSet.of(SpaceWeatherLocation.DAYSIDE, SpaceWeatherLocation.NIGHTSIDE));

    private AviMessageLexer lexer;

    private static Optional<PartialOrCompleteTimeInstant> createAnalysisTimeInstant(final Lexeme lexeme, @Nullable final PartialOrCompleteTimeInstant issueTime) {
        final Integer day = lexeme.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
        final Integer minute = lexeme.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
        final Integer hour = lexeme.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);

        if (day == null || minute == null || hour == null) {
            return Optional.empty();
        }

        final PartialDateTime time = PartialDateTime.ofDayHourMinuteZone(day, hour, minute, ZoneId.of("Z"));
        final PartialOrCompleteTimeInstant instant = issueTime != null && issueTime.getCompleteTime().isPresent()
                ? PartialOrCompleteTimeInstant.of(time, time.toZonedDateTimeNear(issueTime.getCompleteTime().get()))
                : PartialOrCompleteTimeInstant.of(time);
        return Optional.of(instant);
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

    private static void processLexeme(final ConversionResult<SpaceWeatherAdvisoryAmd82> result, final Lexeme previousLexeme,
                                      final Set<LexemeIdentity> remainingLexemeIdentities, final LexemeIdentity lexemeIdentity) {
        processLexeme(result, previousLexeme, remainingLexemeIdentities, lexemeIdentity, lexeme -> {
        });
    }

    private static void processLexeme(final ConversionResult<SpaceWeatherAdvisoryAmd82> result, final Lexeme previousLexeme,
                                      final Set<LexemeIdentity> remainingLexemeIdentities, final LexemeIdentity lexemeIdentity, final Consumer<Lexeme> lexemeHandler) {
        processLexeme(result, previousLexeme, remainingLexemeIdentities, lexemeIdentity, lexemeHandler, null);
    }

    private static void processLexeme(final ConversionResult<SpaceWeatherAdvisoryAmd82> result, final Lexeme previousLexeme,
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

    private static void checkIsNotPrependedBy(final List<ConversionIssue> issues, final Lexeme lexeme, final LexemeIdentity... prependingIdentities) {
        final ConversionIssue issue = checkBeforeAnyOf(lexeme, prependingIdentities);
        if (issue != null) {
            issues.add(issue);
        }
    }

    private static AdvisoryNumberImpl newAdvisoryNumber(final Lexeme advisoryNumberLexeme) {
        return AdvisoryNumberImpl.builder()
                .setYear(advisoryNumberLexeme.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class))
                .setSerialNumber(advisoryNumberLexeme.getParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, Integer.class))
                .build();
    }

    private static void checkCoordinateFormat(final Lexeme lexeme, final List<ConversionIssue> issues) {
        final String coordinatePair = lexeme.getTACToken();
        if (coordinatePair == null) {
            return;
        }

        for (final String coordinate : coordinatePair.split("[\\s\\-]+")) {
            if (coordinate.isEmpty()) {
                continue;
            }

            final char prefix = coordinate.charAt(0);
            final int expectedLength = (prefix == 'N' || prefix == 'S') ? 3 : 4;

            if (coordinate.length() != expectedLength) {
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                        "<" + coordinate + "> has invalid format"));
            }
        }
    }

    private static double processDegrees(final Double degrees, final String identifier, final Lexeme lexeme,
                                         final List<ConversionIssue> issues) {
        final double rounded = Math.round(degrees);
        if (degrees != rounded) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                    identifier + " in " + lexeme.getTACToken() + " contains fractional degrees parsed into <"
                            + degrees + ">, rounded to <" + rounded + ">"));
            return rounded;
        }
        return degrees;
    }

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public ConversionResult<SpaceWeatherAdvisoryAmd82> convertMessage(final String input,
                                                                      final ConversionHints hints) {
        final ConversionResult<SpaceWeatherAdvisoryAmd82> retval = new ConversionResult<>();

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

        final SpaceWeatherAdvisoryAmd82Impl.Builder builder = SpaceWeatherAdvisoryAmd82Impl.builder();

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

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ADVISORY_STATUS, (match) -> builder.setPermissibleUsageReason(
                match.getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class)));

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

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.SWX_EFFECT_LABEL);

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.SWX_EFFECT, (match) -> {
            builder.setEffect(Effect.fromString(match.getParsedValue(Lexeme.ParsedValueName.PHENOMENON, String.class)));
        }, () -> conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                "Missing space weather effect")));

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ADVISORY_NUMBER_LABEL);

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.ADVISORY_NUMBER,
                (match) -> builder.setAdvisoryNumber(newAdvisoryNumber(match)));

        processLexeme(retval, firstLexeme, remainingLexemeIdentities, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, (label) -> {
            final Lexeme firstNumber = label.findNext(LexemeIdentity.REPLACE_ADVISORY_NUMBER);
            if (firstNumber == null) {
                conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Replace advisory number is missing"));
                return;
            }

            remainingLexemeIdentities.remove(LexemeIdentity.REPLACE_ADVISORY_NUMBER);
            final ConversionIssue orderIssue = checkBeforeAnyOf(firstNumber, remainingLexemeIdentities);
            if (orderIssue != null) {
                retval.addIssue(orderIssue);
            }

            int count = 0;
            for (Lexeme number = firstNumber;
                 number != null && number.getIdentity() == LexemeIdentity.REPLACE_ADVISORY_NUMBER;
                 number = number.getNext()) {
                builder.addReplaceAdvisoryNumbers(newAdvisoryNumber(number));
                count++;
            }

            if (count > MAX_ADVISORIES_TO_REPLACE) {
                conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                        "Too many replacement advisory numbers: " + count + ", maximum is " + MAX_ADVISORIES_TO_REPLACE));
            }
        });

        conversionIssues.addAll(checkPhenomenaLabelOrder(firstLexeme, remainingLexemeIdentities));
        final List<LexemeSequence> analysisLexemeSequences = lexed.splitBy(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        conversionIssues.addAll(checkPhenomenaLabels(analysisLexemeSequences));
        final List<SpaceWeatherAdvisoryAnalysis> analyses = analysisLexemeSequences.stream()
                .map(analysisSequence -> {
                    final Lexeme analysis = analysisSequence.getFirstLexeme();
                    if (LexemeIdentity.ADVISORY_PHENOMENA_LABEL.equals(analysis.getIdentity())) {
                        return processAnalysis(analysis, builder.getIssueTime().orElse(null), conversionIssues);
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

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
            nxt.setTimeSpecifier(match.getParsedValue(Lexeme.ParsedValueName.TYPE,
                            fi.fmi.avi.converter.tac.lexer.impl.token.NextAdvisory.Type.class)
                    .getAmd82Type());

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

    private Collection<ConversionIssue> checkPhenomenaLabelOrder(final Lexeme firstLexeme,
                                                                 final Set<LexemeIdentity> remainingLexemeIdentities) {
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
        for (final LexemeSequence lexemeSequence : analysisList) {
            final Lexeme analysis = lexemeSequence.getFirstLexeme();
            if (LexemeIdentity.ADVISORY_PHENOMENA_LABEL.equals(analysis.getIdentity())) {
                if (analysis.getParsedValue(Lexeme.ParsedValueName.TYPE, SWXPhenomena.Type.class) == SWXPhenomena.Type.OBS && previousHour > 0) {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                            "Invalid token order: observation after forecast(s)"));
                } else {
                    @Nullable final Integer hour = analysis.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
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

    private SpaceWeatherAdvisoryAnalysis processAnalysis(final Lexeme lexeme,
                                                         @Nullable final PartialOrCompleteTimeInstant issueTime,
                                                         final List<ConversionIssue> issues) {
        final SpaceWeatherAdvisoryAnalysisImpl.Builder builder = SpaceWeatherAdvisoryAnalysisImpl.builder();
        final List<ConversionIssue> analysisLexemeCountIssues = checkAnalysisLexemes(lexeme);
        if (!analysisLexemeCountIssues.isEmpty()) {
            issues.addAll(analysisLexemeCountIssues);
        }

        if (lexeme.getParsedValue(Lexeme.ParsedValueName.TYPE, SWXPhenomena.Type.class) == SWXPhenomena.Type.OBS) {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        } else {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        }

        final Lexeme analysisLexeme = lexeme.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
        if (analysisLexeme != null) {
            final Optional<PartialOrCompleteTimeInstant> analysisTime = createAnalysisTimeInstant(analysisLexeme, issueTime);
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

        if (analysisLexeme.findNext(LexemeIdentity.SWX_NOT_EXPECTED) != null) {
            builder.setNilReason(SpaceWeatherAdvisoryAnalysis.NilReason.NO_SWX_EXPECTED);
            return builder.build();
        }
        if (analysisLexeme.findNext(LexemeIdentity.SWX_NOT_AVAILABLE) != null) {
            builder.setNilReason(SpaceWeatherAdvisoryAnalysis.NilReason.NO_INFORMATION_AVAILABLE);
            return builder.build();
        }

        builder.addAllIntensityAndRegions(handleIntensityAndRegions(analysisLexeme, builder.getTimeBuilder().getCompleteTime()
                .map(ZonedDateTime::toInstant).orElse(null), issues));
        if (!builder.getNilReason().isPresent() && builder.getIntensityAndRegions().isEmpty()) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                    "Missing intensity and regions or reason for missing analysis"));
        }
        return builder.build();
    }

    private List<ConversionIssue> checkAnalysisLexemes(final Lexeme lexeme) {
        final List<ConversionIssue> conversionIssues = new ArrayList<>();
        final List<ConversionIssue> exactlyOne = checkExactlyOne(lexeme.getTailSequence(),
                new LexemeIdentity[]{LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP});
        if (!exactlyOne.isEmpty()) {
            conversionIssues.addAll(exactlyOne);
        }

        final LexemeIdentity[] zeroOrOne = new LexemeIdentity[]{
                LexemeIdentity.SWX_NOT_EXPECTED,
                LexemeIdentity.SWX_NOT_AVAILABLE,
                LexemeIdentity.SWX_INTENSITY};
        final List<ConversionIssue> issues = checkZeroOrOne(lexeme.getTailSequence(), zeroOrOne);
        if (!issues.isEmpty()) {
            conversionIssues.addAll(issues);
        }

        return conversionIssues;
    }

    protected Stream<SpaceWeatherIntensityAndRegion> handleIntensityAndRegions(final Lexeme lexeme, @Nullable final Instant analysisTime, final List<ConversionIssue> issues) {
        return lexeme.getTailSequence()
                .splitBy(LexemeIdentity.SWX_INTENSITY)
                .stream()
                .<SpaceWeatherIntensityAndRegion>map(intensityAndRegionSequence -> {
                    final Lexeme intensityLexeme = intensityAndRegionSequence.getFirstLexeme();
                    if (intensityLexeme == null || intensityLexeme.getIdentity() != LexemeIdentity.SWX_INTENSITY) {
                        return null;
                    }
                    return SpaceWeatherIntensityAndRegionImpl.builder()
                            .setIntensity(Intensity.fromString(intensityLexeme.getParsedValue(Lexeme.ParsedValueName.INTENSITY, String.class)))
                            .addAllRegions(handleRegions(intensityLexeme, analysisTime, issues))
                            .build();
                })
                .filter(Objects::nonNull);
    }

    protected List<SpaceWeatherRegion> handleRegions(final Lexeme lexeme, @Nullable final Instant analysisTime,
                                                     final List<ConversionIssue> issues) {
        // 1. Discover limits
        Optional<PolygonGeometry> polygonLimit = Optional.empty();
        Optional<Double> minLongitude = Optional.empty();
        Optional<Double> maxLongitude = Optional.empty();
        final VerticalLimitsImpl.Builder verticalLimitsBuilder = VerticalLimitsImpl.builder();

        Lexeme l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        if (l != null) {
            checkIsNotPrependedBy(issues, l, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
            checkCoordinateFormat(l, issues);
            final Double minLon = l.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Double.class);
            final Double maxLon = l.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Double.class);
            if (minLon != null) {
                minLongitude = Optional.of(processDegrees(minLon, "Minimum longitude", l, issues));
            }
            if (maxLon != null) {
                maxLongitude = Optional.of(processDegrees(maxLon, "Maximum longitude", l, issues));
            }
        }

        l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
        if (l != null) {
            final Integer minValue = l.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Integer.class);
            final Integer maxValue = l.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.class);
            final String unit = l.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
            if (unit != null) {
                if (minValue != null) {
                    verticalLimitsBuilder.setLowerLimit(NumericMeasureImpl.builder().setValue(minValue.doubleValue()).setUom(unit).build());
                }
                if (maxValue != null) {
                    verticalLimitsBuilder.setUpperLimit(NumericMeasureImpl.builder().setValue(maxValue.doubleValue()).setUom(unit).build());
                }
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                        "Missing vertical limit unit for airspace volume"));
            }
            verticalLimitsBuilder.setNullableOperator(l.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR,
                    AviationCodeListUser.RelationalOperator.class));
        }

        final VerticalLimits verticalLimits = verticalLimitsBuilder.build();

        l = lexeme.findNext(LexemeIdentity.POLYGON_COORDINATE_PAIR);
        if (l != null) {
            checkIsNotPrependedBy(issues, l, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
            final PolygonGeometryImpl.Builder polyBuilder = PolygonGeometryImpl.builder()//
                    .setCrs(CoordinateReferenceSystemImpl.wgs84());
            while (l != null) {
                checkCoordinateFormat(l, issues);
                final Double lat = l.getParsedValue(Lexeme.ParsedValueName.VALUE, Double.class);
                final Double lon = l.getParsedValue(Lexeme.ParsedValueName.VALUE2, Double.class);
                polyBuilder.addExteriorRingPositions(processDegrees(lat, "Latitude", l, issues));
                polyBuilder.addExteriorRingPositions(processDegrees(lon, "Longitude", l, issues));

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
            if (!Winding.isClosedRing(polyBuilder.getExteriorRingPositions())) {
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL,
                        "Polygon coordinate pairs do not form a closed ring"));
            }
            polygonLimit = Optional.of(polyBuilder.build());
        }

        //2. Create regions applying limits
        final List<SpaceWeatherRegion> regionList = new ArrayList<>();

        l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
        if (polygonLimit.isPresent()) {
            // Explicit polygon limit provided, create a single airspace volume with no location indicator:
            if (verticalLimits.getLowerLimit().isPresent()
                    && !verticalLimits.getOperator().filter(op -> op == AviationCodeListUser.RelationalOperator.ABOVE).isPresent()) {
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "Airspace lower limit given, but missing the relational operator " + AviationCodeListUser.RelationalOperator.ABOVE));
            }
            if (verticalLimits.getUpperLimit().isPresent()
                    && !verticalLimits.getOperator().filter(op -> op == AviationCodeListUser.RelationalOperator.BELOW).isPresent()) {
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "Airspace upper limit given, but missing the relational operator " + AviationCodeListUser.RelationalOperator.BELOW));
            }
            regionList.add(SpaceWeatherRegionImpl.builder()
                    .setAirSpaceVolume(AirspaceVolumeImpl.fromPolygon(polygonLimit.get(), verticalLimits)).build());
        } else if (l != null) {
            // Create regions from each preset location (if any)
            while (l != null) {
                final boolean[] noLocationIndicator = {true};
                @Nullable final SpaceWeatherLocation location =
                        Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.LOCATION_INDICATOR, String.class))
                                .map(code -> {
                                    noLocationIndicator[0] = false;
                                    try {
                                        return SpaceWeatherLocation.fromTacCode(code);
                                    } catch (final IllegalArgumentException exception) {
                                        issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING,
                                                ConversionIssue.Type.SYNTAX, exception.getMessage(), exception));
                                        return null;
                                    }
                                })
                                .orElse(null);
                if (location != null) {
                    final SpaceWeatherRegionImpl.Builder regionBuilder = SpaceWeatherRegionImpl.builder()
                            .setLocationIndicator(location);

                    if (location == SpaceWeatherLocation.DAYSIDE && analysisTime == null) {
                        issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                "Analysis instant is not available for computing DAYLIGHT_SIDE region"));
                    }

                    checkIsNotPrependedBy(issues, l, LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT, LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);

                    minLongitude.ifPresent(regionBuilder::setLongitudeLimitMinimum);
                    maxLongitude.ifPresent(regionBuilder::setLongitudeLimitMaximum);

                    regionBuilder.setAirSpaceVolume(AirspaceVolumeImpl.fromLocationIndicator(location, analysisTime,
                            minLongitude.orElse(null), maxLongitude.orElse(null), verticalLimits));

                    regionList.add(regionBuilder.build());
                } else if (noLocationIndicator[0]) {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER,
                            "No location indicator available in Lexeme " + l));
                }
                l = l.findNext(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
            }
        } else {
            if (minLongitude.isPresent() && maxLongitude.isPresent()) {
                // No latitude bands given, but polygon(s) can be created based on the given longitudes
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "Missing latitude band(s) in " + lexeme.getFirst().getTACToken()));
            } else {
                // No polygon or latitude bands given and longitudes are completely or partially missing
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "Missing effect extent in " + lexeme.getFirst().getTACToken()));
            }
            regionList.add(SpaceWeatherRegionImpl.builder().setAirSpaceVolume(AirspaceVolumeImpl
                            .fromBounds(-90d, minLongitude.orElse(-180d), 90d, maxLongitude.orElse(180d), verticalLimits))
                    .build());
        }
        regionList.subList(0, Math.max(0, regionList.size() - 1)).stream()
                .map(region -> region.getLocationIndicator().orElse(null))
                .filter(DAY_AND_NIGHTSIDE::contains)
                .findFirst()
                .ifPresent(locationIndicator -> issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                        locationIndicator.getCode() + " is allowed only as last region")));
        return regionList;
    }

}
