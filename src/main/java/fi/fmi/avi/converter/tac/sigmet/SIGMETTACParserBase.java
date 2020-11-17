package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.token.SigmetValidTime;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.immutable.AirspaceImpl;
import fi.fmi.avi.model.immutable.UnitPropertyGroupImpl;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

public abstract class SIGMETTACParserBase<T extends SIGMET> extends AbstractTACParser<T> {

    protected static final LexemeIdentity[] zeroOrOneAllowed = {/*LexemeIdentity.SIGMET_START,*/ LexemeIdentity.AIRSPACE_DESIGNATOR, LexemeIdentity.SEQUENCE_DESCRIPTOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.VALID_TIME,
            LexemeIdentity.CORRECTION, LexemeIdentity.AMENDMENT, LexemeIdentity.CANCELLATION, LexemeIdentity.NIL, LexemeIdentity.MIN_TEMPERATURE,
            LexemeIdentity.MAX_TEMPERATURE, LexemeIdentity.REMARKS_START };
    protected AviMessageLexer lexer;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
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

        builder.setIssuingAirTrafficServicesUnit(getFicInfo("AMSTERDAM FIR", "EHAA"));
        builder.setMeteorologicalWatchOffice(getMWOInfo("EHDB", "EHDB"));

        lexed.getFirstLexeme().findNext(LexemeIdentity.VALID_TIME, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.MWO_DESIGNATOR};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                PartialOrCompleteTimePeriod.Builder validPeriod = new PartialOrCompleteTimePeriod.Builder();
                int dd1=match.getParsedValue(DAY1, Integer.class);
                int hh1=match.getParsedValue(HOUR1, Integer.class);
                int mm1=match.getParsedValue(MINUTE1, Integer.class);
                validPeriod.setStartTime(PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.ofDayHourMinute(dd1, hh1, mm1)).build());
                int dd2=match.getParsedValue(DAY1, Integer.class);
                int hh2=match.getParsedValue(HOUR2, Integer.class);
                int mm2=match.getParsedValue(MINUTE2, Integer.class);
                validPeriod.setEndTime(PartialOrCompleteTimeInstant.builder().setPartialTime(PartialDateTime.ofDayHourMinute(dd2, hh2, mm2)).build());
                builder.setValidityPeriod(validPeriod.build());
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET sequence descriptor not given in " + input)));

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
        System.err.println("L:"+ lexed.getFirstLexeme());
        lexed.getFirstLexeme().findNext(LexemeIdentity.SEQUENCE_DESCRIPTOR, (match) -> {
            final LexemeIdentity[] before = new LexemeIdentity[] { LexemeIdentity.VALID_TIME};
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                builder.setSequenceNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class));
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "SIGMET sequence descriptor not given in " + input)));


        builder.setStatus(AviationCodeListUser.SigmetAirmetReportStatus.NORMAL);
        builder.setSigmetPhenomenon(AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.EMBD_TS);
        builder.setAnalysisType(SigmetAnalysisType.FORECAST);
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
