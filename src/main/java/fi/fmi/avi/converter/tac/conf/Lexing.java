package fi.fmi.avi.converter.tac.conf;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.converter.tac.lexer.*;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.OccurrenceFrequency;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.*;
import fi.fmi.avi.converter.tac.lexer.impl.util.DashVariant;
import fi.fmi.avi.model.MessageType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import static fi.fmi.avi.converter.tac.lexer.impl.token.LowWindStart.LOW_WIND_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXREPStart.WXREP_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXWarningStart.WX_WARNING_START;
import static java.util.Objects.requireNonNull;

/**
 * TAC converter Lexing Spring configuration
 */
@SuppressWarnings({"Convert2Lambda", "Anonymous2MethodRef"})
@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", //
        justification = "Code is cleaner this way. Lambdas are not suitable because older Spring versions are unable to handle those.")
@Configuration
public class Lexing {

    private static final Pattern BULLETIN_START_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{2}$");

    @SafeVarargs
    private static List<Predicate<String>> rule(final Predicate<String>... rules) {
        return Arrays.asList(rules);
    }

    private static List<Predicate<String>> regexRule(final String... patterns) {
        final List<Predicate<String>> rule = new ArrayList<>(patterns.length);
        for (final String pattern : patterns) {
            rule.add(regexMatcher(pattern));
        }
        return rule;
    }

    private static Predicate<String> regexMatcher(final String pattern) {
        requireNonNull(pattern, "pattern");
        final Pattern compiledPattern = Pattern.compile(pattern);
        return new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return compiledPattern.matcher(s).matches();
            }
        };
    }

    private static List<Predicate<String>> equalityRule(final String... allExpected) {
        final List<Predicate<String>> rule = new ArrayList<>(allExpected.length);
        for (final String expected : allExpected) {
            rule.add(equalityMatcher(expected));
        }
        return rule;
    }

    private static Predicate<String> equalityMatcher(final String expected) {
        requireNonNull(expected, "expected");
        return new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return expected.equals(s);
            }
        };
    }

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
        f.setMessageStartToken(lowWind(), f.createLexeme("LOW WIND", LOW_WIND_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(wxRep(), f.createLexeme("WXREP", WXREP_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(wxWarning(), f.createLexeme("WX", WX_WARNING_START, Lexeme.Status.OK, true));
        return f;
    }

    private MessageType wxRep() {
        return new MessageType("WXREP");
    }

    private MessageType wxWarning() {
        return new MessageType("WX_WARNING");
    }

    private MessageType lowWind() {
        return new MessageType("LOW_WIND");
    }

    private List<Predicate<String>> fractionalHorizontalVisibilityCombinationRule() {
        // cases like "1 1/8SM",
        return regexRule(
                "^[0-9]*$",
                "^[0-9]*/[0-9]*SM$");
    }

    private List<Predicate<String>> windShearAllCombinationRule() {
        return equalityRule("WS", "ALL", "RWY");
    }

    private List<Predicate<String>> windShearCombinationRule() {
        return rule(
                equalityMatcher("WS"),
                // Windshear token for a particular runway has changed between 16th and 19th edition of Annex 3
                //  16th = "WS RWYnn[LRC]"
                //  19th = "WS Rnn[LRC]"
                regexMatcher("^R(?:WY)?[0-9]{2}[LRC]?$"));
    }

    private List<Predicate<String>> probTempoCombinationRule() {
        return rule(
                regexMatcher("^PROB[34]0$"),
                equalityMatcher("TEMPO"));
    }

    private List<Predicate<String>> lowWindCombinationRule() {
        return equalityRule("LOW", "WIND");
    }

    private List<Predicate<String>> wxWarningCombinationRule() {
        return equalityRule("WX", "WRNG");
    }

    private List<Predicate<String>> sigmetValidTimeCombinationRule() {
        return rule(
                equalityMatcher("VALID"),
                regexMatcher("^[0-9]{6}[/-][0-9]{6}$"));
    }

    private List<Predicate<String>> usSigmetValidTimeCombinationRule() {
        return rule(
                equalityMatcher("VALID"),
                equalityMatcher("UNTIL"),
                regexMatcher("^[0-9]{2}[0-9]{2}Z$"));
    }

    private List<Predicate<String>> advisoryStartCombinationRule() {
        return rule(
                regexMatcher("^SWX|VA$"),
                equalityMatcher("ADVISORY"));
    }

    private List<Predicate<String>> advisoryFctOffsetCombinationRule() {
        return regexRule(
                "^\\+[0-9]{1,2}$",
                "HR:$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryPhenomenaCombinationRule() {
        return regexRule(
                "^(?:OBS|FCST)$",
                "^SWX:?$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectLabelCombinationRule() {
        return equalityRule("SWX", "EFFECT:");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryHorizontalLimitCombinationRule() {
        return regexRule(
                "^([WE])\\d{1,5}$",
                "^[" + Pattern.quote(DashVariant.ALL_AS_STRING) + "]$",
                "^([WE])\\d{1,5}$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryVerticalLimitCombinationRule() {
        return regexRule(
                "^ABV$",
                "^FL\\d{3}$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectHFCom() {
        return equalityRule("HF", "COM");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectAndIntensity() {
        return regexRule(
                "^HF\\s+COM|SATCOM|GNSS|RADIATION$",
                "^MOD|SEV$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryDaylightSide() {
        return equalityRule("DAYLIGHT", "SIDE");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryPhenomenon() {
        return regexRule(
                "^FCST$",
                "^SWX:$",
                "^\\+\\d{1,2}$",
                "^HR:$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryForecastTimeCombinationRule() {
        return regexRule(
                "^FCST\\s+SWX",
                "^\\+[0-9]{1,2}\\s+HR:$");
    }

    private List<Predicate<String>> advisoryNumberCombinationRule() {
        return equalityRule("ADVISORY", "NR:");
    }

    private List<Predicate<String>> latitudeLongitudePairCombinationRule() {
        return regexRule(
                "^[NS]\\d+$",
                "^[WE]\\d+$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryIssuedByCombinationRule() {
        return regexRule(
                "^WILL$",
                "^BE$",
                "^ISSUED$",
                "^BY$",
                //TODO:
                "^[0-9]{4}[0-9]{2}[0-9]{2}/[0-9]{2}[0-9]{2}Z$");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNoAdvisoriesCombinationRule() {
        return equalityRule("NO", "FURTHER", "ADVISORIES");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNextAdvisoryCombinationRules() {
        return equalityRule("NXT", "ADVISORY:");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryReplaceAdvisoryWithSpaceCombinationRules() {
        return equalityRule("NR", "RPLC", ":");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryReplaceAdvisoryCombinationRules() {
        return equalityRule("NR", "RPLC:");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNoExpectedCombinationRule() {
        return equalityRule("NO", "SWX", "EXP");
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNotAvailableCombinationRule() {
        return equalityRule("NOT", "AVBL");
    }

    private List<Predicate<String>> volcanicAshAdvisoryDtgCombinationRule() {
        return equalityRule("OBS", "VA", "DTG:");
    }

    private List<Predicate<String>> volcanicAshAdvisoryCloudForecastCombinationRule() {
        return equalityRule("FCST", "VA", "CLD");
    }

    private List<Predicate<String>> volcanicAshAdvisoryForecastTimeCombinationRule() {
        return regexRule(
                "^FCST\\s+VA\\s+CLD",
                "^\\+[0-9]{1,2}\\s+HR:$");
    }

    private List<Predicate<String>> intlSigmetEntireFirCombinationRule() {
        return regexRule(
                "^ENTIRE$",
                "^(FIR|UIR|FIR/UIR|CTA)$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule() {
        return regexRule(
                "^N|NE|E|SE|S|SW|W|NW$",
                "^OF$",
                "^LINE$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule2() {
        return regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule3() {
        return regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule4() {
        return regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmet2LineCombinationRule() {
        return regexRule(
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})(\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})){0,2}$",
                "^AND$",
                "^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})(\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})){0,2}$");
    }

    private List<Predicate<String>> intlSigmetOutsideLatLonCombinationRule() {
        return regexRule(
                "^N|S|E|W$",
                "^OF$",
                "^([NSEW]\\d+)");
    }

    private List<Predicate<String>> intlSigmetOutsideLatLonCombinationRuleWithAnd() {
        return regexRule(
                "^([NSEW])\\sOF\\s([NSEW]\\d+)$",
                "^AND$",
                "^([NSEW])\\sOF\\s([NSEW]\\d+)$");

    }

    private List<Predicate<String>> intlSigmetStartRule() {
        return regexRule(
                "^[A-Z]{4}",
                "^(SIGMET)$");
    }

    private List<Predicate<String>> intlAirmetStartRule() {
        return regexRule(
                "^[A-Z]{4}",
                "^(AIRMET)$");
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule1() {
        return rule(
                equalityMatcher("TOP"),
                regexMatcher("^(ABV|BLW)$"));
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule2() {
        return regexRule(
                "^(TOP ABV|ABV)$",
                "^(FL[0-9]{3}|[0-9]{4,5}FT)$");
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule3() {
        return regexRule(
                "^(TOP ABV|TOP BLW|TOP)$",
                "^(FL[0-9]{3})$");
    }

    private List<Predicate<String>> intlSigmetMovingCombinationRule() {
        return regexRule(
                "MOV",
                "^(N|NNE|NE|ENE|E|ESE|SE|SSE|S|SSW|SW|WSW|W|WNW|NW|NNW)$",
                "^([0-9]{1,3})(KT|KMH)$");
    }

    private List<Predicate<String>> intlSigmetObsFcstAtCombinationRule() {
        return rule(
                regexMatcher("^(OBS|FCST)$"),
                equalityMatcher("AT"),
                regexMatcher("^[0-9]{4}Z$"));
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule() {
        return regexRule(
                "^APRX$",
                "^(\\d{2}(KM|NM))$",
                "^WID$",
                "^LINE$",
                "^BTN$");
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule2() {
        return regexRule(
                "^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule3() {
        return regexRule(
                "^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule4() {
        return regexRule(
                "^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$",
                "^-$",
                "^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetCancelCombinationRule() {
        return regexRule(
                "^CNL$",
                "^(SIGMET|AIRMET)$",
                "^\\w?\\d?\\d$",
                "^(\\d{2}\\d{2}\\d{2}/\\d{2}\\d{2}\\d{2})$");
    }

    private List<Predicate<String>> intlSigmetVaCancelCombinationRule() {
        return rule(
                regexMatcher("^CNL SIGMET (\\w?\\d?\\d) (\\d{2}\\d{2}\\d{2}/\\d{2}\\d{2}\\d{2})$"),
                equalityMatcher("VA"),
                equalityMatcher("MOV"),
                equalityMatcher("TO"),
                regexMatcher("^\\w*$"),
                equalityMatcher("FIR"));
    }

    private List<Predicate<String>> intlSigmetNoVaExpCombinationRule() {
        return equalityRule("NO", "VA", "EXP");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule1() {
        return regexRule(
                "^(ISOL|OCNL)$",
                "^(TS|TSGR)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule2() {
        return equalityRule("MT", "OBSC");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule3() {
        return regexRule(
                "^(ISOL|OCNL|FRQ)$",
                "^(CB|TCU)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule4() {
        return rule(
                equalityMatcher("MOD"),
                regexMatcher("^(TURB|ICE|MTW)$"));
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule5() {
        return rule(
                regexMatcher("^(BKN|OVC)$"),
                equalityMatcher("CLD"));
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule6() {
        return regexRule(
                "^(SFC)$",
                "^(WIND|VIS)$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule7() {
        return rule(
                equalityMatcher("SFC VIS"),
                regexMatcher("^(\\d{2,4}M)$"));
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule8() {
        return regexRule(
                "^(SFC VIS\\s\\d{2,4}M)$",
                "^(\\((BR|DS|DU|DZ|FC|FG|FU|GR|GS|HZ|PL|PO|RA|SA|SG|SN|SQ|SS|VA)\\))$");
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule9() {
        return rule(
                equalityMatcher("SFC WIND"),
                regexMatcher("^(\\d{3}/\\d{2,3})(KT|MPS)$"));
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule10() {
        return regexRule(
                "^(BKN|OVC)\\sCLD$",
                "^((\\d{3,4})|SFC)/(ABV)?((\\d{3,4}M)|(\\d{4,5}FT))$");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule1() {
        return rule(
                equalityMatcher("SEV"),
                regexMatcher("^(TURB|ICE|MTW)$"));
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule2() {
        return regexRule(
                "^(OBSC|EMBD|FRQ|SQL)$",
                "^(TS|TSGR)$");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule3() {
        return regexRule(
                "^(HVY)$",
                "^(DS|SS)$");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule4() {
        return equalityRule("RDOACT", "CLD");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule5() {
        return equalityRule("VA", "CLD");
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule6() {
        return equalityRule("VA", "ERUPTION");
    }

    private List<Predicate<String>> intlSigmetRdoactiveCldCombinationRule() {
        return regexRule(
                "^(WI)$",
                "^(\\d{2})(KM|NM)$",
                "^OF$",
                "^([NS])(\\d{2,4})$",
                "^([WE])(\\d{3,5})$");
    }

    private List<Predicate<String>> intlSigmetVolcanoName1() {
        return regexRule(
                "^(MT)$",
                "^(\\w*)$");
    }

    private List<Predicate<String>> intlSigmetVolcanoPosition() {
        return regexRule(
                "^(PSN)$",
                "^([NS])(\\d{2,4}) ([EW])(\\d{3,5})");
    }

    private List<Predicate<String>> intlSigmetPhenomenonFZRACombinationRule() {
        return regexRule(
                "^(SEV ICE)$",
                "^(\\(FZRA\\))$");
    }

    private RecognizingAviMessageTokenLexer metarTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && "METAR".equals(firstLexeme.getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.METAR;
            }
        });
        l.teach(new MetarStart(OccurrenceFrequency.FREQUENT));
        teachMetarAndSpeciCommonTokens(l);
        return l;
    }

    private RecognizingAviMessageTokenLexer speciTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && "SPECI".equals(firstLexeme.getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.SPECI;
            }
        });
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
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && "TAF".equals(firstLexeme.getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.TAF;
            }
        });
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
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                //Just check the first Lexeme for now, add checks for further Lexemes if
                // collisions arise with other token lexers:
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && BULLETIN_START_PATTERN.matcher(firstLexeme.getTACToken()).matches();
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.BULLETIN;
            }

        });
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
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                return true;
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.GENERIC;
            }

        });
        l.teach(new EndToken(OccurrenceFrequency.FREQUENT));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer lowWindTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && "LOW WIND".equals(firstLexeme.getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return lowWind();
            }
        });
        l.teach(new LowWindStart(OccurrenceFrequency.FREQUENT));
        l.teach(new ICAOCode(OccurrenceFrequency.RARE));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer wxWarningTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && "WX WRNG".equals(firstLexeme.getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return wxWarning();
            }
        });
        l.teach(new WXWarningStart(OccurrenceFrequency.FREQUENT));
        l.teach(new ICAOCode(OccurrenceFrequency.RARE));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer wxRepTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && "WXREP".equals(firstLexeme.getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return wxRep();
            }
        });
        l.teach(new WXREPStart(OccurrenceFrequency.FREQUENT));
        l.teach(new REP(OccurrenceFrequency.FREQUENT));
        l.teach(new IssueTime(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer intlSigmetTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return (firstLexeme != null) && ((firstLexeme.getTACToken().matches("(\\w{4})\\sSIGMET.*") || firstLexeme.getTACToken().equals("SIGMET")));
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.SIGMET;
            }
        });
        l.teach(new SigmetStart(OccurrenceFrequency.FREQUENT));
        l.teach(new SigmetSequenceDescriptor(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetValidTime(OccurrenceFrequency.AVERAGE));
        l.teach(new MWODesignator(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        l.teach(new AirSigmetObsOrForecast(OccurrenceFrequency.FREQUENT));
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
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && firstLexeme.getTACToken().matches("^SIG[CWE]$");
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.SIGMET;
            }
        });

        l.teach(new USSigmetStart(OccurrenceFrequency.FREQUENT));
        l.teach(new USSigmetValidUntil(OccurrenceFrequency.AVERAGE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

    private RecognizingAviMessageTokenLexer intlAirmetTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return (firstLexeme != null) && ((firstLexeme.getTACToken().matches("(\\w{4})\\sAIRMET.*") || firstLexeme.getTACToken().equals("AIRMET")));
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.AIRMET;
            }
        });
        l.teach(new AirmetStart(OccurrenceFrequency.RARE));
        l.teach(new SigmetSequenceDescriptor(OccurrenceFrequency.AVERAGE));
        l.teach(new SigmetValidTime(OccurrenceFrequency.AVERAGE));
        l.teach(new MWODesignator(OccurrenceFrequency.RARE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        l.teach(new AirSigmetObsOrForecast(OccurrenceFrequency.FREQUENT));
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
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && firstLexeme.getTACToken().matches("^SWX\\s+ADVISORY$");
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.SPACE_WEATHER_ADVISORY;
            }
        });

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
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                final Lexeme firstLexeme = sequence.getFirstLexeme();
                return firstLexeme != null && firstLexeme.getTACToken().matches("^VA\\s+ADVISORY$");
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.VOLCANIC_ASH_ADVISORY;
            }
        });

        l.teach(new VolcanicAshAdvisoryStart(OccurrenceFrequency.RARE));
        l.teach(new DTGIssueTime(OccurrenceFrequency.RARE));
        l.teach(new DTGIssueTimeLabel(OccurrenceFrequency.RARE));
        l.teach(new VolcanicAshPhenomena(OccurrenceFrequency.AVERAGE));
        l.teach(new AdvisoryPhenomenaTimeGroup(OccurrenceFrequency.AVERAGE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
        return l;
    }

}
