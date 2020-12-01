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
import org.springframework.context.annotation.Primary;

import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.OccurrenceFrequency;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryNumber;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryNumberLabel;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomenaTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryRemarkStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryStatus;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryStatusLabel;
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
import fi.fmi.avi.converter.tac.lexer.impl.token.NextAdvisory;
import fi.fmi.avi.converter.tac.lexer.impl.token.NextAdvisoryLabel;
import fi.fmi.avi.converter.tac.lexer.impl.token.Nil;
import fi.fmi.avi.converter.tac.lexer.impl.token.NoFurtherAdvisories;
import fi.fmi.avi.converter.tac.lexer.impl.token.NoSignificantChanges;
import fi.fmi.avi.converter.tac.lexer.impl.token.NoSignificantWeather;
import fi.fmi.avi.converter.tac.lexer.impl.token.PolygonCoordinatePair;
import fi.fmi.avi.converter.tac.lexer.impl.token.PolygonCoordinatePairSeparator;
import fi.fmi.avi.converter.tac.lexer.impl.token.REP;
import fi.fmi.avi.converter.tac.lexer.impl.token.Remark;
import fi.fmi.avi.converter.tac.lexer.impl.token.RemarkStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.ReplaceAdvisoryNumber;
import fi.fmi.avi.converter.tac.lexer.impl.token.ReplaceAdvisoryNumberLabel;
import fi.fmi.avi.converter.tac.lexer.impl.token.RoutineDelayedObservation;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayVisualRange;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXAdvisoryStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXCenter;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXCenterLabel;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXEffect;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXEffectConjuction;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXEffectLabel;
import fi.fmi.avi.converter.tac.lexer.impl.token.DTGIssueTimeLabel;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXNotAvailable;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXNotExpected;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXPhenomena;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXPhenonmenonLongitudeLimit;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXPresetLocation;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXVerticalLimit;
import fi.fmi.avi.converter.tac.lexer.impl.token.SeaState;
import fi.fmi.avi.converter.tac.lexer.impl.token.SigmetStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.SigmetValidTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.SnowClosure;
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
import fi.fmi.avi.converter.tac.lexer.impl.token.VolcanicAshPhenomena;
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

    private List<Predicate<String>> spaceWeatherAdvisoryEffect() {
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^(W|E)\\d{1,5}$");
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
                return s.matches("^(W|E)\\d{1,5}$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryVerticalLimitCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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

    private List<Predicate<String>> advisoryNumberCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
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

    private List<Predicate<String>> spaceWeatherAdvisoryPolygonCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
        for(int i = 0; i < 5; i++) {
            retval.add(new Predicate<String>() {
                @Override
                public boolean test(final String s) {
                    return s.matches("^(N|S)\\d+$");
                }
            });
            retval.add(new Predicate<String>() {
                @Override
                public boolean test(final String s) {
                    return s.matches("^(W|E)\\d+$");
                }
            });

            if(i < 4) {
                retval.add(new Predicate<String>() {
                    @Override
                    public boolean test(final String s) {
                        return s.matches("^-$");
                    }
                });
            }
        }
        return retval;
    }

    private List<Predicate<String>> latitudeLongitudePairCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
        retval.add(new Predicate<String>() {
            @Override
            public boolean test(final String s) {
                return s.matches("^[0-9]{4}[0-9]{2}[0-9]{2}/[0-9]{2}[0-9]{2}Z$");
            }
        });
        return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNoAdvisoriesCombinationRule() {
        List<Predicate<String>> retval = new ArrayList<>();
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
        }); return retval;
    }

    private List<Predicate<String>> spaceWeatherAdvisoryNextAdvisoryCombinationRules() {
        List<Predicate<String>> retval = new ArrayList<>();
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

    private List<Predicate<String>>  spaceWeatherAdvisoryReplaceAdvisoryWithSpaceCombinationRules() {
        List<Predicate<String>> retval = new ArrayList<>();
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

    private List<Predicate<String>>  spaceWeatherAdvisoryReplaceAdvisoryCombinationRules() {
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
        List<Predicate<String>> retval = new ArrayList<>();
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
                return sequence.getFirstLexeme().hasNext() && sequence.getFirstLexeme().getNext().getTACToken().equals("SIGMET");
            }

            @Override
            public MessageType getMessageType() {
                return MessageType.SIGMET;
            }
        });
        l.teach(new SigmetStart(OccurrenceFrequency.FREQUENT));
        l.teach(new SigmetValidTime(OccurrenceFrequency.AVERAGE));
        l.teach(new EndToken(OccurrenceFrequency.RARE));
        l.teach(new Whitespace(OccurrenceFrequency.FREQUENT));
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
