package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.geoinfo.FirInfoStore;
import fi.fmi.avi.converter.tac.geoinfo.GeoUtilsTac;
import fi.fmi.avi.converter.tac.geoinfo.impl.FirInfoStoreImpl;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.Airspace.AirspaceType;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.AviationCodeListUser.PermissibleUsage;
import fi.fmi.avi.model.AviationCodeListUser.PermissibleUsageReason;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.immutable.*;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.SigmetIntensityChange;
import fi.fmi.avi.model.sigmet.immutable.SigmetReferenceImpl;
import fi.fmi.avi.model.sigmet.immutable.SigmetReferenceImpl.Builder;
import fi.fmi.avi.model.sigmet.immutable.VAInfoImpl;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.FIR_NAME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_FIR_NAME_WORD;

public abstract class SIGMETTACParserBase<T extends SIGMET> extends AbstractTACParser<T> {

    protected static final LexemeIdentity[] zeroOrOneAllowed = {LexemeIdentity.SIGMET_START,  /* LexemeIdentity.AIRSPACE_DESIGNATOR, */ LexemeIdentity.SEQUENCE_DESCRIPTOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.VALID_TIME,
            LexemeIdentity.CORRECTION, LexemeIdentity.AMENDMENT, LexemeIdentity.CANCELLATION, LexemeIdentity.NIL, LexemeIdentity.MIN_TEMPERATURE,
            LexemeIdentity.MAX_TEMPERATURE, LexemeIdentity.REMARKS_START};
    protected AviMessageLexer lexer;

    protected FirInfoStore firInfo = null;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    private boolean sequenceContains(LexemeSequence seq, List<LexemeIdentity> wanted) {
        for (Lexeme l : seq.getLexemes()) {
            for (LexemeIdentity id : wanted) {
                if (id.equals(l.getIdentity())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected TacOrGeoGeometry parseGeometry(LexemeSequence seq, SIGMETImpl.Builder builder) {
        TacOrGeoGeometryImpl.Builder geomBuilder = TacOrGeoGeometryImpl.builder();
        String firName = builder.getAirspace().getDesignator();
        Lexeme firstLexeme = seq.getFirstLexeme();
        if (LexemeIdentity.WHITE_SPACE.equals(firstLexeme.getIdentity())) {
            firstLexeme = firstLexeme.getNext();
        }
        if (LexemeIdentity.SIGMET_ENTIRE_AREA.equals(firstLexeme.getIdentity())) {
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            geomBuilder.setEntireArea(true);
        } else if (LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(firstLexeme.getIdentity())) {
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            Double lat = firstLexeme.getParsedValue(VALUE, Double.class);
            Double lon = firstLexeme.getParsedValue(VALUE2, Double.class);
            PointGeometryImpl.Builder pointBuilder = PointGeometryImpl.builder();
            pointBuilder.setCrs(CoordinateReferenceSystemImpl.wgs84());
            pointBuilder.addCoordinates(lat, lon);
            geomBuilder.setGeoGeometry(pointBuilder.build());
        } else if (LexemeIdentity.SIGMET_WITHIN.equals(firstLexeme.getIdentity())) {
            final List<LexemeIdentity> polygonLexemes = Arrays.asList(LexemeIdentity.POLYGON_COORDINATE_PAIR, LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR, LexemeIdentity.WHITE_SPACE);
            StringBuilder sb = new StringBuilder();
            sb.append(firstLexeme.getTACToken());
            Lexeme l = firstLexeme.getNext();
            PolygonGeometryImpl.Builder polygonBuilder = PolygonGeometryImpl.builder();
            polygonBuilder.setCrs(CoordinateReferenceSystemImpl.wgs84());
            while (polygonLexemes.contains(l.getIdentity())) {
                if (LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(l.getIdentity())) {
                    Double lat = l.getParsedValue(VALUE, Double.class);
                    Double lon = l.getParsedValue(VALUE2, Double.class);
                    polygonBuilder.addExteriorRingPositions(lat, lon);
                }
                sb.append(" ");
                sb.append(l.getTACToken());
                l = l.getNext();
            }
            geomBuilder.setGeoGeometry(polygonBuilder.build());
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(sb.toString());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
        } else if (LexemeIdentity.SIGMET_BETWEEN_LATLON.equals(firstLexeme.getIdentity())) {
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
        } else if (LexemeIdentity.SIGMET_OUTSIDE_LATLON.equals(firstLexeme.getIdentity())) {
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            geomBuilder.setGeoGeometry(GeoUtilsTac.getPolygonOutside(firstLexeme, firName, firInfo));
        } else if (LexemeIdentity.SIGMET_APRX_LINE.equals(firstLexeme.getIdentity())) {
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            geomBuilder.setGeoGeometry(GeoUtilsTac.getPolygonAprxWidth(firstLexeme, firName, firInfo));
        } else if (LexemeIdentity.SIGMET_LINE.equals(firstLexeme.getIdentity())) {
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            geomBuilder.setGeoGeometry(GeoUtilsTac.getRelativeToLine(firstLexeme, firName, firInfo));
        } else if (LexemeIdentity.SIGMET_2_LINES.equals(firstLexeme.getIdentity())) {
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            geomBuilder.setGeoGeometry(GeoUtilsTac.getRelativeTo2Lines(firstLexeme, firName, firInfo));
        } else if (LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT.equals(firstLexeme.getIdentity())) {
            System.out.println("WITHIN_RADIUS_OF!!!!! " + firstLexeme);
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setTacContent(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            geomBuilder.setGeoGeometry(GeoUtilsTac.getWithinRadius(firstLexeme));
        }
        return geomBuilder.build();
    }

    protected void parseAnalysisType(LexemeSequence seq,
                                     PhenomenonGeometryWithHeightImpl.Builder phenBuilder,
                                     ConversionResult<SIGMETImpl> result,
                                     String input) {
        Lexeme first = seq.getFirstLexeme();
        Boolean isForecast = first.getParsedValue(IS_FORECAST, Boolean.class);
        if (isForecast) {
            phenBuilder.setAnalysisType(SigmetAnalysisType.FORECAST);
        } else {
            phenBuilder.setAnalysisType(SigmetAnalysisType.OBSERVATION);
        }

        Integer analysisHour = first.getParsedValue(HOUR1, Integer.class);
        Integer analysisMinute = first.getParsedValue(MINUTE1, Integer.class);
        if ((analysisHour != null) && (analysisMinute != null)) {
            PartialOrCompleteTimeInstant.Builder timeBuilder = PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.of(-1, analysisHour, analysisMinute, ZoneOffset.UTC));
            PartialOrCompleteTimeInstant pi = timeBuilder.build();
            phenBuilder.setTime(pi);
        }
    }

    protected void parseForecastTime(LexemeSequence seq,
                                     PhenomenonGeometryImpl.Builder forecastBuilder,
                                     ConversionResult<SIGMETImpl> result,
                                     String input) {
        Lexeme first = seq.getFirstLexeme();
        Integer analysisHour = first.getParsedValue(HOUR1, Integer.class);
        Integer analysisMinute = first.getParsedValue(MINUTE1, Integer.class);
        if (analysisHour != null) {
            PartialOrCompleteTimeInstant.Builder timeBuilder = PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.of(-1, analysisHour, analysisMinute, ZoneOffset.UTC));
            PartialOrCompleteTimeInstant pi = timeBuilder.build();
            forecastBuilder.setTime(pi);
        }
    }

    protected void parseLevelMovingIntensity(LexemeSequence seq,
                                             PhenomenonGeometryWithHeightImpl.Builder phenBuilder,
                                             ConversionResult<SIGMETImpl> result,
                                             String input) {
        seq.getFirstLexeme().findNext(LexemeIdentity.SIGMET_LEVEL, (match) -> {
            String modifier = match.getParsedValue(LEVEL_MODIFIER, String.class);
            String lowerLimit = match.getParsedValue(VALUE, String.class);
            String lowerUnit = match.getParsedValue(UNIT, String.class);
            String upperLimit = match.getParsedValue(VALUE2, String.class);
            String upperUnit = match.getParsedValue(UNIT2, String.class);
            if (lowerLimit != null) {
                if (upperLimit != null) {
                    if ("SFC".equals(lowerLimit)) {
                        // BETW_SFC
                        phenBuilder.setLowerLimit(NumericMeasureImpl.of(0, "FT"));
                        phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), upperUnit));
                    } else {
                        // BETW
                        phenBuilder.setLowerLimit(NumericMeasureImpl.of(Double.parseDouble(lowerLimit), lowerUnit));
                        phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), upperUnit));
                    }
                } else if ("ABV".equals(modifier)) {
                    // ABV
                    phenBuilder.setLowerLimit(NumericMeasureImpl.of(Double.parseDouble(lowerLimit), lowerUnit));
                    phenBuilder.setLowerLimitOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                } else {
                    // AT
                    phenBuilder.setLowerLimit(NumericMeasureImpl.of(Double.parseDouble(lowerLimit), lowerUnit));
                }
            } else if (upperLimit != null) {
                if ("TOP ABV".equals(modifier)) {
                    // TOP ABV
                    phenBuilder.setUpperLimitOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                    phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), upperUnit));
                } else if ("TOP BLW".equals(modifier)) {
                    // TOP BLW
                    phenBuilder.setUpperLimitOperator(AviationCodeListUser.RelationalOperator.BELOW);
                    phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), upperUnit));
                } else if ("TOP".equals(modifier)) {
                    //TOP
                    phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), upperUnit));
                } else {
                    //ERROR
                }
            }
        });
        seq.getFirstLexeme().findNext(LexemeIdentity.SIGMET_MOVING, (match) -> {
            if (!match.getParsedValue(STATIONARY, Boolean.class).equals(Boolean.TRUE)) {
                String[] windDirs = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW",
                        "WSW", "W", "WNW", "NW", "NNW"};
                ArrayList<String> windDirList = new ArrayList<>(Arrays.asList(windDirs));
                Double movingSpeed = match.getParsedValue(ParsedValueName.VALUE, Double.class);
                String movingDirection = match.getParsedValue(ParsedValueName.DIRECTION, String.class);
                String unit = match.getParsedValue(ParsedValueName.UNIT, String.class);

                phenBuilder.setMovingSpeed(NumericMeasureImpl.of(movingSpeed, unit));
                if (windDirList.contains(movingDirection)) {
                    phenBuilder.setMovingDirection(NumericMeasureImpl.of(22.5 * windDirList.indexOf(movingDirection), "degrees"));
                }
            } else {
                phenBuilder.setMovingSpeed(Optional.empty());
                phenBuilder.setMovingDirection(Optional.empty());
            }
        });

        seq.getFirstLexeme().findNext(LexemeIdentity.SIGMET_INTENSITY, (match) -> {
            String intensityChange = match.getParsedValue(INTENSITY, String.class);
            if (intensityChange != null) {
                switch (intensityChange) {
                    case "NC":
                        phenBuilder.setIntensityChange(SigmetIntensityChange.NO_CHANGE);
                        break;
                    case "INTSF":
                        phenBuilder.setIntensityChange(SigmetIntensityChange.INTENSIFYING);
                        break;
                    case "WKN":
                        phenBuilder.setIntensityChange(SigmetIntensityChange.WEAKENING);
                        break;
                }
            }
        });

    }

    protected ConversionResult<SIGMETImpl> convertMessageInternal(final String input, final ConversionHints hints) {
        final ConversionResult<SIGMETImpl> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        final LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (firInfo == null) {
            firInfo = new FirInfoStoreImpl();
        }
        if (!checkAndReportLexingResult(lexed, hints, result)) {
            return result;
        }

        final Lexeme firstLexeme = lexed.getFirstLexeme();
        if (!(LexemeIdentity.SIGMET_START.equals(firstLexeme.getIdentity()))) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as SIGMET"));
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

        final SIGMETImpl.Builder builder = SIGMETImpl.builder();

        String atsu = firstLexeme.getParsedValue(LOCATION_INDICATOR, String.class);
        builder.setIssuingAirTrafficServicesUnit(getFicInfo("AMSTERDAM", atsu));

        lexed.getFirstLexeme().findNext(LexemeIdentity.SEQUENCE_DESCRIPTOR, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[]{LexemeIdentity.VALID_TIME};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setSequenceNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET sequence descriptor not given in " + input)));

        lexed.getFirstLexeme().findNext(LexemeIdentity.VALID_TIME, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[]{LexemeIdentity.MWO_DESIGNATOR};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                PartialOrCompleteTimePeriod.Builder validPeriod = PartialOrCompleteTimePeriod.builder();
                final Integer dd1 = match.getParsedValue(DAY1, Integer.class);
                final Integer hh1 = match.getParsedValue(HOUR1, Integer.class);
                final Integer mm1 = match.getParsedValue(MINUTE1, Integer.class);
                final PartialDateTime startTime = PartialDateTime.ofDayHourMinute(dd1, hh1, mm1);
                // validPeriod.setStartTime(PartialOrCompleteTimeInstant.of(startTime));
                validPeriod.setStartTime(PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.ofDayHourMinuteZone(dd1, hh1, mm1, ZoneOffset.UTC)).build());
                final Integer dd2 = match.getParsedValue(DAY2, Integer.class);
                final Integer hh2 = match.getParsedValue(HOUR2, Integer.class);
                final Integer mm2 = match.getParsedValue(MINUTE2, Integer.class);
                final PartialDateTime endTime = PartialDateTime.ofDayHourMinute(dd2, hh2, mm2);
                // validPeriod.setEndTime(PartialOrCompleteTimeInstant.of(endTime));
                validPeriod.setEndTime(PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.ofDayHourMinuteZone(dd2, hh2, mm2, ZoneOffset.UTC)).build());                builder.setValidityPeriod(validPeriod.build());
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET validity time not given in " + input)));

        lexed.getFirstLexeme().findNext(LexemeIdentity.MWO_DESIGNATOR, (match) -> {
            builder.setMeteorologicalWatchOffice(getMWOInfo("De Bilt", match.getParsedValue(VALUE, String.class)));
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.FIR_DESIGNATOR, (match) -> {
            StringBuilder firName = new StringBuilder();
            String firType;
            Lexeme l = match;
            String designator = l.getTACToken();

            while (((l = l.getNext()) != null) && SIGMET_FIR_NAME_WORD.equals(l.getIdentity())) {
                firName.append(l.getTACToken());
            }
            if (FIR_NAME.equals(l.getIdentity())) {
                firName.append(" ");
                firName.append(l.getTACToken());
            }
            firType = l.getParsedValue(FIR_TYPE, String.class);
            AirspaceImpl.Builder airspaceBuilder = new AirspaceImpl.Builder()
                    .setDesignator(designator)
                    .setType(AirspaceType.valueOf(firType.replace("/", "_")))
                    .setName(firName.toString());
            builder.setAirspace(airspaceBuilder.build());
        });

        builder.setPermissibleUsage(PermissibleUsage.OPERATIONAL);
        lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_USAGE, (match) -> {
            if ("TEST".equals(match.getParsedValue(TESTOREXERCISE, String.class))) {
                builder.setPermissibleUsageReason(PermissibleUsageReason.TEST);
            } else {
                builder.setPermissibleUsageReason(PermissibleUsageReason.EXERCISE);
            }
            builder.setPermissibleUsage(PermissibleUsage.NON_OPERATIONAL);
            String supplement = match.getParsedValue(USAGEREASON, String.class);
            if (supplement != null) {
                builder.setPermissibleUsageSupplementary(supplement);
            }
        });


        lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_CANCEL, (match) -> {
            Builder ref = new SigmetReferenceImpl.Builder();
            ref.setSequenceNumber(match.getParsedValue(ParsedValueName.SEQUENCE_DESCRIPTOR, String.class));
            ref.setIssuingAirTrafficServicesUnit(builder.getIssuingAirTrafficServicesUnit());
            ref.setMeteorologicalWatchOffice(builder.getMeteorologicalWatchOffice());
            PartialOrCompleteTimePeriod.Builder periodBuilder = PartialOrCompleteTimePeriod.builder();
            PartialOrCompleteTimeInstant startTime = PartialOrCompleteTimeInstant.of(
                    PartialDateTime.of(
                            match.getParsedValue(DAY1, Integer.class),
                            match.getParsedValue(HOUR1, Integer.class),
                            match.getParsedValue(MINUTE1, Integer.class),
                            ZoneId.of("Z")));
            periodBuilder.setStartTime(startTime);
            PartialOrCompleteTimeInstant endTime = PartialOrCompleteTimeInstant.of(
                    PartialDateTime.of(
                            match.getParsedValue(DAY2, Integer.class),
                            match.getParsedValue(HOUR2, Integer.class),
                            match.getParsedValue(MINUTE2, Integer.class),
                            ZoneId.of("Z")));
            periodBuilder.setEndTime(endTime);
            ref.setValidityPeriod(periodBuilder.build());
            builder.setCancelledReference(ref.build());
            builder.setCancelMessage(true);

            // A MOVED_TO value determines this as a CANCEL of a VA_CLD SIGMET
            String movedToFir = match.getParsedValue(ParsedValueName.MOVED_TO, String.class);
            if (movedToFir != null) {
                VAInfoImpl.Builder vaInfoBuilder = new VAInfoImpl.Builder();
                UnitPropertyGroupImpl.Builder firBuilder = new UnitPropertyGroupImpl.Builder();
                firBuilder.setDesignator(movedToFir);
                String firName = firInfo.getFirName(movedToFir);
                if (firName != null) {
                    firBuilder.setType(getFirType(firName));
                    firBuilder.setName(firName);
                } else {
                    firBuilder.setType(AirspaceType.FIR.toString());
                    firBuilder.setName(movedToFir);
                }
                vaInfoBuilder.setVolcanicAshMovedToFIR(firBuilder.build());
                builder.setVAInfo(vaInfoBuilder.build());
            }
        });

        if (!builder.isCancelMessage()) {
            builder.setReportStatus(ReportStatus.NORMAL);

            lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_PHENOMENON, (match) -> {
                String phen = match.getParsedValue(Lexeme.ParsedValueName.PHENOMENON, String.class);
                if ("VA_CLD".equals(phen)) {
                    //Find volcano information
                    VAInfoImpl.Builder vaInfoBuilder = new VAInfoImpl.Builder();
                    VolcanoDescriptionImpl.Builder volcanoDescriptionBuilder = new VolcanoDescriptionImpl.Builder();

                    lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_VA_ERUPTION, (matchEruption) -> {
                        // volcanoDescriptionBuilder.setVolcanoEruption() //TODO: add Eruption FLAG???
                    });
                    lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_VA_NAME, (matchName) -> {
                        String name = matchName.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class);
                        volcanoDescriptionBuilder.setVolcanoName(name);
                    });
                    lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_VA_POSITION, (matchPsn) -> {
                        String latStr = matchPsn.getParsedValue(Lexeme.ParsedValueName.VOLCANO_LATITUDE, String.class);
                        String lonStr = matchPsn.getParsedValue(Lexeme.ParsedValueName.VOLCANO_LONGITUDE, String.class);
                        PointGeometry pt = GeometryHelper.parsePoint(latStr, lonStr);
                        ElevatedPointImpl.Builder pointBuilder = ElevatedPointImpl.builder();
                        pointBuilder.addCoordinates(pt.getCoordinates().get(0), pt.getCoordinates().get(1));
                        volcanoDescriptionBuilder.setVolcanoPosition(pointBuilder.build());
                    });
                    vaInfoBuilder.setVolcano(volcanoDescriptionBuilder.build());
                    phen = "VA";
                    builder.setVAInfo(vaInfoBuilder.build());
                }
                builder.setPhenomenon(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.valueOf(phen));
            }, () -> {
                // Missing phenomen is not a conversion issue for test messages
                if (!PermissibleUsageReason.TEST.equals(builder.getPermissibleUsageReason().orElse(null))) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET phenomenon not given in " + input));
                }
            });

            PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();

            PhenomenonGeometryImpl.Builder firstFcstBuilder = new PhenomenonGeometryImpl.Builder();

            // Analysisgeometry: after OBS_OR_FORECAST and before LEVEL, MOVEMENT, and INTENSITY_CHANGE

            final List<LexemeIdentity> analysisLexemes = Arrays.asList(LexemeIdentity.SIGMET_LEVEL, LexemeIdentity.SIGMET_INTENSITY, LexemeIdentity.SIGMET_MOVING);
            final List<LexemeIdentity> noVaExpLexemes = Arrays.asList(LexemeIdentity.SIGMET_NO_VA_EXP);
            final List<LexemeSequence> subSequences = lexed.splitBy(LexemeIdentity.OBS_OR_FORECAST, LexemeIdentity.SIGMET_FCST_AT);
            List<TacOrGeoGeometry> analysisGeometries = new ArrayList<>();
            List<TacOrGeoGeometry> forecastGeometries = new ArrayList<>();

            for (int i = 0; i < subSequences.size(); i++) {
                final LexemeSequence seq = subSequences.get(i);
                Lexeme l = seq.getFirstLexeme();

                if (LexemeIdentity.OBS_OR_FORECAST.equals(seq.getFirstLexeme().getIdentity())) {
                    if (sequenceContains(seq, analysisLexemes)) {
                        parseLevelMovingIntensity(seq, phenBuilder, result, input);
                    }
                    parseAnalysisType(seq, phenBuilder, result, input);
                    analysisGeometries.add(parseGeometry(l.getTailSequence(), builder));
                    System.err.println("Added:"+l.getTailSequence());
                }
                if (LexemeIdentity.SIGMET_FCST_AT.equals(seq.getFirstLexeme().getIdentity())) {
                    parseForecastTime(seq, firstFcstBuilder, result, input);
                    forecastGeometries.add(parseGeometry(l.getTailSequence(), builder));
                    if (sequenceContains(seq, noVaExpLexemes)) {
                        firstFcstBuilder.setNoVolcanicAshExpected(true);
                    }
                }
            }
            if (!analysisGeometries.isEmpty()) {
                System.out.println("analysisGeometries: "+analysisGeometries.get(0).getGeoGeometry()+ " "+analysisGeometries.get(0).getTacGeometry());
                phenBuilder.setGeometry(analysisGeometries.get(0)); //TODO list
                phenBuilder.setApproximateLocation(false);
                PhenomenonGeometryWithHeight phenGeom = phenBuilder.build();
                builder.setAnalysisGeometries(Arrays.asList(phenGeom));
            }

            if (firstFcstBuilder.getNoVolcanicAshExpected().isPresent() && firstFcstBuilder.getNoVolcanicAshExpected().get()) {
                firstFcstBuilder.clearApproximateLocation();
                PhenomenonGeometry fcstGeom = firstFcstBuilder.build();
                builder.setForecastGeometries(Arrays.asList(fcstGeom));
            } else if (forecastGeometries.size() > 0) {
                firstFcstBuilder.setGeometry(forecastGeometries.get(0)); //TODO list
                firstFcstBuilder.setApproximateLocation(false);
                PhenomenonGeometry fcstGeom = firstFcstBuilder.build();
                builder.setForecastGeometries(Arrays.asList(fcstGeom));
            }
        }
        // fixSigmetTimes(builder, ZonedDateTime.parse("2017-08-27T11:30:00Z"));
        ZonedDateTime zdt=ZonedDateTime.parse("2017-08-27T11:30:00Z");
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Z"));
        try {
            String converted = om.writeValueAsString(now);
            System.err.println("converted:"+converted);
            ZonedDateTime restored = om.readValue(converted, ZonedDateTime.class);
            System.err.println("restored: "+restored);
            System.err.println(now.equals(restored));
            System.err.println(now.isEqual(restored));
            System.err.println(restored.toEpochSecond()+ " " + now.toEpochSecond());
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        System.err.println("zdt: "+zdt.getZone()+" "+zdt.getOffset().getTotalSeconds());
        System.err.println("NOW is "+ZonedDateTime.parse("2017-08-27T11:30:00Z")+" ");
        builder.setTranslated(true);
        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }


        /* TODO Set phenomenon type based on the presence of VA info. Minimal test sigmets do not necessarily have
           VA info, so they should parse the type based on the sequence descriptor */
        if (builder.getVAInfo().isPresent()) {
            builder.setPhenomenonType(AviationCodeListUser.SigmetPhenomenonType.VOLCANIC_ASH_SIGMET);
        } else {
            builder.setPhenomenonType(AviationCodeListUser.SigmetPhenomenonType.SIGMET);
        }

        withTimeForTranslation(hints, builder::setTranslationTime);
        // ZonedDateTime dt = ZonedDateTime.of(2017, 8, 27, 11, 0, 0, 0, ZoneId.of("Z"));
        // FixSigmetTimes(builder, dt);
        try {
            result.setConvertedMessage(builder.build());
        } catch (final IllegalStateException ignored) {
            // The message has an unset mandatory property and cannot be built, omit it from result
            System.err.println("ERR:" + result.getStatus() + " " + ignored);
        }
        return result;
    }

    private String getFirType(String firName) {
        if (firName.endsWith("FIR")) {
            return "FIR";
        } else if (firName.endsWith("FIR/UIR")) { //Order matters!
            return "OTHER:FIR_UIR";
        } else if (firName.endsWith("UIR")) {
            return "UIR";
        } else if (firName.endsWith("CTA")) {
            return "CTA";
        }
        return "OTHER:UNKNOWN";
    }

    String getBaseFirName(String firFullName) {
        return firFullName.trim().replaceFirst("(\\w+)\\s((FIR|UIR|CTA|UIR/FIR)$)", "$1");
    }

    private UnitPropertyGroup getFicInfo(String firFullName, String icao) {
        String firName = getBaseFirName(firFullName);
        UnitPropertyGroupImpl.Builder unit = new UnitPropertyGroupImpl.Builder();
        unit.setPropertyGroup(firName, icao, "FIC");
        return unit.build();
    }

    private UnitPropertyGroup getFirInfo(String firFullName, String icao) {
        String firName = getBaseFirName(firFullName);
        UnitPropertyGroupImpl.Builder unit = new UnitPropertyGroupImpl.Builder();
        unit.setPropertyGroup(firName, icao, getFirType(firFullName));
        return unit.build();
    }

    private UnitPropertyGroup getMWOInfo(String mwoFullName, String locationIndicator) {
        String mwoName = mwoFullName.trim().replace("(\\w+)\\s(MWO$)", "$1");
        UnitPropertyGroupImpl.Builder mwo = new UnitPropertyGroupImpl.Builder();
        mwo.setPropertyGroup(mwoName, locationIndicator, "MWO");
        return mwo.build();
    }

    private void fixSigmetTimes(SIGMETImpl.Builder builder, ZonedDateTime reference_time) {

        //Fix validityPeriod
        PartialOrCompleteTimePeriod.Builder validityPeriodBuilder = builder.getValidityPeriodBuilder();
        validityPeriodBuilder.completePartialEndingNear(reference_time)
        .completePartialStartingNear(reference_time);
        PartialOrCompleteTimeInstant start = validityPeriodBuilder.getStartTime().get();
        PartialOrCompleteTimeInstant end = validityPeriodBuilder.getEndTime().get();
        System.out.println(">>>>>"+start);
        PartialOrCompleteTimeInstant.Builder sb = PartialOrCompleteTimeInstant.builder().mergeFrom(start).clearPartialTime();
        PartialOrCompleteTimeInstant.Builder eb = PartialOrCompleteTimeInstant.builder().mergeFrom(end).clearPartialTime();
        validityPeriodBuilder.setEndTime(eb.build()).setStartTime(sb.build());
        System.out.println(">>>>>"+sb.build());
        builder.setValidityPeriod(validityPeriodBuilder.build());

        //Fix issueTime
        PartialOrCompleteTimeInstant issueTime;
        if (builder.getIssueTime().isPresent()) {
            issueTime = builder.getIssueTime().get();
        } else {
            issueTime = builder.getValidityPeriodBuilder().getStartTime().get();
        }

        PartialOrCompleteTimeInstant.Builder tb = PartialOrCompleteTimeInstant.builder().mergeFrom(issueTime);
        System.err.println("<<<>>>>>"+tb.build());
        tb.completePartialNear(reference_time);
        System.err.println(">>>>>>>>"+tb.build());
        builder.setIssueTime(tb.build());


        //Fix analysisTimes
        if (builder.getAnalysisGeometries().isPresent()) {
            List<PhenomenonGeometryWithHeight>analysisGeometries = new ArrayList<>();
            for (final PhenomenonGeometryWithHeight geometryWithHeight : builder.getAnalysisGeometries().get()) {
                PhenomenonGeometryWithHeightImpl.Builder phenomenonGeometryWithHeightBuilder = PhenomenonGeometryWithHeightImpl.Builder.from(geometryWithHeight);
                if (geometryWithHeight.getTime().isPresent()) {
                    PartialOrCompleteTimeInstant.Builder tb1 = PartialOrCompleteTimeInstant.builder().mergeFrom(geometryWithHeight.getTime().get());
                    tb1.completePartialNear(builder.getValidityPeriodBuilder().build().getStartTime().get().getCompleteTime().get()).clearPartialTime();
                    phenomenonGeometryWithHeightBuilder.setTime(tb1.build());
                }
                analysisGeometries.add(phenomenonGeometryWithHeightBuilder.build());
            }
            builder.setAnalysisGeometries(analysisGeometries);
        }

        //Fix forecastTimes
        if (builder.getForecastGeometries().isPresent()) {
            List<PhenomenonGeometry>forecastGeometries = new ArrayList<>();
            for (final PhenomenonGeometry geometry : builder.getForecastGeometries().get()) {
                PhenomenonGeometryImpl.Builder phenomenonGeometryBuilder = PhenomenonGeometryImpl.Builder.from(geometry);
                if (geometry.getTime().isPresent()) {
                    PartialOrCompleteTimeInstant.Builder tb1 = PartialOrCompleteTimeInstant.builder().mergeFrom(geometry.getTime().get());
                    tb1.completePartialNear(builder.getValidityPeriodBuilder().build().getStartTime().get().getCompleteTime().get());
                    phenomenonGeometryBuilder.setTime(tb1.build());
                }
                forecastGeometries.add(phenomenonGeometryBuilder.build());
            }
            builder.setForecastGeometries(forecastGeometries);
        }
    }
}
