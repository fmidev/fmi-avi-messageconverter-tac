package fi.fmi.avi.converter.tac.swx;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
import fi.fmi.avi.model.immutable.MultiPolygonGeometryImpl;
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

    private final LexemeIdentity[] oneRequired = new LexemeIdentity[] {LexemeIdentity.ISSUE_TIME, LexemeIdentity.ADVISORY_NUMBER,
            LexemeIdentity.SWX_EFFECT_LABEL, LexemeIdentity.NEXT_ADVISORY, LexemeIdentity.REMARKS_START };
    private AviMessageLexer lexer;

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

        firstLexeme.findNext(LexemeIdentity.ADVISORY_STATUS_LABEL, (match) -> {
            builder.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
            final Lexeme value = match.findNext(LexemeIdentity.ADVISORY_STATUS);
            if (value == null) {
                conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Advisory status label was found, but the status could not be parsed in message\n" + input));
            } else {
                builder.setPermissibleUsageReason(value.getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class));
            }
        }, () -> {
            builder.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.OPERATIONAL);
        });

        firstLexeme.findNext(LexemeIdentity.ADVISORY_STATUS, (match) -> builder.setPermissibleUsageReason(
                match.getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class)));

        firstLexeme.findNext(LexemeIdentity.ISSUE_TIME, (match) -> {
            final Integer day = match.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
            final Integer minute = match.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
            final Integer hour = match.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
            final Integer month = match.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class);
            final Integer year = match.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class);
            if (year != null && month != null && day != null && minute != null && hour != null) {
                builder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Z"))));
            } else {
                conversionIssues.add(
                        new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing at least some of the issue time components in " + lexed.getTAC()));
            }
        });

        firstLexeme.findNext(LexemeIdentity.SWX_CENTRE, (match) -> {
            final IssuingCenterImpl.Builder issuingCenter = IssuingCenterImpl.builder();
            issuingCenter.setName(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            issuingCenter.setDesignator("SWXC");
            builder.setIssuingCenter(issuingCenter.build());
        }, () -> conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "The name of the issuing space weather center is missing")));

        firstLexeme.findNext(LexemeIdentity.ADVISORY_NUMBER,
                (match) -> builder.setAdvisoryNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class)));

        firstLexeme.findNext(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, (match) -> {
            final Lexeme value = match.findNext(LexemeIdentity.REPLACE_ADVISORY_NUMBER);
            if (value == null) {
                conversionIssues.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA,
                        "Replace Advisory number label was found, but the data could not be parsed in message\n" + input));
            } else {
                builder.setReplaceAdvisoryNumber(value.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class));
            }
        });

        firstLexeme.findNext(LexemeIdentity.SWX_EFFECT, (match) -> {
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

        final List<LexemeSequence> analysisList = lexed.splitBy(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
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
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.WARNING,
                    "Advisories should contain 5 observation/forecasts but " + analyses.size() + " were found in message:\n" + lexed.getTAC()));
        }

        firstLexeme.findNext(LexemeIdentity.REMARKS_START, (match) -> {
            final List<String> remarks = getRemarks(match, hints);
            if (!remarks.isEmpty() && (remarks.size() != 1 || !remarks.get(0).equalsIgnoreCase("NIL"))) {
                builder.setRemarks(remarks);
            }
        });

        firstLexeme.findNext(LexemeIdentity.NEXT_ADVISORY, (match) -> {
            final NextAdvisoryImpl.Builder nxt = NextAdvisoryImpl.builder();
            nxt.setTimeSpecifier(match.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));
            createCompleteTimeInstant(match, nxt::setTime);
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

    protected SpaceWeatherAdvisoryAnalysis processAnalysis(final Lexeme lexeme, final List<ConversionIssue> issues) {
        final SpaceWeatherAdvisoryAnalysisImpl.Builder builder = SpaceWeatherAdvisoryAnalysisImpl.builder();
        final List<ConversionIssue> issue = checkAnalysisLexemes(lexeme);
        if (issue.size() > 0) {
            issues.addAll(issue);
        }

        if (lexeme.getParsedValue(Lexeme.ParsedValueName.TYPE, SWXPhenomena.Type.class) == SWXPhenomena.Type.OBS) {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        } else {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        }

        Lexeme analysisLexeme = lexeme.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
        if (analysisLexeme != null) {
            createPartialTimeInstant(analysisLexeme, builder::setTime);
        } else {
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
            minLongitude = Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Double.class));
            maxLongitude = Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Double.class));
        }

        l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
        if (l != null) {
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
            while (l != null) {
                final Optional<SpaceWeatherRegion.SpaceWeatherLocation> location = Optional.ofNullable(
                        l.getParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.class));
                if (location.isPresent()) {
                    regionBuilder = SpaceWeatherRegionImpl.builder();
                    regionBuilder.setLocationIndicator(location);
                    if (minLongitude.isPresent()) {
                        regionBuilder.setLongitudeLimitMinimum(minLongitude.get());
                    }
                    if (maxLongitude.isPresent()) {
                        regionBuilder.setLongitudeLimitMaximum(maxLongitude.get());
                    }

                    if (!location.get().equals(SpaceWeatherRegion.SpaceWeatherLocation.DAYLIGHT_SIDE)) {
                        final Geometry geometry = buildMultiPolygon(location.get().getLatitudeBandMinCoordinate().get(), minLongitude.orElse(-180d),
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

    private Geometry buildMultiPolygon(final double minLatitude, final double minLongitude, final double maxLatitude, final double maxLongitude) {
        if (minLongitude >= maxLongitude && (Math.abs(minLongitude) != 180d && Math.abs(maxLongitude) != 180d)) {
            List<List<Double>> polygons = new ArrayList<>();
             if(Math.abs(minLongitude) == 0 && Math.abs(maxLongitude) == 0) {
                 polygons.add(createPolygon(minLatitude, 0d, maxLatitude, 180d));
                 polygons.add(createPolygon(minLatitude, -180d, maxLatitude, 0d));
             } else {
                 polygons.add(createPolygon(minLatitude, minLongitude, maxLatitude, 180d));
                 polygons.add(createPolygon(minLatitude, -180d, maxLatitude, maxLongitude));
             }


            return MultiPolygonGeometryImpl.builder().addAllExteriorRingPositions(polygons).setCrs(CoordinateReferenceSystemImpl.wgs84()).build();
        } else {
            List<Double> polygon;
            if(Math.abs(minLongitude) == 180d && Math.abs(maxLongitude) == 180d) {
                polygon = createPolygon(minLatitude, -180d, maxLatitude, 180d);
            } else if(Math.abs(minLongitude) == 180d || Math.abs(maxLongitude) == 180d){
                if(Math.abs(minLongitude) == 180d) {
                    polygon = createPolygon(minLatitude, -180d, maxLatitude, maxLongitude);
                } else {
                    polygon = createPolygon(minLatitude, minLongitude, maxLatitude, 180d);
                }
            } else {
                polygon = createPolygon(minLatitude, minLongitude, maxLatitude, maxLongitude);
            }

            return PolygonGeometryImpl.builder()
                    .setCrs(CoordinateReferenceSystemImpl.wgs84())
                    .addAllExteriorRingPositions(polygon)
                    .build();
        }
    }

    private List<Double> createPolygon(final double  minLat, final double  minLon, final double  maxLat, final double  maxLon) {
        final List<Double> coordinates = new ArrayList<>();

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

        return coordinates;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private AirspaceVolume buildAirspaceVolume(final Geometry geometry, final Optional<NumericMeasure> lowerLimit,
            final Optional<NumericMeasure> upperLimit, final Optional<AviationCodeListUser.RelationalOperator> verticalLimitOperator,
            final List<ConversionIssue> issues) {
        final AirspaceVolumeImpl.Builder volumeBuilder = AirspaceVolumeImpl.builder()//
                .setHorizontalProjection(geometry)//
                .setLowerLimitReference("STD");
        if (lowerLimit.isPresent()) {
            if (upperLimit.isPresent()) {
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
                    volumeBuilder.setUpperLimit(upperLimit);
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                            "Airspace upper limit given, but missing the relational operator " + AviationCodeListUser.RelationalOperator.BELOW));
                }
            }
        }
        return volumeBuilder.build();
    }

    protected void createPartialTimeInstant(final Lexeme lexeme, final Consumer<PartialOrCompleteTimeInstant> consumer) {
        final Integer day = lexeme.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
        final Integer minute = lexeme.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
        final Integer hour = lexeme.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
        if (day != null && minute != null && hour != null) {
            consumer.accept(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHourMinuteZone(day, hour, minute, ZoneId.of("Z"))));
        }
    }

    protected void createCompleteTimeInstant(final Lexeme lexeme, final Consumer<PartialOrCompleteTimeInstant> consumer) {

        final Integer year = lexeme.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class);
        final Integer month = lexeme.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class);
        final Integer day = lexeme.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
        final Integer minute = lexeme.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
        final Integer hour = lexeme.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);

        if (year != null && month != null && day != null && minute != null && hour != null) {
            consumer.accept(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Z"))));
        }
    }
}
