package fi.fmi.avi.converter.tac.conf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.LexemeCombiningRules;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.OccurrenceFrequency;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.*;
import fi.fmi.avi.converter.tac.lexer.impl.util.DashVariant;
import fi.fmi.avi.model.MessageType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static fi.fmi.avi.converter.tac.lexer.impl.token.LowWindStart.LOW_WIND_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXREPStart.WXREP_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXWarningStart.WX_WARNING_START;

/**
 * TAC converter Lexing Spring configuration
 */
@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", //
        justification = "Code is cleaner this way. Lambdas are not suitable because older Spring versions are unable to handle those.")
@Configuration
public class Lexing {

    private static final MessageType LOW_WIND = new MessageType("LOW_WIND");
    private static final MessageType WXREP = new MessageType("WXREP");
    private static final MessageType WX_WARNING = new MessageType("WX_WARNING");

    @Bean
    @Primary
    public AviMessageLexer aviMessageLexer() {
        final AviMessageLexerImpl l = new AviMessageLexerImpl();
        l.setLexingFactory(lexingFactory());
        l.addTokenLexer(metarTokenLexer());
        l.addTokenLexer(speciTokenLexer());
        l.addTokenLexer(tafTokenLexer());
        l.addTokenLexer(genericMeteorologicalBulletinTokenLexer());
        l.addTokenLexer(lowWindTokenLexer());
        l.addTokenLexer(wxWarningTokenLexer());
        l.addTokenLexer(wxRepTokenLexer());
        l.addTokenLexer(intlSigmetTokenLexer());
        l.addTokenLexer(usSigmetTokenLexer());
        l.addTokenLexer(intlAirmetTokenLexer());
        l.addTokenLexer(spaceWeatherAdvisoryTokenLexer());
        l.addTokenLexer(volcanicAshAdvisoryTokenLexer());
        l.addTokenLexer(genericAviationWeatherMessageTokenLexer()); //Keep this last, matches anything
        return l;
    }

    @Bean
    public LexingFactory lexingFactory() {
        final LexingFactoryImpl f = new LexingFactoryImpl();
        f.addTokenCombiningRule(fractionalHorizontalVisibilityCombinationRule());
        f.addTokenCombiningRule(windShearAllCombinationRule());
        f.addTokenCombiningRule(windShearCombinationRule());
        f.addTokenCombiningRule(probTempoCombinationRule());
        f.addTokenCombiningRule(lowWindCombinationRule());
        f.addTokenCombiningRule(wxWarningCombinationRule());
        f.addTokenCombiningRule(sigmetValidTimeCombinationRule());
        f.addTokenCombiningRule(usSigmetValidTimeCombinationRule());
        f.addTokenCombiningRule(advisoryStartCombinationRule());
        f.addTokenCombiningRule(advisoryFctOffsetCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryPhenomenaCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryForecastTimeCombinationRule());
        f.addTokenCombiningRule(volcanicAshAdvisoryDtgCombinationRule());
        f.addTokenCombiningRule(volcanicAshAdvisoryCloudForecastCombinationRule());
        f.addTokenCombiningRule(volcanicAshAdvisoryForecastTimeCombinationRule());
        f.addTokenCombiningRule(advisoryNumberCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryNotAvailableCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryNoExpectedCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryHorizontalLimitCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryVerticalLimitCombinationRule());
        f.addTokenCombiningRule(intlSigmetRdoactiveCldCombinationRule());
        f.addTokenCombiningRule(latitudeLongitudePairCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryEffectLabelCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryEffectAndIntensity());
        f.addTokenCombiningRule(spaceWeatherAdvisoryEffectHFCom());
        f.addTokenCombiningRule(spaceWeatherAdvisoryDaylightSide());
        f.addTokenCombiningRule(spaceWeatherAdvisoryPhenomenon());
        f.addTokenCombiningRule(spaceWeatherAdvisoryNextAdvisoryCombinationRules());
        f.addTokenCombiningRule(spaceWeatherAdvisoryIssuedByCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryNoAdvisoriesCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryReplaceAdvisoryCombinationRules());
        f.addTokenCombiningRule(spaceWeatherAdvisoryReplaceAdvisoryWithSpaceCombinationRules());
        f.addTokenCombiningRule(intlSigmetStartRule());
        f.addTokenCombiningRule(intlSigmetPhenomenonFZRACombinationRule());
        f.addTokenCombiningRule(intlSigmetEntireFirCombinationRule());

        f.addTokenCombiningRule(intlSigmetPhenomenonCombinationRule1());
        f.addTokenCombiningRule(intlSigmetPhenomenonCombinationRule2());
        f.addTokenCombiningRule(intlSigmetPhenomenonCombinationRule3());
        f.addTokenCombiningRule(intlSigmetPhenomenonCombinationRule4());
        f.addTokenCombiningRule(intlSigmetPhenomenonCombinationRule5());
        f.addTokenCombiningRule(intlSigmetPhenomenonCombinationRule6());
        f.addTokenCombiningRule(intlSigmetVolcanoName1());
        f.addTokenCombiningRule(intlSigmetVolcanoPosition());

        f.addTokenCombiningRule(intlSigmetLineCombinationRule());
        f.addTokenCombiningRule(intlSigmetLineCombinationRule2());
        f.addTokenCombiningRule(intlSigmetLineCombinationRule3());
        f.addTokenCombiningRule(intlSigmetLineCombinationRule4());
        f.addTokenCombiningRule(intlSigmet2LineCombinationRule());
        f.addTokenCombiningRule(intlSigmetOutsideLatLonCombinationRule());
        f.addTokenCombiningRule(intlSigmetOutsideLatLonCombinationRuleWithAnd());
        f.addTokenCombiningRule(intlSigmetAprxCombinationRule());
        f.addTokenCombiningRule(intlSigmetAprxCombinationRule2());
        f.addTokenCombiningRule(intlSigmetAprxCombinationRule3());
        f.addTokenCombiningRule(intlSigmetAprxCombinationRule4());
        f.addTokenCombiningRule(intlSigmetLevelCombinationRule1());
        f.addTokenCombiningRule(intlSigmetLevelCombinationRule2());
        f.addTokenCombiningRule(intlSigmetLevelCombinationRule3());
        f.addTokenCombiningRule(intlSigmetMovingCombinationRule());
        f.addTokenCombiningRule(intlSigmetObsFcstAtCombinationRule());
        f.addTokenCombiningRule(intlSigmetCancelCombinationRule());
        f.addTokenCombiningRule(intlSigmetVaCancelCombinationRule());
        f.addTokenCombiningRule(intlSigmetNoVaExpCombinationRule());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule1());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule2());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule3());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule4());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule5());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule6());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule7());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule8());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule9());
        f.addTokenCombiningRule(intlAirmetPhenomenonCombinationRule10());


        f.addTokenCombiningRule(intlAirmetStartRule());
//        f.addTokenCombiningRule(intlAirmetCancelCombinationRule());

        //        f.addTokenCombiningRule(spaceWeatherAdvisoryPolygonCombinationRule());

        f.setMessageStartToken(MessageType.METAR, f.createLexeme("METAR", LexemeIdentity.METAR_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SPECI, f.createLexeme("SPECI", LexemeIdentity.SPECI_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.TAF, f.createLexeme("TAF", LexemeIdentity.TAF_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SPECIAL_AIR_REPORT, f.createLexeme("ARS", LexemeIdentity.ARS_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.VOLCANIC_ASH_ADVISORY,
                f.createLexeme("VA ADVISORY", LexemeIdentity.VOLCANIC_ASH_ADVISORY_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SPACE_WEATHER_ADVISORY,
                f.createLexeme("SWX ADVISORY", LexemeIdentity.SPACE_WEATHER_ADVISORY_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SIGMET, f.createLexeme("SIGMET", LexemeIdentity.SIGMET_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.AIRMET, f.createLexeme("AIRMET", LexemeIdentity.AIRMET_START, Lexeme.Status.OK, true));

        //Non-standard types:
        f.setMessageStartToken(LOW_WIND, f.createLexeme("LOW WIND", LOW_WIND_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(WXREP, f.createLexeme("WXREP", WXREP_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(WX_WARNING, f.createLexeme("WX", WX_WARNING_START, Lexeme.Status.OK, true));
        return f;
    }

    private List<Predicate<String>> fractionalHorizontalVisibilityCombinationRule() {
        // cases like "1 1/8SM",
        return LexemeCombiningRules.regexRule(
                "^[0-9]*$",
                "^[0-9]*/[0-9]*SM$");
    }

    private List<Predicate<String>> windShearAllCombinationRule() {
        return LexemeCombiningRules.equalityRule("WS", "ALL", "RWY");
    }

    private List<Predicate<String>> windShearCombinationRule() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.equalityMatcher("WS"),
                // Windshear token for a particular runway has changed between 16th and 19th edition of Annex 3
                //  16th = "WS RWYnn[LRC]"
                //  19th = "WS Rnn[LRC]"
                LexemeCombiningRules.regexMatcher("^R(?:WY)?[0-9]{2}[LRC]?$"));
    }

    private List<Predicate<String>> probTempoCombinationRule() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.regexMatcher("^PROB[34]0$"),
                LexemeCombiningRules.equalityMatcher("TEMPO"));
    }

    private List<Predicate<String>> lowWindCombinationRule() {
        return LexemeCombiningRules.equalityRule("LOW", "WIND");
    }

    private List<Predicate<String>> wxWarningCombinationRule() {
        return LexemeCombiningRules.equalityRule("WX", "WRNG");
    }

    private List<Predicate<String>> sigmetValidTimeCombinationRule() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.equalityMatcher("VALID"),
                LexemeCombiningRules.regexMatcher("^[0-9]{6}[/-][0-9]{6}$"));
    }

    private List<Predicate<String>> usSigmetValidTimeCombinationRule() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.equalityMatcher("VALID"),
                LexemeCombiningRules.equalityMatcher("UNTIL"),
                LexemeCombiningRules.regexMatcher("^[0-9]{2}[0-9]{2}Z$"));
    }

    private List<Predicate<String>> advisoryStartCombinationRule() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.regexMatcher("^SWX|VA$"),
                LexemeCombiningRules.equalityMatcher("ADVISORY"));
    }

    private List<Predicate<String>> advisoryFctOffsetCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^\\+[0-9]{1,2}$",
                "HR:$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryPhenomenaCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^(?:OBS|FCST)$",
                "^SWX:?$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectLabelCombinationRule() {
        return LexemeCombiningRules.equalityRule("SWX", "EFFECT:");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryHorizontalLimitCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^([WE])\\d{1,5}$",
                "^[" + Pattern.quote(DashVariant.ALL_AS_STRING) + "]$",
                "^([WE])\\d{1,5}$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryVerticalLimitCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^ABV$",
                "^FL\\d{3}$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectHFCom() {
        return LexemeCombiningRules.equalityRule("HF", "COM");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectAndIntensity() {
        return LexemeCombiningRules.regexRule(
                "^HF\\s+COM|SATCOM|GNSS|RADIATION$",
                "^MOD|SEV$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryDaylightSide() {
        return LexemeCombiningRules.equalityRule("DAYLIGHT", "SIDE");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryPhenomenon() {
        return LexemeCombiningRules.regexRule(
                "^FCST$",
                "^SWX:$",
                "^\\+\\d{1,2}$",
                "^HR:$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryForecastTimeCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^FCST\\s+SWX",
                "^\\+[0-9]{1,2}\\s+HR:$");
    }

    private List<Predicate<String>> advisoryNumberCombinationRule() {
        return LexemeCombiningRules.equalityRule("ADVISORY", "NR:");
    }

    private List<Predicate<String>> latitudeLongitudePairCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^[NS]\\d+$",
                "^[WE]\\d+$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryIssuedByCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^WILL$",
                "^BE$",
                "^ISSUED$",
                "^BY$",
                //TODO:
                "^[0-9]{4}[0-9]{2}[0-9]{2}/[0-9]{2}[0-9]{2}Z$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNoAdvisoriesCombinationRule() {
        return LexemeCombiningRules.equalityRule("NO", "FURTHER", "ADVISORIES");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNextAdvisoryCombinationRules() {
        return LexemeCombiningRules.equalityRule("NXT", "ADVISORY:");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryReplaceAdvisoryWithSpaceCombinationRules() {
        return LexemeCombiningRules.equalityRule("NR", "RPLC", ":");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryReplaceAdvisoryCombinationRules() {
        return LexemeCombiningRules.equalityRule("NR", "RPLC:");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNoExpectedCombinationRule() {
        return LexemeCombiningRules.equalityRule("NO", "SWX", "EXP");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNotAvailableCombinationRule() {
        return LexemeCombiningRules.equalityRule("NOT", "AVBL");
    }

    private List<Predicate<String>> volcanicAshAdvisoryDtgCombinationRule() {
        return LexemeCombiningRules.equalityRule("OBS", "VA", "DTG:");
    }

    private List<Predicate<String>> volcanicAshAdvisoryCloudForecastCombinationRule() {
        return LexemeCombiningRules.equalityRule("FCST", "VA", "CLD");
    }

    private List<Predicate<String>> volcanicAshAdvisoryForecastTimeCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^FCST\\s+VA\\s+CLD",
                "^\\+[0-9]{1,2}\\s+HR:$");
    }

    private List<Predicate<String>> intlSigmetEntireFirCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^ENTIRE$",
                "^(FIR|UIR|FIR/UIR|CTA)$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^N|NE|E|SE|S|SW|W|NW$",
                "^OF$",
                "^LINE$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule2() {
        return LexemeCombiningRules.regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule3() {
        return LexemeCombiningRules.regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule4() {
        return LexemeCombiningRules.regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmet2LineCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})(\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})){0,2}$",
                "^AND$",
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})(\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})){0,2}$");
    }

    private List<Predicate<String>> intlSigmetOutsideLatLonCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^N|S|E|W$",
                "^OF$",
                "^([NSEW]\\d+)");
    }

    private List<Predicate<String>> intlSigmetOutsideLatLonCombinationRuleWithAnd() {
        return LexemeCombiningRules.regexRule(
                "^([NSEW])\\sOF\\s([NSEW]\\d+)$",
                "^AND$",
                "^([NSEW])\\sOF\\s([NSEW]\\d+)$");

    }

    private List<Predicate<String>> intlSigmetStartRule() {
        return LexemeCombiningRules.regexRule(
                "^[A-Z]{4}",
                "^(SIGMET)$");
    }

    private List<Predicate<String>> intlAirmetStartRule() {
        return LexemeCombiningRules.regexRule(
                "^[A-Z]{4}",
                "^(AIRMET)$");
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule1() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.equalityMatcher("TOP"),
                LexemeCombiningRules.regexMatcher("^(ABV|BLW)$"));
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule2() {
        return LexemeCombiningRules.regexRule(
                "^(TOP\\s+ABV|ABV)$",
                "^(FL[0-9]{3}|[0-9]{4,5}FT)$");
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule3() {
        return LexemeCombiningRules.regexRule(
                "^(TOP\\s+ABV|TOP\\s+BLW|TOP)$",
                "^(FL[0-9]{3})$");
    }

    private List<Predicate<String>> intlSigmetMovingCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "MOV",
                "^(N|NNE|NE|ENE|E|ESE|SE|SSE|S|SSW|SW|WSW|W|WNW|NW|NNW)$",
                "^([0-9]{1,3})(KT|KMH)$");
    }

    private List<Predicate<String>> intlSigmetObsFcstAtCombinationRule() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.regexMatcher("^(OBS|FCST)$"),
                LexemeCombiningRules.equalityMatcher("AT"),
                LexemeCombiningRules.regexMatcher("^[0-9]{4}Z$"));
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^APRX$",
                "^(\\d{2}(KM|NM))$",
                "^WID$",
                "^LINE$",
                "^BTN$");
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule2() {
        return LexemeCombiningRules.regexRule(
                "^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule3() {
        return LexemeCombiningRules.regexRule(
                "^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule4() {
        return LexemeCombiningRules.regexRule(
                "^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetCancelCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^CNL$",
                "^(SIGMET|AIRMET)$",
                "^\\w?\\d?\\d$",
                "^(\\d{2}\\d{2}\\d{2}/\\d{2}\\d{2}\\d{2})$");
    }

    private List<Predicate<String>> intlSigmetVaCancelCombinationRule() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.regexMatcher("^CNL SIGMET\\s+(\\w?\\d?\\d)\\s+(\\d{2}\\d{2}\\d{2}/\\d{2}\\d{2}\\d{2})$"),
                LexemeCombiningRules.equalityMatcher("VA"),
                LexemeCombiningRules.equalityMatcher("MOV"),
                LexemeCombiningRules.equalityMatcher("TO"),
                LexemeCombiningRules.regexMatcher("^\\w*$"),
                LexemeCombiningRules.equalityMatcher("FIR"));
    }

    private List<Predicate<String>> intlSigmetNoVaExpCombinationRule() {
        return LexemeCombiningRules.equalityRule("NO", "VA", "EXP");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule1() {
        return LexemeCombiningRules.regexRule(
                "^(ISOL|OCNL)$",
                "^(TS|TSGR)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule2() {
        return LexemeCombiningRules.equalityRule("MT", "OBSC");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule3() {
        return LexemeCombiningRules.regexRule(
                "^(ISOL|OCNL|FRQ)$",
                "^(CB|TCU)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule4() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.equalityMatcher("MOD"),
                LexemeCombiningRules.regexMatcher("^(TURB|ICE|MTW)$"));
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule5() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.regexMatcher("^(BKN|OVC)$"),
                LexemeCombiningRules.equalityMatcher("CLD"));
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule6() {
        return LexemeCombiningRules.regexRule(
                "^(SFC)$",
                "^(WIND|VIS)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule7() {
        return LexemeCombiningRules.regexRule(
                "^SFC\\s+VIS$",
                "^(\\d{2,4}M)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule8() {
        return LexemeCombiningRules.regexRule(
                "^(SFC VIS\\s\\d{2,4}M)$",
                "^(\\((BR|DS|DU|DZ|FC|FG|FU|GR|GS|HZ|PL|PO|RA|SA|SG|SN|SQ|SS|VA)\\))$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule9() {
        return LexemeCombiningRules.regexRule(
                "^SFC\\s+WIND$",
                "^(\\d{3}/\\d{2,3})(KT|MPS)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule10() {
        return LexemeCombiningRules.regexRule(
                "^(BKN|OVC)\\sCLD$",
                "^((\\d{3,4})|SFC)/(ABV)?((\\d{3,4}M)|(\\d{4,5}FT))$");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule1() {
        return LexemeCombiningRules.rule(
                LexemeCombiningRules.equalityMatcher("SEV"),
                LexemeCombiningRules.regexMatcher("^(TURB|ICE|MTW)$"));
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule2() {
        return LexemeCombiningRules.regexRule(
                "^(OBSC|EMBD|FRQ|SQL)$",
                "^(TS|TSGR)$");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule3() {
        return LexemeCombiningRules.regexRule(
                "^(HVY)$",
                "^(DS|SS)$");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule4() {
        return LexemeCombiningRules.equalityRule("RDOACT", "CLD");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule5() {
        return LexemeCombiningRules.equalityRule("VA", "CLD");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule6() {
        return LexemeCombiningRules.equalityRule("VA", "ERUPTION");
    }

    private List<Predicate<String>> intlSigmetRdoactiveCldCombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^(WI)$",
                "^(\\d{2})(KM|NM)$",
                "^OF$",
                "^([NS])(\\d{2,4})$",
                "^([WE])(\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetVolcanoName1() {
        return LexemeCombiningRules.regexRule(
                "^(MT)$",
                "^(\\w*)$");
    }

    private List<Predicate<String>> intlSigmetVolcanoPosition() {
        return LexemeCombiningRules.regexRule(
                "^(PSN)$",
                "^([NS])(\\d{2,4}) ([EW])(\\d{3,5})");
    }

    private List<Predicate<String>> intlSigmetPhenomenonFZRACombinationRule() {
        return LexemeCombiningRules.regexRule(
                "^(SEV\\s+ICE)$",
                "^(\\(FZRA\\))$");
    }

    private RecognizingAviMessageTokenLexer metarTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeEquals("METAR", MessageType.METAR));

        l.teach(new MetarStart(OccurrenceFrequency.FREQUENT));
        teachMetarAndSpeciCommonTokens(l);
        return l;
    }

    private RecognizingAviMessageTokenLexer speciTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeEquals("SPECI", MessageType.SPECI));

        l.teach(new SpeciStart(OccurrenceFrequency.FREQUENT));
        teachMetarAndSpeciCommonTokens(l);
        return l;
    }

    private void teachMetarAndSpeciCommonTokens(final RecognizingAviMessageTokenLexer l) {
        l.teach(new ICAOCode(OccurrenceFrequency.RARE));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new CloudLayer(OccurrenceFrequency.FREQUENT));
        l.teach(new Weather(OccurrenceFrequency.AVERAGE));
        l.teach(new SurfaceWind(OccurrenceFrequency.RARE));
        l.teach(new VariableSurfaceWind(OccurrenceFrequency.RARE));
        l.teach(new MetricHorizontalVisibility(OccurrenceFrequency.AVERAGE));
        l.teach(new FractionalHorizontalVisibility(OccurrenceFrequency.AVERAGE));
        l.teach(new TrendChangeIndicator(OccurrenceFrequency.RARE));
        l.teach(new NoSignificantChanges(OccurrenceFrequency.RARE));
        l.teach(new TrendTimeGroup(OccurrenceFrequency.RARE));
        l.teach(new ColorCode(OccurrenceFrequency.RARE));
        l.teach(new CAVOK(OccurrenceFrequency.RARE));
        l.teach(new Correction(OccurrenceFrequency.RARE));
        l.teach(new RunwayVisualRange(OccurrenceFrequency.FREQUENT));
        l.teach(new AirDewpointTemperature(OccurrenceFrequency.RARE));
        l.teach(new AtmosphericPressureQNH(OccurrenceFrequency.RARE));
        l.teach(new RunwayState(OccurrenceFrequency.RARE));
        l.teach(new SnowClosure(OccurrenceFrequency.RARE));
        l.teach(new AutoMetar(OccurrenceFrequency.RARE));
        l.teach(new NoSignificantWeather(OccurrenceFrequency.RARE));
        l.teach(new RemarkStart(OccurrenceFrequency.FREQUENT));
        l.teach(new Remark(OccurrenceFrequency.FREQUENT));
        l.teach(new WindShear(OccurrenceFrequency.RARE));
        l.teach(new SeaState(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        l.teach(new Nil(OccurrenceFrequency.FREQUENT));
        l.teach(new RoutineDelayedObservation(OccurrenceFrequency.FREQUENT));
    }

    private RecognizingAviMessageTokenLexer tafTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeEquals("TAF", MessageType.TAF));

        l.teach(new TAFStart(OccurrenceFrequency.FREQUENT));
        l.teach(new ICAOCode(OccurrenceFrequency.RARE));
        l.teach(new ValidTime(OccurrenceFrequency.RARE));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new CloudLayer(OccurrenceFrequency.FREQUENT));
        l.teach(new Weather(OccurrenceFrequency.AVERAGE));
        l.teach(new SurfaceWind(OccurrenceFrequency.RARE));
        l.teach(new VariableSurfaceWind(OccurrenceFrequency.RARE));
        l.teach(new MetricHorizontalVisibility(OccurrenceFrequency.AVERAGE));
        l.teach(new FractionalHorizontalVisibility(OccurrenceFrequency.AVERAGE));
        l.teach(new TAFForecastChangeIndicator(OccurrenceFrequency.RARE));
        l.teach(new TAFChangeForecastTimeGroup(OccurrenceFrequency.RARE));
        l.teach(new Correction(OccurrenceFrequency.RARE));
        l.teach(new Amendment(OccurrenceFrequency.RARE));
        l.teach(new Nil(OccurrenceFrequency.FREQUENT));
        l.teach(new Cancellation(OccurrenceFrequency.RARE));
        l.teach(new CAVOK(OccurrenceFrequency.RARE));
        l.teach(new NoSignificantWeather(OccurrenceFrequency.RARE));
        l.teach(new ForecastMaxMinTemperature(OccurrenceFrequency.RARE));
        l.teach(new RemarkStart(OccurrenceFrequency.FREQUENT));
        l.teach(new Remark(OccurrenceFrequency.FREQUENT));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer genericMeteorologicalBulletinTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Just check the first Lexeme for now, add checks for further Lexemes if
        // collisions arise with other token lexers:
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("^[A-Z]{4}[0-9]{2}$", MessageType.BULLETIN));

        l.teach(new BulletinHeaderDataDesignators(OccurrenceFrequency.AVERAGE));
        l.teach(new BulletinLocationIndicator(OccurrenceFrequency.AVERAGE));
        l.teach(new IssueTime(OccurrenceFrequency.FREQUENT));
        l.teach(new BulletinHeadingBBBIndicator(OccurrenceFrequency.AVERAGE));
        l.teach(new EndToken(OccurrenceFrequency.FREQUENT));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer genericAviationWeatherMessageTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.alwaysSuits(MessageType.GENERIC));

        l.teach(new EndToken(OccurrenceFrequency.FREQUENT));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer lowWindTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("^LOW\\s+WIND$", LOW_WIND));
        l.teach(new LowWindStart(OccurrenceFrequency.FREQUENT));
        l.teach(new ICAOCode(OccurrenceFrequency.RARE));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer wxWarningTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("^WX\\s+WRNG$", WX_WARNING));
        l.teach(new WXWarningStart(OccurrenceFrequency.FREQUENT));
        l.teach(new ICAOCode(OccurrenceFrequency.RARE));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer wxRepTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeEquals("WXREP", WXREP));
        l.teach(new WXREPStart(OccurrenceFrequency.FREQUENT));
        l.teach(new REP(OccurrenceFrequency.FREQUENT));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer intlSigmetTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("(\\w{4})\\s+SIGMET.*|SIGMET", MessageType.SIGMET));

        l.teach(new SigmetStart(OccurrenceFrequency.FREQUENT));
        l.teach(new SigmetSequenceDescriptor(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetValidTime(OccurrenceFrequency.AVERAGE));
        l.teach(new MWODesignator(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        l.teach(new AirmetSigmetObsOrForecast(OccurrenceFrequency.FREQUENT));
        l.teach(new FIRDesignator(OccurrenceFrequency.AVERAGE));
        l.teach(new FIRName(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetUsage(OccurrenceFrequency.RARE));
        l.teach(new SigmetPhenomenon(OccurrenceFrequency.AVERAGE));
        l.teach(new PolygonCoordinatePair(OccurrenceFrequency.FREQUENT));
        l.teach(new PolygonCoordinatePairSeparator(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetEntireFir(OccurrenceFrequency.RARE));
        l.teach(new SigmetWithin(OccurrenceFrequency.RARE));
        l.teach(new SigmetLine(OccurrenceFrequency.RARE));
        l.teach(new Sigmet2Lines(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetOutsideLatOrLon(OccurrenceFrequency.RARE));
        l.teach(new SigmetBetweenLatOrLon(OccurrenceFrequency.RARE));
        l.teach(new SigmetAnd(OccurrenceFrequency.RARE));
        l.teach(new SigmetVaEruption(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetVaName(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetVaPosition(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetVaName(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetFirNameWord(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetWithinRadius(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetAprx(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetLevel(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetMoving(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetIntensity(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetForecastAt(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetCancel(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetNoVaExp(OccurrenceFrequency.AVERAGE));
        return l;
    }

    private RecognizingAviMessageTokenLexer usSigmetTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("^SIG[CWE]$", MessageType.SIGMET));

        l.teach(new USSigmetStart(OccurrenceFrequency.FREQUENT));
        l.teach(new USSigmetValidUntil(OccurrenceFrequency.AVERAGE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer intlAirmetTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("(\\w{4})\\s+AIRMET.*|AIRMET", MessageType.AIRMET));

        l.teach(new AirmetStart(OccurrenceFrequency.RARE));
        l.teach(new SigmetSequenceDescriptor(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetValidTime(OccurrenceFrequency.AVERAGE));
        l.teach(new MWODesignator(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        l.teach(new AirmetSigmetObsOrForecast(OccurrenceFrequency.FREQUENT));
        l.teach(new FIRDesignator(OccurrenceFrequency.AVERAGE));
        l.teach(new FIRName(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetUsage(OccurrenceFrequency.RARE));
        l.teach(new AirmetPhenomenon(OccurrenceFrequency.AVERAGE));
        l.teach(new PolygonCoordinatePair(OccurrenceFrequency.FREQUENT));
        l.teach(new PolygonCoordinatePairSeparator(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetEntireFir(OccurrenceFrequency.RARE));
        l.teach(new SigmetWithin(OccurrenceFrequency.RARE));
        l.teach(new SigmetLine(OccurrenceFrequency.RARE));
        l.teach(new Sigmet2Lines(OccurrenceFrequency.AVERAGE));
        l.teach(new Latitude(OccurrenceFrequency.RARE));
        l.teach(new Longitude(OccurrenceFrequency.RARE));
        l.teach(new SigmetOutsideLatOrLon(OccurrenceFrequency.RARE));
        l.teach(new SigmetBetweenLatOrLon(OccurrenceFrequency.RARE));
        l.teach(new SigmetAnd(OccurrenceFrequency.RARE));
        l.teach(new SigmetVaEruption(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetVaName(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetVaPosition(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetVaName(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetFirNameWord(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetAprx(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetLevel(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetMoving(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetIntensity(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetForecastAt(OccurrenceFrequency.AVERAGE));
        l.teach(new AirmetCancel(OccurrenceFrequency.AVERAGE));
        return l;
    }

    private RecognizingAviMessageTokenLexer spaceWeatherAdvisoryTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("^SWX\\s+ADVISORY$", MessageType.SPACE_WEATHER_ADVISORY));

        l.teach(new SWXAdvisoryStart(OccurrenceFrequency.RARE));
        l.teach(new DTGIssueTimeLabel(OccurrenceFrequency.AVERAGE));
        l.teach(new DTGIssueTime(OccurrenceFrequency.AVERAGE));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new SWXPhenomena(OccurrenceFrequency.AVERAGE));
        l.teach(new AdvisoryPhenomenaTimeGroup(OccurrenceFrequency.AVERAGE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        l.teach(new AdvisoryStatus(OccurrenceFrequency.AVERAGE));
        l.teach(new AdvisoryStatusLabel(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXCenter(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXCenterLabel(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXEffectLabel(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXEffect(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXEffectAndIntensity(OccurrenceFrequency.AVERAGE));
        l.teach(new AdvisoryNumberLabel(OccurrenceFrequency.RARE));
        l.teach(new AdvisoryNumber(OccurrenceFrequency.RARE));
        l.teach(new SWXEffectConjuction(OccurrenceFrequency.FREQUENT));
        l.teach(new SWXPresetLocation(OccurrenceFrequency.AVERAGE));
        l.teach(new NextAdvisory(OccurrenceFrequency.RARE));
        l.teach(new NextAdvisoryLabel(OccurrenceFrequency.RARE));
        l.teach(new NoFurtherAdvisories(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXIntensity(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXNotAvailable(OccurrenceFrequency.RARE));
        l.teach(new SWXNotExpected(OccurrenceFrequency.RARE));
        l.teach(new SWXPhenonmenonLongitudeLimit(OccurrenceFrequency.AVERAGE));
        l.teach(new PolygonCoordinatePair(OccurrenceFrequency.FREQUENT));
        l.teach(new PolygonCoordinatePairSeparator(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXVerticalLimit(OccurrenceFrequency.AVERAGE));
        l.teach(new ReplaceAdvisoryNumberLabel(OccurrenceFrequency.AVERAGE));
        l.teach(new ReplaceAdvisoryNumber(OccurrenceFrequency.AVERAGE));
        l.teach(new AdvisoryRemarkStart(OccurrenceFrequency.AVERAGE));
        l.teach(new Remark(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer volcanicAshAdvisoryTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(RecognizingAviMessageTokenLexer.SuitabilityTester.firstLexemeMatches("^VA\\s+ADVISORY$", MessageType.VOLCANIC_ASH_ADVISORY));

        l.teach(new VolcanicAshAdvisoryStart(OccurrenceFrequency.RARE));
        l.teach(new DTGIssueTime(OccurrenceFrequency.RARE));
        l.teach(new DTGIssueTimeLabel(OccurrenceFrequency.RARE));
        l.teach(new VolcanicAshPhenomena(OccurrenceFrequency.AVERAGE));
        l.teach(new AdvisoryPhenomenaTimeGroup(OccurrenceFrequency.AVERAGE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }
}
