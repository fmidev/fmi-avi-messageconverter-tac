package fi.fmi.avi.converter.tac.swx;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.swx.AdvisoryNumber;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
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
            builder.setIssuingCenter(issuingCenter.build());
        }, () -> {
            System.out.println("No match");
        });

        firstLexeme.findNext(LexemeIdentity.ADVISORY_NUMBER, (match) -> {
            builder.setAdvisoryNumber(match.getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumber.class));
        }, () -> {
            System.out.println("No match");
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
            System.out.println("No match");
        });

        firstLexeme.findNext(LexemeIdentity.NEXT_ADVISORY, (match) -> {
            NextAdvisoryImpl.Builder nxt = NextAdvisoryImpl.builder();
            nxt.setTimeSpecifier(match.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));
            createCompleteTimeInstant(match, nxt::setTime);
            builder.setNextAdvisory(nxt.build());
        }, () -> {
            System.out.println("No match");
        });

        Lexeme phenomenon = firstLexeme;
        List<SpaceWeatherAdvisoryAnalysis> analyses = new ArrayList<>();

        while ((phenomenon = phenomenon.findNext(LexemeIdentity.ADVISORY_PHENOMENA_LABEL)) != null) {
            analyses.add(processPhenomenon(phenomenon, hints));
        }

        builder.addAllAnalyses(analyses);

        parseRemark(firstLexeme.findNext(LexemeIdentity.REMARKS_START), builder::setRemarks);

        retval.addIssue(conversionIssues);
        retval.setConvertedMessage(builder.build());

        retval.setStatus(ConversionResult.Status.SUCCESS);
        return retval;
    }

    protected List<ConversionIssue> setSWXIssueTime(final SpaceWeatherAdvisoryImpl.Builder builder, final LexemeSequence lexed, final ConversionHints hints) {
        LexemeIdentity[] before = {LexemeIdentity.ADVISORY_PHENOMENA_LABEL};

        return new ArrayList<>(withFoundIssueTime(lexed, before, hints, builder::setIssueTime));
    }

    protected SpaceWeatherAdvisoryAnalysis processPhenomenon(final Lexeme lexeme, final ConversionHints hints) {
        SpaceWeatherAdvisoryAnalysisImpl.Builder builder = SpaceWeatherAdvisoryAnalysisImpl.builder();

        StringBuilder analysisString = new StringBuilder();
        appendToken(analysisString, lexeme);

        if (lexeme.getTACToken().startsWith("OBS")) {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        } else {
            builder.setAnalysisType(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        }

        Lexeme currentLexeme = lexeme.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
        createPartialTimeInstant(currentLexeme, builder::setTime);
        appendToken(analysisString, currentLexeme);

        currentLexeme = currentLexeme.getNext();
        if (currentLexeme.getIdentity().equals(LexemeIdentity.NO_SWX_EXPECTED)) {
            //builder.setTac(analysisString.toString());
            appendToken(analysisString, currentLexeme);
            builder.setNoPhenomenaExpected(true);
            return builder.build();
        }

        List<SpaceWeatherRegion> regionList = handleRegion(analysisString, currentLexeme);

        builder.setRegion(regionList);

        return builder.build();
    }

    protected List<SpaceWeatherRegion> handleRegion(final StringBuilder analysisString, final Lexeme lexeme) {
        //TODO: Translate tac line to region object
        Lexeme regionLex = lexeme;
        SpaceWeatherRegionImpl.Builder regionBuilder1 = SpaceWeatherRegionImpl.builder();
        SpaceWeatherRegionImpl.Builder regionBuilder2 = SpaceWeatherRegionImpl.builder();

        if (regionLex.getIdentity().equals(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION)) {
            appendToken(analysisString, regionLex);
            regionBuilder1.setLocationIndicator(regionLex.getParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.class));
        }

        regionLex = regionLex.getNext();

        if (regionLex.getIdentity().equals(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION)) {
            appendToken(analysisString, regionLex);
            regionBuilder2.setLocationIndicator(regionLex.getParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.class));
        }

        regionLex = regionLex.getNext();

        if (regionLex.getIdentity().equals(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT)) {
            appendToken(analysisString, regionLex);
        }

        regionLex = regionLex.getNext();
        if (regionLex.getIdentity().equals(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT)) {
            appendToken(analysisString, regionLex);
        }

        List<SpaceWeatherRegion> regionList = new ArrayList<>();
        regionList.add(regionBuilder1.build());
        regionList.add(regionBuilder2.build());

        return regionList;
    }

    private void parseRemark(final Lexeme lexeme, final Consumer<List<String>> consumer) {
        Lexeme current = lexeme;
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
