package fi.fmi.avi.converter.tac.conf;

import static fi.fmi.avi.converter.tac.lexer.impl.token.LowWindStart.LOW_WIND_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXREPStart.WXREP_START;
import static fi.fmi.avi.converter.tac.lexer.impl.token.WXWarningStart.WX_WARNING_START;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.Priority;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomena;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomenaTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.AirDewpointTemperature;
import fi.fmi.avi.converter.tac.lexer.impl.token.Amendment;
import fi.fmi.avi.converter.tac.lexer.impl.token.AtmosphericPressureQNH;
import fi.fmi.avi.converter.tac.lexer.impl.token.AutoMetar;
import fi.fmi.avi.converter.tac.lexer.impl.token.BulletinHeaderDataDesignators;
import fi.fmi.avi.converter.tac.lexer.impl.token.BulletinHeadingBBBIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.BulletinLocationIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.CAVOK;
import fi.fmi.avi.converter.tac.lexer.impl.token.Cancellation;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer;
import fi.fmi.avi.converter.tac.lexer.impl.token.ColorCode;
import fi.fmi.avi.converter.tac.lexer.impl.token.Correction;
import fi.fmi.avi.converter.tac.lexer.impl.token.DTGIssueTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.EndToken;
import fi.fmi.avi.converter.tac.lexer.impl.token.ForecastMaxMinTemperature;
import fi.fmi.avi.converter.tac.lexer.impl.token.FractionalHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.ICAOCode;
import fi.fmi.avi.converter.tac.lexer.impl.token.IssueTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.LowWindStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetarStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetricHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.Nil;
import fi.fmi.avi.converter.tac.lexer.impl.token.NoSignificantChanges;
import fi.fmi.avi.converter.tac.lexer.impl.token.NoSignificantWeather;
import fi.fmi.avi.converter.tac.lexer.impl.token.REP;
import fi.fmi.avi.converter.tac.lexer.impl.token.Remark;
import fi.fmi.avi.converter.tac.lexer.impl.token.RemarkStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.RoutineDelayedObservation;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayVisualRange;
import fi.fmi.avi.converter.tac.lexer.impl.token.SeaState;
import fi.fmi.avi.converter.tac.lexer.impl.token.SigmetStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.SigmetValidTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.SnowClosure;
import fi.fmi.avi.converter.tac.lexer.impl.token.SpaceWeatherAdvisoryStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.SpeciStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.SurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFChangeForecastTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFForecastChangeIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.TrendChangeIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.TrendTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.USSigmetStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.USSigmetValidUntil;
import fi.fmi.avi.converter.tac.lexer.impl.token.ValidTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.VariableSurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.VolcanicAshAdvisoryStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.WXREPStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.WXWarningStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.Weather;
import fi.fmi.avi.converter.tac.lexer.impl.token.Whitespace;
import fi.fmi.avi.converter.tac.lexer.impl.token.WindShear;
import fi.fmi.avi.model.MessageType;

/**
 * TAC converter Lexing Spring configuration
 */
@Configuration
public class Lexing {

    private static final Pattern BULLETIN_START_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{2}$");

    @Bean
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
        l.addTokenLexer(spaceWeatherAdvisoryTokenLexer());
        l.addTokenLexer(volcanicAshAdvisoryTokenLexer());
        l.addTokenLexer(genericAviationWeatherMessageTokenLexer()); //Keep this last, matches anything
        return l;
    }
    
    @Bean
    public LexingFactory lexingFactory() {
        LexingFactoryImpl f = new LexingFactoryImpl();
        f.addTokenCombiningRule(fractionalHorizontalVisibilityCombinationRule());
        f.addTokenCombiningRule(windShearAllCombinationRule());
        f.addTokenCombiningRule(windShearCombinationRule());
        f.addTokenCombiningRule(probTempoCombinationRule());
        f.addTokenCombiningRule(lowWindCombinationRule());
        f.addTokenCombiningRule(wxWarningCombinationRule());
        f.addTokenCombiningRule(sigmetValidTimeCombinationRule());
        f.addTokenCombiningRule(usSigmetValidTimeCombinationRule());
        f.addTokenCombiningRule(advisoryStartCombinationRule());
        f.addTokenCombiningRule(dtgCombinationRule());
        f.addTokenCombiningRule(advisoryFctOffsetCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryPhenomenaCombinationRule());
        f.addTokenCombiningRule(spaceWeatherAdvisoryForecastTimeCombinationRule());
        f.addTokenCombiningRule(volcanicAshAdvisoryDtgCombinationRule());
        f.addTokenCombiningRule(volcanicAshAdvisoryCloudForecastCombinationRule());
        f.addTokenCombiningRule(volcanicAshAdvisoryForecastTimeCombinationRule());

        f.setMessageStartToken(MessageType.METAR,
                f.createLexeme("METAR", LexemeIdentity.METAR_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SPECI,
                f.createLexeme("SPECI", LexemeIdentity.SPECI_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.TAF,
                f.createLexeme("TAF", LexemeIdentity.TAF_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SPECIAL_AIR_REPORT,
                f.createLexeme("ARS", LexemeIdentity.ARS_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.VOLCANIC_ASH_ADVISORY,
                f.createLexeme("VA ADVISORY", LexemeIdentity.VOLCANIC_ASH_ADVISORY_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SPACE_WEATHER_ADVISORY,
                f.createLexeme("SWX ADVISORY", LexemeIdentity.SPACE_WEATHER_ADVISORY_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(MessageType.SIGMET,
                f.createLexeme("SIGMET", LexemeIdentity.SIGMET_START, Lexeme.Status.OK, true));

        //Non-standard types:
        f.setMessageStartToken(lowWind(),
                f.createLexeme("LOW WIND", LOW_WIND_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(wxRep(),
                f.createLexeme("WXREP", WXREP_START, Lexeme.Status.OK, true));
        f.setMessageStartToken(wxWarning(),
                f.createLexeme("WX", WX_WARNING_START, Lexeme.Status.OK, true));
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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

    private List<Predicate<String>> dtgCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return "DTG:".equals(s);
            }
        });
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]{8}/[0-9]{4}Z$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> advisoryFctOffsetCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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

    private List<Predicate<String>> spaceWeatherAdvisoryForecastTimeCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
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

    private List<Predicate<String>> volcanicAshAdvisoryDtgCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        l.teach(new MetarStart(Priority.HIGH));
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
        l.teach(new SpeciStart(Priority.HIGH));
        teachMetarAndSpeciCommonTokens(l);
        return l;
    }

    private void teachMetarAndSpeciCommonTokens(final RecognizingAviMessageTokenLexer l) {
        l.teach(new ICAOCode(Priority.LOW));
        l.teach(new IssueTime(Priority.LOW));
        l.teach(new CloudLayer(Priority.HIGH));
        l.teach(new Weather(Priority.NORMAL));
        l.teach(new SurfaceWind(Priority.LOW));
        l.teach(new VariableSurfaceWind(Priority.LOW));
        l.teach(new MetricHorizontalVisibility(Priority.NORMAL));
        l.teach(new FractionalHorizontalVisibility(Priority.NORMAL));
        l.teach(new TrendChangeIndicator(Priority.LOW));
        l.teach(new NoSignificantChanges(Priority.LOW));
        l.teach(new TrendTimeGroup(Priority.LOW));
        l.teach(new ColorCode(Priority.LOW));
        l.teach(new CAVOK(Priority.LOW));
        l.teach(new Correction(Priority.LOW));
        l.teach(new RunwayVisualRange(Priority.HIGH));
        l.teach(new AirDewpointTemperature(Priority.LOW));
        l.teach(new AtmosphericPressureQNH(Priority.LOW));
        l.teach(new RunwayState(Priority.LOW));
        l.teach(new SnowClosure(Priority.LOW));
        l.teach(new AutoMetar(Priority.LOW));
        l.teach(new NoSignificantWeather(Priority.LOW));
        l.teach(new RemarkStart(Priority.HIGH));
        l.teach(new Remark(Priority.HIGH));
        l.teach(new WindShear(Priority.LOW));
        l.teach(new SeaState(Priority.LOW));
        l.teach(new EndToken(Priority.LOW));
        l.teach(new Whitespace(Priority.HIGH));
        l.teach(new Nil(Priority.HIGH));
        l.teach(new RoutineDelayedObservation(Priority.HIGH));
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
        l.teach(new TAFStart(Priority.HIGH));
        l.teach(new ICAOCode(Priority.LOW));
        l.teach(new ValidTime(Priority.LOW));
        l.teach(new IssueTime(Priority.LOW));
        l.teach(new CloudLayer(Priority.HIGH));
        l.teach(new Weather(Priority.NORMAL));
        l.teach(new SurfaceWind(Priority.LOW));
        l.teach(new VariableSurfaceWind(Priority.LOW));
        l.teach(new MetricHorizontalVisibility(Priority.NORMAL));
        l.teach(new FractionalHorizontalVisibility(Priority.NORMAL));
        l.teach(new TAFForecastChangeIndicator(Priority.LOW));
        l.teach(new TAFChangeForecastTimeGroup(Priority.LOW));
        l.teach(new Correction(Priority.LOW));
        l.teach(new Amendment(Priority.LOW));
        l.teach(new Nil(Priority.HIGH));
        l.teach(new Cancellation(Priority.LOW));
        l.teach(new CAVOK(Priority.LOW));
        l.teach(new NoSignificantWeather(Priority.LOW));
        l.teach(new ForecastMaxMinTemperature(Priority.LOW));
        l.teach(new RemarkStart(Priority.HIGH));
        l.teach(new Remark(Priority.HIGH));
        l.teach(new EndToken(Priority.LOW));
        l.teach(new Whitespace(Priority.HIGH));
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
        l.teach(new BulletinHeaderDataDesignators(Priority.NORMAL));
        l.teach(new BulletinLocationIndicator(Priority.NORMAL));
        l.teach(new IssueTime(Priority.HIGH));
        l.teach(new BulletinHeadingBBBIndicator(Priority.NORMAL));
        l.teach(new EndToken(Priority.HIGH));
        l.teach(new Whitespace(Priority.HIGH));
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
        l.teach(new EndToken(Priority.HIGH));
        l.teach(new Whitespace(Priority.HIGH));
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
        l.teach(new LowWindStart(Priority.HIGH));
        l.teach(new ICAOCode(Priority.LOW));
        l.teach(new IssueTime(Priority.LOW));
        l.teach(new EndToken(Priority.LOW));
        l.teach(new Whitespace(Priority.HIGH));
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
        l.teach(new WXWarningStart(Priority.HIGH));
        l.teach(new ICAOCode(Priority.LOW));
        l.teach(new IssueTime(Priority.LOW));
        l.teach(new EndToken(Priority.LOW));
        l.teach(new Whitespace(Priority.HIGH));
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
        l.teach(new WXREPStart(Priority.HIGH));
        l.teach(new REP(Priority.HIGH));
        l.teach(new IssueTime(Priority.LOW));
        l.teach(new EndToken(Priority.LOW));
        l.teach(new Whitespace(Priority.HIGH));
        return l;
    }

    private RecognizingAviMessageTokenLexer intlSigmetTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
                return sequence.getFirstLexeme().hasNext() && sequence.getFirstLexeme().getNext().getTACToken().equals("SIGMET");
            }
            @Override
            public MessageType getMessageType() {
                return MessageType.SIGMET;
            }
        });
        l.teach(new SigmetStart(Priority.HIGH));
        l.teach(new SigmetValidTime(Priority.NORMAL));
        l.teach(new EndToken(Priority.LOW));
        l.teach(new Whitespace(Priority.HIGH));
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

        l.teach(new USSigmetStart(Priority.HIGH));
        l.teach(new USSigmetValidUntil(Priority.NORMAL));
        l.teach(new EndToken(Priority.LOW));
        l.teach(new Whitespace(Priority.HIGH));
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

        l.teach(new SpaceWeatherAdvisoryStart(Priority.LOW));
        l.teach(new DTGIssueTime(Priority.LOW));
        l.teach(new AdvisoryPhenomena(Priority.NORMAL));
        l.teach(new AdvisoryPhenomenaTimeGroup(Priority.NORMAL));
        l.teach(new Whitespace(Priority.HIGH));
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

        l.teach(new VolcanicAshAdvisoryStart(Priority.LOW));
        l.teach(new DTGIssueTime(Priority.LOW));
        l.teach(new AdvisoryPhenomena(Priority.NORMAL));
        l.teach(new AdvisoryPhenomenaTimeGroup(Priority.NORMAL));
        l.teach(new Whitespace(Priority.HIGH));
        return l;
    }

}
