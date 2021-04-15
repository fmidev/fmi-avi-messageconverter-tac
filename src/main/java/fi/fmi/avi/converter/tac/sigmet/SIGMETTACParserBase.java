package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
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
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.SigmetIntensityChange;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;
public abstract class SIGMETTACParserBase<T extends SIGMET> extends AbstractTACParser<T> {

    protected static final LexemeIdentity[] zeroOrOneAllowed = {LexemeIdentity.SIGMET_START,  /* LexemeIdentity.AIRSPACE_DESIGNATOR, */ LexemeIdentity.SEQUENCE_DESCRIPTOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.VALID_TIME,
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

    protected TacOrGeoGeometry parseGeometry(LexemeSequence seq, SIGMETImpl.Builder builder){
        TacOrGeoGeometryImpl.Builder geomBuilder=TacOrGeoGeometryImpl.builder();
        Lexeme firstLexeme = seq.getFirstLexeme();
        if (LexemeIdentity.WHITE_SPACE.equals(firstLexeme.getIdentity())) {
            firstLexeme = firstLexeme.getNext();
        }
        System.err.println("parseGeometry("+firstLexeme+")");
        if (LexemeIdentity.SIGMET_ENTIRE_AREA.equals(firstLexeme.getIdentity())){
            System.err.println("Geometry Entire Area");

            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            //TODO geomBuilder.setEntireFir(true);
            //TODO geomBuilder.setGeoGeometry(getFirGeometry());
            System.err.println("Added TacGeometry: "+firstLexeme.getTACToken());
        } else if (LexemeIdentity.POLYGON_COORDINATE_PAIR.equals(firstLexeme.getIdentity())){
            System.err.println("Geometry Point");
            TacGeometryImpl.Builder tacGeometryBuilder = TacGeometryImpl.builder();
            tacGeometryBuilder.setData(firstLexeme.getTACToken());
            geomBuilder.setTacGeometry(tacGeometryBuilder.build());
            Double lat = firstLexeme.getParsedValue(VALUE, Double.class);
            Double lon = firstLexeme.getParsedValue(VALUE2, Double.class);
            PointGeometryImpl.Builder pointBuilder = PointGeometryImpl.builder();
            pointBuilder.setCrs(CoordinateReferenceSystemImpl.wgs84());
            pointBuilder.addCoordinates(lat, lon);
            geomBuilder.setGeoGeometry(pointBuilder.build());
            System.err.println("Added TacGeometry Point: "+firstLexeme.getTACToken());
        } else if (LexemeIdentity.SIGMET_WITHIN.equals(firstLexeme.getIdentity())){
            final List<LexemeIdentity> polygonLexemes = Arrays.asList(LexemeIdentity.POLYGON_COORDINATE_PAIR, LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR, LexemeIdentity.WHITE_SPACE);
            System.err.println("Geometry Polygon");
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
            System.err.println("Added TacGeometry: "+firstLexeme.getTACToken());
        }
        return geomBuilder.build();
    }

    protected ConversionResult<SIGMETImpl> convertMessageInternal(final String input, final ConversionHints hints) {
        final ConversionResult<SIGMETImpl> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        final LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (!checkAndReportLexingResult(lexed, hints, result)) {
            return result;
        }

        final Lexeme firstLexeme = lexed.getFirstLexeme();
        if (!(LexemeIdentity.SIGMET_START.equals(firstLexeme.getIdentity()))) {
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
        System.err.println("check0o1:"+issues.size()+" "+issues.isEmpty()+" "+lexed.getTAC());
        if (!issues.isEmpty()) {
            result.addIssue(issues);
            return result;
        }

        final SIGMETImpl.Builder builder = SIGMETImpl.builder();

        lexed.getFirstLexeme().findNext(LexemeIdentity.MWO_DESIGNATOR, (match) -> {
            builder.setMeteorologicalWatchOffice(getMWOInfo("De Bilt", match.getParsedValue(VALUE, String.class)));
        });

        lexed.getFirstLexeme().findNext(LexemeIdentity.REAL_SIGMET_START, (match) -> {
            String atsu = match.getParsedValue(LOCATION_INDICATOR, String.class);
            System.err.println("ATSU:"+atsu);
            builder.setIssuingAirTrafficServicesUnit(getFicInfo("AMSTERDAM", atsu));
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

        Lexeme lex=lexed.getFirstLexeme();
        while (lex.hasNext()) {
            System.err.println(lex+" ");
            lex=lex.getNext();
        }

        lexed.getFirstLexeme().findNext(LexemeIdentity.SEQUENCE_DESCRIPTOR, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.VALID_TIME};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setSequenceNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET sequence descriptor not given in " + input)));


        builder.setReportStatus(ReportStatus.NORMAL);

        lexed.getFirstLexeme().findNext(LexemeIdentity.PHENOMENON_SIGMET, (match) -> {
            String phen=match.getParsedValue(Lexeme.ParsedValueName.SIGMET_PHENOMENON, String.class);
            builder.setSigmetPhenomenon(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.valueOf(phen));
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET phenomenon not given in " + input)));

        PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();
        lexed.getFirstLexeme().findNext(LexemeIdentity.OBS_OR_FORECAST, (match) -> {
            Boolean isForecast=match.getParsedValue(IS_FORECAST, Boolean.class);
            if (isForecast) {
                builder.setAnalysisType(SigmetAnalysisType.FORECAST);
            } else {
                builder.setAnalysisType(SigmetAnalysisType.OBSERVATION);
            }

            Integer analysisHour=match.getParsedValue(HOUR1, Integer.class);
            Integer analysisMinute=match.getParsedValue(MINUTE1, Integer.class);
            if (analysisHour!=null) {
                PartialOrCompleteTimeInstant.Builder timeBuilder = PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.of(-1, analysisHour, analysisMinute, ZoneOffset.UTC));
                PartialOrCompleteTimeInstant pi = timeBuilder.build();
                phenBuilder.setTime(pi);
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET obs or forecast not given in " + input)));

        // Analysisgeometry: after OBS_OR_FORECAST and before LEVEL, MOVEMENT, and INTENSITY_CHANGE

        final List<LexemeIdentity> wanted=Arrays.asList(LexemeIdentity.SIGMET_LEVEL, LexemeIdentity.SIGMET_INTENSITY, LexemeIdentity.SIGMET_MOVING);
        final List<LexemeSequence> subSequences =lexed.splitBy(LexemeIdentity.OBS_OR_FORECAST, LexemeIdentity.SIGMET_FCST_AT);
        List<TacOrGeoGeometry> analisysGeometries = new ArrayList<>();
        List<TacOrGeoGeometry> forecastGeometries = new ArrayList<>();
        int cnt=0;
        boolean forecastsFound=false;

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
            cnt++;
            System.err.println("A["+cnt+"] "+l.getTailSequence().getTAC());
            if (LexemeIdentity.OBS_OR_FORECAST.equals(seq.getFirstLexeme().getIdentity())){
                if (sequenceContains(seq, wanted)) {
                    analisysGeometries.add(parseGeometry(l.getTailSequence(), builder));
                } else {
                    forecastGeometries.add(parseGeometry(l.getTailSequence(), builder));
                    forecastsFound = true;
                }
            }
        }
        /* TODO The analysisType description(s) should go into the analysisgeometry too */
        // builder.setAnalysisType(SigmetAnalysisType.FORECAST);

        /* TODO The movement description(s) should go into the analysisgeometry too */
        if (!forecastsFound) {
            // Add movement description
            lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_MOVING, (match) -> {
                String[] windDirs={"N", "NNE", "NE", "NNE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW",
                                  "WSW", "W", "WNW", "NW", "NNW"};
                ArrayList<String> windDirList = new ArrayList<>(Arrays.asList(windDirs));
                Double movingSpeed=match.getParsedValue(ParsedValueName.VALUE, Double.class);
                String movingDirection = match.getParsedValue(ParsedValueName.DIRECTION, String.class);
                String unit = match.getParsedValue(ParsedValueName.UNIT, String.class);

                builder.setMovingSpeed(NumericMeasureImpl.of(movingSpeed,unit));
                if (windDirList.contains(movingDirection)){
                    builder.setMovingDirection(NumericMeasureImpl.of(22.5*windDirList.indexOf(movingDirection), "degrees"));
                }
            });
        }

        /* TODO The intensity change(s) should go into the analysisgeometry too */
        lexed.getFirstLexeme().findNext(LexemeIdentity.SIGMET_INTENSITY, (match) -> {
            String intensityChange=match.getParsedValue(INTENSITY, String.class);
            if (intensityChange!=null) {
                switch (intensityChange) {
                case "NC":
                    builder.setIntensityChange(SigmetIntensityChange.NO_CHANGE);
                    break;
                case "INTSF":
                    builder.setIntensityChange(SigmetIntensityChange.INTENSIFYING);
                    break;
                case "WKN":
                    builder.setIntensityChange(SigmetIntensityChange.WEAKENING);
                    break;
                }
            }
        });

        System.err.println("<>:"+phenBuilder.getTime());
        phenBuilder.setGeometry(analisysGeometries.get(0)); //TODO list

        phenBuilder.setApproximateLocation(false);

        PhenomenonGeometryWithHeight phenGeom = phenBuilder.build();
        System.err.println("phenTime: "+phenGeom.getTime());
        builder.setAnalysisGeometries(Arrays.asList(phenBuilder.build()));

//        builder.setForecastGeometries(Arrays.asList(fcstBuilder.build()));

        System.err.println("analysistype:"+builder.getAnalysisType());
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
