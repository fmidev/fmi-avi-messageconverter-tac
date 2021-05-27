package fi.fmi.avi.converter.tac.conf;

import static fi.fmi.avi.converter.tac.lexer.impl.token.LowWindStart.LOW_WIND_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXREPStart.WXREP_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXWarningStart.WX_WARNING_START;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import fi.fmi.avi.converter.tac.lexer.impl.token.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.OccurrenceFrequency;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.model.MessageType;

/**
 * TAC converter Lexing Spring configuration
 */
@SuppressWarnings({ "Convert2Lambda", "Anonymous2MethodRef" })
@SuppressFBWarnings(value = "SIC_INNER_SHOULD_BE_STATIC_ANON", //
        justification = "Code is cleaner this way. Lambdas are not suitable because older Spring versions are unable to handle those.")
@Configuration
public class Lexing {

    private static final Pattern BULLETIN_START_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{2}$");

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
        f.addTokenCombiningRule(latitudeLongitudePairCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryEffect());
        f.addTokenCombiningRule(spaceWeatherAdvisoryEffectType());
        f.addTokenCombiningRule(spaceWeatherAdvisoryEffectTypeHFCom());
        f.addTokenCombiningRule(spaceWeatherAdvisoryDaylightSide());
        f.addTokenCombiningRule(spaceWeatherAdvisoryPhenomenon());
        f.addTokenCombiningRule(spaceWeatherAdvisoryNextAdvisoryCombinationRules());
        f.addTokenCombiningRule(spaceWeatherAdvisoryIssuedAtCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryIssuedByCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryNoAdvisoriesCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryReplaceAdvisoryCombinationRules());
        f.addTokenCombiningRule(spaceWeatherAdvisoryReplaceAdvisoryWithSpaceCombinationRules());
        f.addTokenCombiningRule(intlSigmetStartRule());
//        f.addTokenCombiningRule(intlSigmetFirName3CombinationRule());
//        f.addTokenCombiningRule(intlSigmetFirNameCombinationRule());
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


//        f.addTokenCombiningRule(intlAirmetStartRule());
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
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]*$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]*/[0-9]*[A-Z]{2}$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> windShearAllCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "WS".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "ALL".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "RWY".equals(s);
            }
        });
        return retval;
    }

    private List<Predicate<String>> windShearCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "WS".equals(s);
            }
        });
        // Windshear token for a particular runway has changed between 16th and 19th edition of Annex 3
        //  16th = "WS RWYnn[LRC]"
        //  19th = "WS Rnn[LRC]"
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^R(?:WY)?[0-9]{2}[LRC]?$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> probTempoCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^PROB[34]0$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "TEMPO".equals(s);
            }
        });
        return retval;
    }

    private List<Predicate<String>> lowWindCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "LOW".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "WIND".equals(s);
            }
        });
        return retval;
    }

    private List<Predicate<String>> wxWarningCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "WX".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "WRNG".equals(s);
            }
        });
        return retval;
    }

    private List<Predicate<String>> sigmetValidTimeCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "VALID".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]{6}[/-][0-9]{6}$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> usSigmetValidTimeCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "VALID".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "UNTIL".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]{2}[0-9]{2}Z$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> advisoryStartCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(?:SWX)|(?:VA)$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "ADVISORY".equals(s);
            }
        });
        return retval;
    }

    private List<Predicate<String>> advisoryFctOffsetCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^\\+[0-9]{1,2}$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("HR:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryPhenomenaCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(?:OBS|FCST)$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^SWX:?$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffect() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^SWX$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^EFFECT:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryHorizontalLimitCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([WE])\\d{1,5}$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^-$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([WE])\\d{1,5}$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryVerticalLimitCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^ABV$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^FL\\d{3}$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectType() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^SATCOM|GNSS|RADIATION$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^MOD|SEV$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryEffectTypeHFCom() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^HF$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^COM$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^MOD|SEV$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryDaylightSide() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^DAYLIGHT$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^SIDE$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryPhenomenon() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^FCST$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^SWX:$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^\\+\\d{1,2}$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^HR:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryForecastTimeCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^FCST\\s+SWX");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^\\+[0-9]{1,2}\\s+HR:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> advisoryNumberCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^ADVISORY$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^NR:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> latitudeLongitudePairCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[NS]\\d+$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[WE]\\d+$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryIssuedByCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^WILL$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^BE$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^ISSUED$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^BY$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            //TODO:
            public boolean test(final String s) {
                return s.matches("^[0-9]{4}[0-9]{2}[0-9]{2}/[0-9]{2}[0-9]{2}Z$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryIssuedAtCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]{4}[0-9]{2}[0-9]{2}/[0-9]{2}[0-9]{2}Z$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNoAdvisoriesCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^NO$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^FURTHER$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^ADVISORIES$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNextAdvisoryCombinationRules() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^NXT$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^ADVISORY:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryReplaceAdvisoryWithSpaceCombinationRules() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^NR$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^RPLC$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryReplaceAdvisoryCombinationRules() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^NR$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^RPLC:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNoExpectedCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^NO$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^SWX$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^EXP$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNotAvailableCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^NOT$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^AVBL$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> volcanicAshAdvisoryDtgCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "OBS".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "VA".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "DTG:".equals(s);
            }
        });
        return retval;
    }

    private List<Predicate<String>> volcanicAshAdvisoryCloudForecastCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "FCST".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "VA".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "CLD".equals(s);
            }
        });
        return retval;
    }

    private List<Predicate<String>> volcanicAshAdvisoryForecastTimeCombinationRule() {
        final List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^FCST\\s+VA\\s+CLD");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^\\+[0-9]{1,2}\\s+HR:$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetFirNameCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\w*)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(FIR|UIR|FIR/UIR|CTA)$");
            }
        });
        return retval;
    }
    private List<Predicate<String>> intlSigmetEntireFirCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^ENTIRE$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(FIR|UIR|FIR/UIR|CTA)$");
            }
        });
        return retval;
    }
    private List<Predicate<String>> intlSigmetLineCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^N|NE|E|SE|S|SW|W|NW$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^OF$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^LINE$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule2() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^-$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetLineCombinationRule3() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(N|NE|E|SE|S|SW|W|NW)\\sOF\\sLINE\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^-$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetOutsideLatLonCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^N|S|E|W$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^OF$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NSEW]\\d+)");
            }
        });
            return retval;
    }

    private List<Predicate<String>> intlSigmetOutsideLatLonCombinationRuleWithAnd() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(N|S|E|W)\\sOF\\s([NSEW]\\d+)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^AND$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(N|S|E|W)\\sOF\\s([NSEW]\\d+)$");
            }
        });

            return retval;
    }


    private List<Predicate<String>> intlSigmetStartRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[A-Z]{4}");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(SIGMET|AIRMET)$");
            }
        });
       return retval;
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule1() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("TOP");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(ABV|BLW)$");
            }
        });
       return retval;
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule2() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(TOP ABV|ABV)$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(FL[0-9]{3}|[0-9]{4,5}FT)$");
            }
        });
       return retval;
    }

    private List<Predicate<String>> intlSigmetLevelCombinationRule3() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(TOP ABV|TOP BLW|TOP)$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
            return s.matches("^(FL[0-9]{3})$");
            }
        });
       return retval;
    }

    private List<Predicate<String>> intlSigmetMovingCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("MOV");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
//                return s.matches("^(FL\\d{3}/\\d{3})|((SFC/)?(FL\\d{3}|\\d{4}M|\\d{4,5}FT))");
                return s.matches("^(N|NNE|NE|ENE|E|ESE|SE|SSE|S|SSW|SW|WSW|W|WNW|NW|NNW)$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
    //                return s.matches("^(FL\\d{3}/\\d{3})|((SFC/)?(FL\\d{3}|\\d{4}M|\\d{4,5}FT))");
                return s.matches("^([0-9]{2})(KT|KMH)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetObsFcstAtCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(OBS|FCST)$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("AT");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]{4}Z$");
            }
        });
        return retval;
}

    private List<Predicate<String>> intlSigmetAprxCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^APRX$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\d{2}(KM|NM))$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^WID$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^LINE$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^BTN$");
            }
        });

        return retval;
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule2() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^-$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule3() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^-$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetAprxCombinationRule4() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^APRX\\s(\\d{2}(KM|NM))\\sWID\\sLINE\\sBTN\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})\\s-\\s([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^-$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^([NS]\\d{2,4}\\s[EW]\\d{3,5})$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetCancelCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^CNL$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(SIGMET|AIRMET)$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^\\w?\\d?\\d$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\d{2}\\d{2}\\d{2}/\\d{2}\\d{2}\\d{2})$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetVaCancelCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^CNL SIGMET (\\w?\\d?\\d) (\\d{2}\\d{2}\\d{2}/\\d{2}\\d{2}\\d{2})$");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("VA");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("MOV");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("TO");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^\\w*$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("FIR");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetNoVaExpCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("NO");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("VA");
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("EXP");
            }
        });

        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule1() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(ISOL|OCNL)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(TS|TSGR)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule2() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("MT");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("OBSC");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule3() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(ISOL|OCNL|FRQ)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(CB|TCU)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule4() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("MOD");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(TURB|ICE|MTW)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule5() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(BKN|OVC)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("CLD");
            }
        });

        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule6() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(SFC)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(WIND|VIS)$");
            }
        });

        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule7() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("SFC VIS");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\d{2,4}M)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule8() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(SFC VIS\\s\\d{2,4}M)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\((BR|DS|DU|DZ|FC|FG|FU|GR|GS|HZ|PL|PO|RA|SA|SG|SN|SQ|SS|VA)\\))$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule9() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("SFC WIND");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\d{3}/\\d{2,3})(KT|MPS)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlAirmetPhenomenonCombinationRule10() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(BKN|OVC)\\sCLD$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^((\\d{3,4})|SFC)/(ABV)?((\\d{3,4}M)|(\\d{4,5}FT))$");
            }
        });

        return retval;
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule1() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.equals("SEV");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(TURB|ICE|MTW)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule2() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(OBSC|EMBD|FRQ|SQL)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(TS|TSGR)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule3() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(HVY)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(DS|SS)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule4() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(RDOACT)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(CLD)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule5() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(VA)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(CLD)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetPhenomenonCombinationRule6() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(VA)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(ERUPTION)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetVolcanoName1() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(MT)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\w*)$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetVolcanoPosition() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(PSN)$");
            }
        });

        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(N|S)(\\d{2,4}) (E|W)(\\d{3,5})");
            }
        });
        return retval;
    }

    private List<Predicate<String>> intlSigmetPhenomenonFZRACombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(SEV ICE)$");
            }
        });

        // retval.add(new Predicate<String>() {
        //     @Override
        //     public boolean test(final String s) {
        //         return s.matches("^(ICE)$");
        //     }
        // });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(\\(FZRA\\))$");
            }
        });
        return retval;
    }

    private RecognizingAviMessageTokenLexer metarTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                return sequence.getFirstLexeme().getTACToken().equals("METAR");
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
                return sequence.getFirstLexeme().getTACToken().equals("SPECI");
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
                return sequence.getFirstLexeme().getTACToken().equals("TAF");
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
                if (sequence.getFirstLexeme() != null) {
                    return BULLETIN_START_PATTERN.matcher(sequence.getFirstLexeme().getTACToken()).matches();
                }
                return false;
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
                return sequence.getFirstLexeme().getTACToken().equals("LOW WIND");
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
                return sequence.getFirstLexeme().getTACToken().equals("WX WRNG");
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
                return sequence.getFirstLexeme().getTACToken().equals("WXREP");
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
                /* 2021-03-30 You can not call the getIdentity in some cases (for Sigmet lexing) */
                if (sequence.
                  getFirstLexeme().
                    getIdentity() == null) {
                        return false;
                    }
                return "SIGMET".equals(sequence.
                getFirstLexeme().getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.SIGMET;
            }
        });
        l.teach(new SigmetStart(OccurrenceFrequency.RARE));
        l.teach(new SigmetSequenceDescriptor(OccurrenceFrequency.AVERAGE));
        l.teach(new AirspaceDesignator(OccurrenceFrequency.RARE));
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
                return sequence.getFirstLexeme().getTACToken().matches("^SIG[CWE]$");
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
                /* 2021-03-30 You can not call the getIdentity in some cases (for Airmet lexing) */
                if (sequence.
                  getFirstLexeme().
                    getIdentity() == null) {
                        return false;
                    }
                return "AIRMET".equals(sequence.
                getFirstLexeme().getTACToken());
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.AIRMET;
            }
        });
        l.teach(new AirmetStart(OccurrenceFrequency.RARE));
        l.teach(new SigmetSequenceDescriptor(OccurrenceFrequency.AVERAGE));
        l.teach(new AirspaceDesignator(OccurrenceFrequency.RARE));
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
                return sequence.getFirstLexeme().getTACToken().matches("^SWX\\s+ADVISORY$");
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
        l.teach(new AdvisoryNumberLabel(OccurrenceFrequency.RARE));
        l.teach(new AdvisoryNumber(OccurrenceFrequency.RARE));
        l.teach(new SWXEffectLabel(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXEffect(OccurrenceFrequency.AVERAGE));
        l.teach(new SWXEffectConjuction(OccurrenceFrequency.FREQUENT));
        l.teach(new SWXPresetLocation(OccurrenceFrequency.AVERAGE));
        l.teach(new NextAdvisory(OccurrenceFrequency.RARE));
        l.teach(new NextAdvisoryLabel(OccurrenceFrequency.RARE));
        l.teach(new NoFurtherAdvisories(OccurrenceFrequency.AVERAGE));
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
                return sequence.getFirstLexeme().getTACToken().matches("^VA\\s+ADVISORY$");
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
