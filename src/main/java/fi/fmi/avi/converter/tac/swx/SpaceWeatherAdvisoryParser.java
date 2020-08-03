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
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomena;
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

public class SpaceWeatherAdvisoryParser extends AbstractTACParser<SpaceWeatherAdvisory> {

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

        if (LexemeIdentity.SPACE_WEATHER_ADVISORY_START != firstLexeme.getIdentity()) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as Space Weather Advisory"));
            return retval;
        } else if (firstLexeme.isSynthetic()) {
            retval.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                    "Message does not start with a start token: " + firstLexeme.getTACToken()));
        }

        SpaceWeatherAdvisoryImpl.Builder builder = SpaceWeatherAdvisoryImpl.builder();

        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }

        List<ConversionIssue> conversionIssues = setSWXIssueTime(builder, lexed, hints);

        firstLexeme.findNext(LexemeIdentity.SPACE_WEATHER_CENTRE, (match) -> {
            IssuingCenterImpl.Builder issuingCenter = IssuingCenterImpl.builder();
            issuingCenter.setName(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            issuingCenter.setDesignator("SWXC");
            builder.setIssuingCenter(issuingCenter.build());
        }, () -> {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "The name of the issuing space weather center is missing"));
        });

        firstLexeme.findNext(LexemeIdentity.ADVISORY_NUMBER, (match) -> {
            builder.setAdvisoryNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class));
        }, () -> {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "The advisory number is missing"));
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
            builder.addAllPhenomena(phenomena);
        }, () -> {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "The space weather effect is missing"));
        });

        firstLexeme.findNext(LexemeIdentity.NEXT_ADVISORY, (match) -> {
            NextAdvisoryImpl.Builder nxt = NextAdvisoryImpl.builder();
            nxt.setTimeSpecifier(match.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));
            createCompleteTimeInstant(match, nxt::setTime);
            builder.setNextAdvisory(nxt.build());
        }, () -> {
            conversionIssues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, "Next advisory information is missing"));
        });

        List<LexemeSequence> analysisList = lexed.splitBy(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        List<SpaceWeatherAdvisoryAnalysis> analyses = new ArrayList<>();

        for(LexemeSequence analysisSequence : analysisList) {
            Lexeme analysis = analysisSequence.getFirstLexeme();
            if(analysis.getIdentity() == LexemeIdentity.ADVISORY_PHENOMENA_LABEL) {
                analyses.add(processAnalysis(analysisSequence.getFirstLexeme(), conversionIssues));
            }
        }

        builder.addAllAnalyses(analyses);

        parseRemark(firstLexeme.findNext(LexemeIdentity.REMARKS_START), builder::setRemarks);

        retval.addIssue(conversionIssues);
        if(conversionIssues.size() == 0) {
            retval.setConvertedMessage(builder.build());
            retval.setStatus(ConversionResult.Status.SUCCESS);
        }

        return retval;
    }

    protected List<ConversionIssue> setSWXIssueTime(final SpaceWeatherAdvisoryImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        LexemeIdentity[] before = {LexemeIdentity.ADVISORY_NUMBER};

        return new ArrayList<>(withFoundIssueTime(lexed, before, hints, builder::setIssueTime));
    }

    protected SpaceWeatherAdvisoryAnalysis processAnalysis(final Lexeme lexeme, final List<ConversionIssue> issues) {
        SpaceWeatherAdvisoryAnalysisImpl.Builder builder = SpaceWeatherAdvisoryAnalysisImpl.builder();

        if (lexeme.getParsedValue(Lexeme.ParsedValueName.TYPE, AdvisoryPhenomena.Type.class) == AdvisoryPhenomena.Type.OBS) {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        } else {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        }

        Lexeme analysisLexeme = lexeme.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
        createPartialTimeInstant(analysisLexeme, builder::setTime);


        analysisLexeme = lexeme.findNext(LexemeIdentity.NO_SWX_EXPECTED);
        if (analysisLexeme != null) {
            builder.setNoPhenomenaExpected(true);
            return builder.build();
        }

        List<SpaceWeatherRegion> regionList = handleRegion(lexeme, issues);

        builder.setRegion(regionList);

        return builder.build();
    }

    protected List<SpaceWeatherRegion> handleRegion(final Lexeme lexeme, final List<ConversionIssue> issues) {

        //1. Discover limits
        Optional<PolygonGeometry> polygonLimit = Optional.empty();
        Optional<Double> minLongitude = Optional.empty();
        Optional<Double> maxLongitude = Optional.empty();
        Optional<NumericMeasure> verticalLimit = Optional.empty();
        Optional<AviationCodeListUser.RelationalOperator> verticalLimitOperator = Optional.empty();

        Lexeme l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_POLYGON);
        if (l != null) {
            polygonLimit = Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.VALUE, PolygonGeometry.class));
        } else {
            l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
            if (l != null) {
                minLongitude = Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Double.class));
                maxLongitude = Optional.ofNullable(l.getParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Double.class));
            }
        }

        l = lexeme.findNext(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
        if (l != null) {
            final Integer value = l.getParsedValue(Lexeme.ParsedValueName.VALUE, Integer.class);
            final String unit = l.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
            if (value != null && unit != null) {
                verticalLimit = Optional.of(NumericMeasureImpl.builder().setValue(value.doubleValue()).setUom(unit).build());
            }
            verticalLimitOperator = Optional.ofNullable(
                    l.getParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.class));
        }

        //2. Create regions applying limits
        final List<SpaceWeatherRegion> regionList = new ArrayList<>();
        SpaceWeatherRegionImpl.Builder regionBuilder;

        if (polygonLimit.isPresent()) {
            //Explicit polygon limit provided, create a single airspace volume with no location indicator:
            final AirspaceVolume volume = buildAirspaceVolume(polygonLimit.get(), verticalLimit, verticalLimitOperator, issues);
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
                                .setSrsName("http://www.opengis.net/def/crs/EPSG/0/4326")
                                .setSrsDimension(BigInteger.valueOf(2))
                                .setAxisLabels(Arrays.asList("lat", "lon"))
                                .build();
                        final AirspaceVolume volume = buildAirspaceVolume(polygon, verticalLimit, verticalLimitOperator, issues);
                        regionBuilder.setAirSpaceVolume(volume);
                    }
                    regionList.add(regionBuilder.build());
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.OTHER,
                            "Location indicator not available in " + "Lexeme " + l + ", strange"));
                }
                l = l.findNext(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
            }
        }
        return regionList;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private AirspaceVolume buildAirspaceVolume(final PolygonGeometry polygon, final Optional<NumericMeasure> verticalLimit,
            final Optional<AviationCodeListUser.RelationalOperator> verticalLimitOperator, final List<ConversionIssue> issues) {
        final AirspaceVolumeImpl.Builder volumeBuilder = AirspaceVolumeImpl.builder().setHorizontalProjection(polygon);
        if (verticalLimit.isPresent() && verticalLimitOperator.isPresent()) {
            if (verticalLimitOperator.get() == AviationCodeListUser.RelationalOperator.ABOVE) {
                volumeBuilder.setLowerLimit(verticalLimit);
                volumeBuilder.setLowerLimitReference("STD");
            } else if (verticalLimitOperator.get() == AviationCodeListUser.RelationalOperator.BELOW) {
                volumeBuilder.setUpperLimit(verticalLimit);
                volumeBuilder.setUpperLimitReference("STD");
            }
        } else if (verticalLimit.isPresent() || verticalLimitOperator.isPresent()) {
            issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                    "Either the vertical limit or the operator " + "is given, but not both"));
        }
        return volumeBuilder.build();
    }

    private void parseRemark(final Lexeme lexeme, final Consumer<List<String>> consumer) {
        Lexeme current = lexeme.getNext();
        StringBuilder remark = new StringBuilder();

        while (current.getIdentity().equals(LexemeIdentity.REMARK)) {
            appendToken(remark, current);
            current = current.getNext();
        }
        consumer.accept(Arrays.asList(remark.toString().trim()));
    }

    public StringBuilder appendToken(final StringBuilder builder, final Lexeme lexeme) {
        return builder.append(lexeme.getTACToken()).append(" ");

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
