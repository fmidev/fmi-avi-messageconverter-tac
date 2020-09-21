package fi.fmi.avi.converter.tac.swx;

import java.math.BigInteger;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
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
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PolygonGeometry;
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

        if(!firstLexeme.getIdentity().equals(LexemeIdentity.SPACE_WEATHER_ADVISORY_START)) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as Space Weather Advisory"));
            return retval;
        } else if (firstLexeme.isSynthetic()) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                    "Message does not start with a start token: " + firstLexeme.getTACToken()));
        }

        List<ConversionIssue> conversionIssues = new ArrayList<>();

        LexemeIdentity[] exactkyOne = new LexemeIdentity[]{
                LexemeIdentity.ADVISORY_STATUS, LexemeIdentity.ISSUE_TIME, LexemeIdentity.ADVISORY_NUMBER, LexemeIdentity.SWX_EFFECT_LABEL,
                /*LexemeIdentity.SWX_EFFECT,*/ LexemeIdentity.NEXT_ADVISORY
        };
        final List<ConversionIssue> checkIssues = checkExactlyOne(firstLexeme.getTailSequence(), exactkyOne);
        if(checkIssues != null) {
            conversionIssues.addAll(checkIssues);
        }

        SpaceWeatherAdvisoryImpl.Builder builder = SpaceWeatherAdvisoryImpl.builder();

        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }

        firstLexeme.findNext(LexemeIdentity.ADVISORY_STATUS_LABEL, (match) -> {
            final Lexeme value = match.getNext();
            builder.setPermissibleUsage(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
            if (LexemeIdentity.ADVISORY_STATUS == value.getIdentity()) {
                builder.setPermissibleUsageReason(value.getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class));
            }
        });

        firstLexeme.findNext(LexemeIdentity.ADVISORY_STATUS, (match) ->{
            builder.setPermissibleUsageReason(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class));
        });

        conversionIssues.addAll(this.withFoundIssueTime(lexed, new LexemeIdentity[]{LexemeIdentity.ADVISORY_NUMBER, LexemeIdentity.SWX_EFFECT_LABEL,
                        LexemeIdentity.SWX_EFFECT, LexemeIdentity.NEXT_ADVISORY}, hints,
                builder::setIssueTime));

        firstLexeme.findNext(LexemeIdentity.SWX_CENTRE, (match) -> {
            IssuingCenterImpl.Builder issuingCenter = IssuingCenterImpl.builder();
            issuingCenter.setName(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            issuingCenter.setDesignator("SWXC");
            builder.setIssuingCenter(issuingCenter.build());
        }, () -> {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "The name of the issuing space weather center is missing"));
        });

        firstLexeme.findNext(LexemeIdentity.ADVISORY_NUMBER, (match) -> {
            builder.setAdvisoryNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class));
        });

        firstLexeme.findNext(LexemeIdentity.REPLACE_ADVISORY_NUMBER, (match) -> {
            builder.setReplaceAdvisoryNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class));
        });

        firstLexeme.findNext(LexemeIdentity.SWX_EFFECT, (match) -> {
            List<SpaceWeatherPhenomenon> phenomena = new ArrayList<>();
            while (match != null) {
                SpaceWeatherPhenomenon phenomenon = match.getParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherPhenomenon.class);
                phenomena.add(phenomenon);
                match = match.findNext(LexemeIdentity.SWX_EFFECT);
            }
            //TODO: add warning if multiple effects are found
            builder.addAllPhenomena(phenomena);
        }, () -> {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Advisory should contain at least 1 valid "
                    + "space wather effect."));
        });
        
        List<LexemeSequence> analysisList = lexed.splitBy(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        List<SpaceWeatherAdvisoryAnalysis> analyses = new ArrayList<>();

        analysisList.stream().forEach(analysisSequence -> {
            Lexeme analysis = analysisSequence.getFirstLexeme();
            //TODO: CHeck if it works
            if (analysis.getIdentity().equals(LexemeIdentity.ADVISORY_PHENOMENA_LABEL)) {
                SpaceWeatherAdvisoryAnalysis processedAnalysis = processAnalysis(analysisSequence.getFirstLexeme(), conversionIssues);
                if(processedAnalysis != null) {
                    analyses.add(processedAnalysis);
                }
            }
        });
        if(analyses.size() != 5) {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.WARNING,
                    "Advisories should contain 5 observation/forecasts but "  + analyses.size()
                    + " were recognized in advisory:\n" + lexed.getTAC()));
        }

        firstLexeme.findNext(LexemeIdentity.REMARKS_START, (match) -> {
            final List<String> remarks = getRemarks(match, hints);
            if (!remarks.isEmpty()) {
                builder.setRemarks(remarks);
            }
        });

        firstLexeme.findNext(LexemeIdentity.NEXT_ADVISORY, (match) -> {
            NextAdvisoryImpl.Builder nxt = NextAdvisoryImpl.builder();
            nxt.setTimeSpecifier(match.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));
            createCompleteTimeInstant(match, nxt::setTime);
            builder.setNextAdvisory(nxt.build());
        }, () -> {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "Next advisory information is missing"));
        });

        try {
            builder.addAllAnalyses(analyses);
            retval.setConvertedMessage(builder.build());
        } catch (final IllegalStateException ignored) {
            System.out.println(ignored.getMessage());
        }

        retval.addIssue(conversionIssues);

        return retval;
    }

    protected SpaceWeatherAdvisoryAnalysis processAnalysis(final Lexeme lexeme, final List<ConversionIssue> issues) {
        SpaceWeatherAdvisoryAnalysisImpl.Builder builder = SpaceWeatherAdvisoryAnalysisImpl.builder();
        List<ConversionIssue> issue = checkAnalysisLexemes(lexeme);
        if(issue.size() > 0) {
            issues.addAll(issue);
        }

        if (lexeme.getParsedValue(Lexeme.ParsedValueName.TYPE, SWXPhenomena.Type.class) == SWXPhenomena.Type.OBS) {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        } else {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        }

        Lexeme analysisLexeme = lexeme.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
        if(analysisLexeme != null) {
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

        List<SpaceWeatherRegion> regionList = handleRegion(lexeme, issues);

        builder.addAllRegions(regionList);

        return builder.build();
    }

    private List<ConversionIssue> checkAnalysisLexemes(Lexeme lexeme) {
        List<ConversionIssue> conversionIssues = new ArrayList<>();
        List<ConversionIssue> exactlyOne = checkExactlyOne(lexeme.getTailSequence(), new LexemeIdentity[]{LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP});
        if(exactlyOne.size() > 0) {
            conversionIssues.addAll(exactlyOne);
        }

        LexemeIdentity[] zeroOrOne = new LexemeIdentity[] {
                LexemeIdentity.SWX_NOT_EXPECTED, LexemeIdentity.SWX_NOT_AVAILABLE
        };
        List<ConversionIssue> issues = checkZeroOrOne(lexeme.getTailSequence(), zeroOrOne);
        if(issues.size() > 0) {
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
                    lowerLimit =  Optional.of(NumericMeasureImpl.builder().setValue(minValue.doubleValue()).setUom(unit).build());
                }
                if (maxValue != null) {
                    upperLimit =  Optional.of(NumericMeasureImpl.builder().setValue(maxValue.doubleValue()).setUom(unit).build());
                }
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "Missing vertical limit "
                        + "unit for airspace volume"));
            }
            verticalLimitOperator = Optional.ofNullable(
                    l.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.class));
        }

        l = lexeme.findNext(LexemeIdentity.POLYGON_COORDINATE_PAIR);
        if (l != null) {
            final PolygonGeometryImpl.Builder polyBuilder = PolygonGeometryImpl.builder()//
                    .setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326)//
                    .setAxisLabels(Arrays.asList("lat", "lon"))//
                    .setSrsDimension(BigInteger.valueOf(2));
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
                        final List<Double> coordinates = new ArrayList<>();

                        //Upper left corner:
                        location.get().getLatitudeBandMinCoordinate().ifPresent(coordinates::add);
                        coordinates.add(minLongitude.orElse(-180d));

                        //Lower left corner:
                        location.get().getLatitudeBandMaxCoordinate().ifPresent(coordinates::add);
                        coordinates.add(minLongitude.orElse(-180d));

                        //lower right corner:
                        location.get().getLatitudeBandMaxCoordinate().ifPresent(coordinates::add);
                        coordinates.add(maxLongitude.orElse(180d));

                        //Upper right corner:
                        location.get().getLatitudeBandMinCoordinate().ifPresent(coordinates::add);
                        coordinates.add(maxLongitude.orElse(180d));

                        //Upper left corner (again, to close the ring):
                        location.get().getLatitudeBandMinCoordinate().ifPresent(coordinates::add);
                        coordinates.add(minLongitude.orElse(-180d));
                        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                                .addAllExteriorRingPositions(coordinates)
                                .setSrsName(AviationCodeListUser.CODELIST_VALUE_EPSG_4326)
                                .setSrsDimension(BigInteger.valueOf(2))
                                .setAxisLabels(Arrays.asList("lat", "lon"))
                                .build();
                        final AirspaceVolume volume = buildAirspaceVolume(polygon, lowerLimit, upperLimit, verticalLimitOperator, issues);
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

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private AirspaceVolume buildAirspaceVolume(final PolygonGeometry polygon, final Optional<NumericMeasure> lowerLimit, final Optional<NumericMeasure>
            upperLimit, final Optional<AviationCodeListUser.RelationalOperator> verticalLimitOperator, final List<ConversionIssue> issues) {
        final AirspaceVolumeImpl.Builder volumeBuilder = AirspaceVolumeImpl.builder()//
                .setHorizontalProjection(polygon)//
                .setLowerLimitReference("STD");
        if (lowerLimit.isPresent()) {
            if (upperLimit.isPresent()) {
                volumeBuilder.setLowerLimit(lowerLimit);
                volumeBuilder.setUpperLimit(upperLimit);
            } else if (verticalLimitOperator.isPresent() && AviationCodeListUser.RelationalOperator.ABOVE == verticalLimitOperator.get()){
                volumeBuilder.setLowerLimit(lowerLimit);
            }  else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Airspace lower limit given, but "
                        + "missing the relational operator " +
                        AviationCodeListUser.RelationalOperator.ABOVE));
            }
        } else {
            if (upperLimit.isPresent()) {
                if (verticalLimitOperator.isPresent() && AviationCodeListUser.RelationalOperator.BELOW == verticalLimitOperator.get()) {
                    volumeBuilder.setUpperLimit(upperLimit);
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Airspace upper limit given, but "
                            + "missing the relational operator " +
                            AviationCodeListUser.RelationalOperator.BELOW));
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
