package fi.fmi.avi.converter.tac.airmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.impl.token.AirmetPhenomenon;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.AirspaceImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.immutable.TacGeometryImpl;
import fi.fmi.avi.model.immutable.TacOrGeoGeometryImpl;
import fi.fmi.avi.model.immutable.UnitPropertyGroupImpl;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.AirmetCloudLevels;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.SigmetIntensityChange;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;
import fi.fmi.avi.model.sigmet.immutable.AirmetCloudLevelsImpl;
import fi.fmi.avi.model.sigmet.immutable.AirmetReferenceImpl;
import fi.fmi.avi.model.sigmet.immutable.AirmetWindImpl;
import fi.fmi.avi.model.sigmet.immutable.AirmetReferenceImpl.Builder;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.AviationCodeListUser.AirmetPhenomenonParamInfo;
import fi.fmi.avi.model.AviationCodeListUser.PermissibleUsage;
import fi.fmi.avi.model.AviationCodeListUser.PermissibleUsageReason;
import fi.fmi.avi.model.AviationCodeListUser.WeatherCausingVisibilityReduction;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;
public abstract class AIRMETTACParserBase<T extends AIRMET> extends AbstractTACParser<T> {

    protected static final LexemeIdentity[] zeroOrOneAllowed = {LexemeIdentity.AIRMET_START,  /* LexemeIdentity.AIRSPACE_DESIGNATOR, */ LexemeIdentity.SEQUENCE_DESCRIPTOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.VALID_TIME,
            LexemeIdentity.CORRECTION, LexemeIdentity.AMENDMENT, LexemeIdentity.CANCELLATION, LexemeIdentity.NIL, LexemeIdentity.MIN_TEMPERATURE,
            LexemeIdentity.MAX_TEMPERATURE, LexemeIdentity.REMARKS_START };
    protected AviMessageLexer lexer;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    private boolean sequenceContains(LexemeSequence seq, List<LexemeIdentity> wanted) {
      for (Lexeme l: seq.getLexemes()){
          for (LexemeIdentity id: wanted){
              if (id.equals(l.getIdentity())){
                  return true;
              }
          }
      }
      return false;
    }

    protected TacOrGeoGeometry parseGeometry(LexemeSequence seq, AIRMETImpl.Builder builder){
        TacOrGeoGeometryImpl.Builder geomBuilder=TacOrGeoGeometryImpl.builder();
        Lexeme firstLexeme = seq.getFirstLexeme();
        if (LexemeIdentity.WHITE_SPACE.equals(firstLexeme.getIdentity())) {
            firstLexeme = firstLexeme.getNext();
        }
        if (LexemeIdentity.SIGMET_ENTIRE_AREA.equals(firstLexeme.getIdentity())){
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            geomBuilder.setEntireArea(true);

            //TODO geomBuilder.setGeoGeometry(getFirGeometry());
        } else if (LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(firstLexeme.getIdentity())){
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            Double lat = firstLexeme.getParsedValue(VALUE, Double.class);
            Double lon = firstLexeme.getParsedValue(VALUE2, Double.class);
            PointGeometryImpl.Builder pointBuilder = PointGeometryImpl.builder();
            pointBuilder.setCrs(CoordinateReferenceSystemImpl.wgs84());
            pointBuilder.addCoordinates(lat, lon);
            geomBuilder.setGeoGeometry(pointBuilder.build());
        } else if (LexemeIdentity.SIGMET_WITHIN.equals(firstLexeme.getIdentity())){
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
            tacGeometryBuilder.setData(sb.toString());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
        } else if (LexemeIdentity.SIGMET_BETWEEN_LATLON.equals(firstLexeme.getIdentity())){
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
        } else if (LexemeIdentity.SIGMET_OUTSIDE_LATLON.equals(firstLexeme.getIdentity())){
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
        } else if (LexemeIdentity.SIGMET_APRX_LINE.equals(firstLexeme.getIdentity())){
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
        } else if (LexemeIdentity.SIGMET_LINE.equals(firstLexeme.getIdentity())){
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());

        }
        return geomBuilder.build();
    }

    protected String getLevelUnit(String level) {
        return level;
    }

    protected void parseLevelMovingIntensity(LexemeSequence seq,
        PhenomenonGeometryWithHeightImpl.Builder phenBuilder,
        ConversionResult<AIRMETImpl> result,
        String input) {
        seq.getFirstLexeme().findNext(LexemeIdentity.SIGMET_LEVEL, (match) -> {
            String modifier = match.getParsedValue(LEVEL_MODIFIER, String.class);
            String lowerLimit = match.getParsedValue(VALUE, String.class);
            String lowerUnit = match.getParsedValue(UNIT, String.class);
            String upperLimit = match.getParsedValue(VALUE2, String.class);
            String upperUnit = match.getParsedValue(UNIT2, String.class);
            if (lowerLimit!=null) {
                if (upperLimit!=null) {
                    if ("SFC".equals(lowerLimit)) {
                        // BETW_SFC
                        phenBuilder.setLowerLimit(NumericMeasureImpl.of(0, "FT"));
                        phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), getLevelUnit(upperUnit)));
                    } else {
                        // BETW
                        phenBuilder.setLowerLimit(NumericMeasureImpl.of(Double.parseDouble(lowerLimit), getLevelUnit(lowerUnit)));
                        phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), getLevelUnit(upperUnit)));
                    }
                } else if ("ABV".equals(modifier)) {
                    // ABV
                    phenBuilder.setLowerLimit(NumericMeasureImpl.of(Double.parseDouble(lowerLimit), getLevelUnit(lowerUnit)));
                    phenBuilder.setLowerLimitOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                } else {
                    // AT
                    phenBuilder.setLowerLimit(NumericMeasureImpl.of(Double.parseDouble(lowerLimit), getLevelUnit(lowerUnit)));
                }
            } else if (upperLimit!=null) {
                if ("TOP ABV".equals(modifier)) {
                    // TOP ABV
                    phenBuilder.setUpperLimitOperator(AviationCodeListUser.RelationalOperator.ABOVE);
                    phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), getLevelUnit(upperUnit)));
                } else if ("TOP BLW".equals(modifier)) {
                    // TOP BLW
                    phenBuilder.setUpperLimitOperator(AviationCodeListUser.RelationalOperator.BELOW);
                    phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), getLevelUnit(upperUnit)));
                } else if ("TOP".equals(modifier)){
                    //TOP
                    phenBuilder.setUpperLimit(NumericMeasureImpl.of(Double.parseDouble(upperLimit), getLevelUnit(upperUnit)));
                } else {
                    //ERROR
                }
            }
        });
        seq.getFirstLexeme().findNext(LexemeIdentity.SIGMET_MOVING, (match) -> {
            if (!match.getParsedValue(STATIONARY, Boolean.class).equals(Boolean.TRUE))  {
                String[] windDirs={"N", "NNE", "NE", "NNE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW",
                              "WSW", "W", "WNW", "NW", "NNW"};
                ArrayList<String> windDirList = new ArrayList<>(Arrays.asList(windDirs));
                Double movingSpeed=match.getParsedValue(ParsedValueName.VALUE, Double.class);
                String movingDirection = match.getParsedValue(ParsedValueName.DIRECTION, String.class);
                String unit = match.getParsedValue(ParsedValueName.UNIT, String.class);

                phenBuilder.setMovingSpeed(NumericMeasureImpl.of(movingSpeed,unit));
                if (windDirList.contains(movingDirection)){
                    phenBuilder.setMovingDirection(NumericMeasureImpl.of(22.5*windDirList.indexOf(movingDirection), "degrees"));
                }
            } else {
                phenBuilder.setMovingSpeed(Optional.empty());
                phenBuilder.setMovingDirection(Optional.empty());
            }
        });

        seq.getFirstLexeme().findNext(LexemeIdentity.SIGMET_INTENSITY, (match) -> {
            String intensityChange=match.getParsedValue(INTENSITY, String.class);
            if (intensityChange!=null) {
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
        Lexeme first = seq.getFirstLexeme();
        Boolean isForecast=first.getParsedValue(IS_FORECAST, Boolean.class);
        if (isForecast) {
            phenBuilder.setAnalysisType(SigmetAnalysisType.FORECAST);
        } else {
            phenBuilder.setAnalysisType(SigmetAnalysisType.OBSERVATION);
        }

        Integer analysisHour=first.getParsedValue(HOUR1, Integer.class);
        Integer analysisMinute=first.getParsedValue(MINUTE1, Integer.class);
        if (analysisHour!=null) {
            PartialOrCompleteTimeInstant.Builder timeBuilder = PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.of(-1, analysisHour, analysisMinute, ZoneOffset.UTC));
            PartialOrCompleteTimeInstant pi = timeBuilder.build();
            phenBuilder.setTime(pi);
        }
    }

    protected ConversionResult<AIRMETImpl> convertMessageInternal(final String input, final ConversionHints hints) {
        final ConversionResult<AIRMETImpl> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        final LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (!checkAndReportLexingResult(lexed, hints, result)) {
            return result;
        }

        final Lexeme firstLexeme = lexed.getFirstLexeme();
        if (!(LexemeIdentity.AIRMET_START.equals(firstLexeme.getIdentity()))) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as SIGMET"));
            return result;
        } else if (firstLexeme.isSynthetic()) {
           // result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
           //         "Message does not start with a start token: " + firstLexeme.getTACToken()));
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

        final AIRMETImpl.Builder builder = AIRMETImpl.builder();

        lexed.getFirstLexeme().findNext(LexemeIdentity.MWO_DESIGNATOR, (match) -> {
            builder.setMeteorologicalWatchOffice(getMWOInfo("De Bilt", match.getParsedValue(VALUE, String.class)));
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.REAL_AIRMET_START, (match) -> {
            String atsu = match.getParsedValue(LOCATION_INDICATOR, String.class);
            builder.setIssuingAirTrafficServicesUnit(getFicInfo("AMSTERDAM", atsu));
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.ISSUE_TIME, (match) -> {
            String iss = match.getParsedValue(VALUE, String.class);
            System.err.println("iss:"+iss);
           // builder.setIssueTime(null);
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.FIR_DESIGNATOR, (match) -> {
            Lexeme l=match;
            StringBuilder firName=new StringBuilder();
            while (l.hasNext()&&SIGMET_FIR_NAME_WORD.equals(l.getNext().getIdentity())){
                l=l.getNext();
            }
            if (FIR_NAME.equals(l.getIdentity())){
                firName.append(l.getParsedValue(VALUE, String.class));
            }
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.VALID_TIME, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.MWO_DESIGNATOR};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                PartialOrCompleteTimePeriod.Builder validPeriod = PartialOrCompleteTimePeriod.builder();
                int dd1=match.getParsedValue(DAY1, Integer.class);
                int hh1=match.getParsedValue(HOUR1, Integer.class);
                int mm1=match.getParsedValue(MINUTE1, Integer.class);
                validPeriod.setStartTime(PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.ofDayHourMinuteZone(dd1, hh1, mm1, ZoneId.of("Z"))).build());
                int dd2=match.getParsedValue(DAY1, Integer.class);
                int hh2=match.getParsedValue(HOUR2, Integer.class);
                int mm2=match.getParsedValue(MINUTE2, Integer.class);
                validPeriod.setEndTime(PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.ofDayHourMinuteZone(dd2, hh2, mm2, ZoneId.of("Z"))).build());
                builder.setValidityPeriod(validPeriod.build());
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET validity time not given in " + input)));


        AirspaceImpl.Builder airspaceBuilder=new AirspaceImpl.Builder()
                .setDesignator("EHAA")
                .setType(Airspace.AirspaceType.FIR)
                .setName("AMSTERDAM FIR");
        builder.setAirspace(airspaceBuilder.build());

        builder.setPermissibleUsage(PermissibleUsage.OPERATIONAL);
        lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_USAGE, (match) -> {
            if ("TEST".equals(match.getParsedValue(TESTOREXERCISE, String.class))) {
                builder.setPermissibleUsageReason(PermissibleUsageReason.TEST);
            } else {
                builder.setPermissibleUsageReason(PermissibleUsageReason.EXERCISE);
            }
            String supplement = match.getParsedValue(USAGEREASON, String.class);
            if (supplement!=null) {
                builder.setPermissibleUsageSupplementary(supplement);
            }
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.SEQUENCE_DESCRIPTOR, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.VALID_TIME};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setSequenceNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET sequence descriptor not given in " + input)));


        lexed.getFirstLexeme().findNext(LexemeIdentity.AIRMET_CANCEL, (match) -> {
            Builder ref = new AirmetReferenceImpl.Builder();
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
        });
        if (!builder.isCancelMessage()){
            builder.setReportStatus(ReportStatus.NORMAL);

            lexed.getFirstLexeme().findNext(LexemeIdentity.AIRMET_PHENOMENON, (match) -> {
                String phen=match.getParsedValue(Lexeme.ParsedValueName.PHENOMENON, String.class);
                AviationCodeListUser.AeronauticalAirmetWeatherPhenomenon airmetPhenomenon = AviationCodeListUser.AeronauticalAirmetWeatherPhenomenon.valueOf(phen);
                switch (airmetPhenomenon) {
                    case SFC_VIS:
                        String vis = match.getParsedValue(Lexeme.ParsedValueName.SURFACE_VISIBILITY, String.class);
                        builder.setVisibility(NumericMeasureImpl.of(Double.valueOf(vis), "M"));
                        String obscuration = match.getParsedValue(Lexeme.ParsedValueName.SURFACE_VISIBILITY_CAUSE, String.class);
                        WeatherCausingVisibilityReduction cause = WeatherCausingVisibilityReduction.fromString(obscuration);
                        builder.setObscuration(Arrays.asList(cause));
                        break;
                    case SFC_WIND:
                        AirmetWindImpl.Builder airmetWindBuilder = new AirmetWindImpl.Builder();
                        String dir = match.getParsedValue(Lexeme.ParsedValueName.SURFACE_WIND_DIRECTION, String.class);
                        airmetWindBuilder.setDirection(NumericMeasureImpl.of(Double.parseDouble(dir), "deg"));
                        String speed = match.getParsedValue(Lexeme.ParsedValueName.SURFACE_WIND_SPEED_UNIT, String.class);
                        String unit = match.getParsedValue(Lexeme.ParsedValueName.SURFACE_WIND_SPEED, String.class);
                        airmetWindBuilder.setDirection(NumericMeasureImpl.of(Double.parseDouble(speed), unit));
                        builder.setWind(airmetWindBuilder.build());
                        break;
                    case OVC_CLD:
                    case BKN_CLD:
                        AirmetCloudLevelsImpl.Builder cldBuilder = new AirmetCloudLevelsImpl.Builder();
                        Boolean above = match.getParsedValue(Lexeme.ParsedValueName.SURFACE_VISIBILITY_CAUSE, Boolean.class);
                        if (above!=null&&above) {
                            cldBuilder.setTopAbove(above);
                        };

                        String cldUnit = match.getParsedValue(Lexeme.ParsedValueName.CLD_LEVELUNIT, String.class);
                        String base = match.getParsedValue(Lexeme.ParsedValueName.CLD_LOWLEVEL, String.class);
                        if (base!=null) {
                            int baseValue = Integer.parseInt(base);
                            cldBuilder.setCloudBase(NumericMeasureImpl.of(baseValue, cldUnit));
                        }
                        String top = match.getParsedValue(Lexeme.ParsedValueName.CLD_HIGHLEVEL, String.class);
                        if (top!=null) {
                            int topValue = Integer.parseInt(top);
                            cldBuilder.setCloudTop(NumericMeasureImpl.of(topValue, cldUnit));
                        }
                        builder.setCloudLevels(cldBuilder.build());
                        break;
                    default:
                }
                builder.setAirmetPhenomenon(AviationCodeListUser.AeronauticalAirmetWeatherPhenomenon.valueOf(phen));
            }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET phenomenon not given in " + input)));

            PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();

            // Analysisgeometry: after OBS_OR_FORECAST and before LEVEL, MOVEMENT, and INTENSITY_CHANGE

            final List<LexemeIdentity> analysisLexemes=Arrays.asList(LexemeIdentity.SIGMET_LEVEL, LexemeIdentity.SIGMET_INTENSITY, LexemeIdentity.SIGMET_MOVING);
            final List<LexemeSequence> subSequences =lexed.splitBy(LexemeIdentity.OBS_OR_FORECAST, LexemeIdentity.SIGMET_FCST_AT);
            List<TacOrGeoGeometry> analysisGeometries = new ArrayList<>();

            for (int i = 0; i < subSequences.size(); i++) {
                final LexemeSequence seq = subSequences.get(i);
                // System.err.print("["+i+"]: ");
                // Lexeme l1=seq.getFirstLexeme();
                // while (l1.hasNext()) {
                //     System.err.print(l1+" - ");
                //     l1=l1.getNext();
                // }
                // System.err.println();
                Lexeme l=seq.getFirstLexeme();

                if (LexemeIdentity.OBS_OR_FORECAST.equals(seq.getFirstLexeme().getIdentity())){
                    if (sequenceContains(seq, analysisLexemes)) {
                        parseLevelMovingIntensity(seq, phenBuilder, result, input);
                        analysisGeometries.add(parseGeometry(l.getTailSequence(), builder));
                    }
                }
            }

            phenBuilder.setGeometry(analysisGeometries.get(0)); //TODO list

            phenBuilder.setApproximateLocation(false);

            PhenomenonGeometryWithHeight phenGeom = phenBuilder.build();

            builder.setAnalysisGeometries(Arrays.asList(phenGeom));
        }

        builder.setTranslated(true);
        if (lexed.getTAC() != null) {
            builder.setTranslatedTAC(lexed.getTAC());
        }

        withTimeForTranslation(hints, builder::setTranslationTime);
        try {
            result.setConvertedMessage(builder.build());
        } catch (final IllegalStateException ignored) {
            // The message has an unset mandatory property and cannot be built, omit it from result
            System.err.println("ERR:"+result.getStatus()+" "+ignored);
        }
        return result;
    }

    private String getFirType(String firName) {
        String firType=null;
        if (firName.endsWith("FIR")) {
            firType = "FIR";
        } else if (firName.endsWith("UIR")) {
            firType = "UIR";
        } else if (firName.endsWith("CTA")) {
            firType = "CTA";
        } else if (firName.endsWith("FIR/UIR")) {
            firType = "OTHER:FIR_UIR";
        } else {
            return "OTHER:UNKNOWN";
        }
        return firType;
    }

    String getFirName(String firFullName){
        return firFullName.trim().replaceFirst("(\\w+)\\s((FIR|UIR|CTA|UIR/FIR)$)", "$1");
    }

    private UnitPropertyGroup getFicInfo(String firFullName, String icao) {
        String firName=firFullName.trim().replaceFirst("(\\w+)\\s((FIR|UIR|CTA|UIR/FIR)$)", "$1");
        UnitPropertyGroupImpl.Builder unit = new UnitPropertyGroupImpl.Builder();
        unit.setPropertyGroup(firName, icao, "FIC");
        return unit.build();
    }

    private UnitPropertyGroup getFirInfo(String firFullName, String icao) {
        String firName=getFirName(firFullName);
        UnitPropertyGroupImpl.Builder unit = new UnitPropertyGroupImpl.Builder();
        unit.setPropertyGroup(firName, icao, getFirType(firFullName));
        return unit.build();
    }

    private UnitPropertyGroup getMWOInfo(String mwoFullName, String locationIndicator) {
        String mwoName=mwoFullName.trim().replace("(\\w+)\\s(MWO$)", "$1");
        UnitPropertyGroupImpl.Builder mwo = new UnitPropertyGroupImpl.Builder();
        mwo.setPropertyGroup(mwoName, locationIndicator, "MWO");
        return mwo.build();
    }
}
