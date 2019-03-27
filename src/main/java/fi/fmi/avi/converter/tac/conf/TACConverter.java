package fi.fmi.avi.converter.tac.conf;

import java.util.regex.Pattern;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.TACParser;
import fi.fmi.avi.converter.tac.bulletin.GenericMeteorologicalBulletinParser;
import fi.fmi.avi.converter.tac.bulletin.GenericMeteorologicalBulletinTACSerializer;
import fi.fmi.avi.converter.tac.bulletin.SIGMETBulletinTACSerializer;
import fi.fmi.avi.converter.tac.bulletin.TAFBulletinTACSerializer;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageTACTokenizerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.Priority;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
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
import fi.fmi.avi.converter.tac.lexer.impl.token.EndToken;
import fi.fmi.avi.converter.tac.lexer.impl.token.ForecastMaxMinTemperature;
import fi.fmi.avi.converter.tac.lexer.impl.token.FractionalHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.ICAOCode;
import fi.fmi.avi.converter.tac.lexer.impl.token.IssueTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetarStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.MetricHorizontalVisibility;
import fi.fmi.avi.converter.tac.lexer.impl.token.Nil;
import fi.fmi.avi.converter.tac.lexer.impl.token.NoSignificantChanges;
import fi.fmi.avi.converter.tac.lexer.impl.token.NoSignificantWeather;
import fi.fmi.avi.converter.tac.lexer.impl.token.Remark;
import fi.fmi.avi.converter.tac.lexer.impl.token.RemarkStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.RoutineDelayedObservation;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayState;
import fi.fmi.avi.converter.tac.lexer.impl.token.RunwayVisualRange;
import fi.fmi.avi.converter.tac.lexer.impl.token.SeaState;
import fi.fmi.avi.converter.tac.lexer.impl.token.SnowClosure;
import fi.fmi.avi.converter.tac.lexer.impl.token.SpeciStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.SurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFChangeForecastTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFForecastChangeIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.TAFStart;
import fi.fmi.avi.converter.tac.lexer.impl.token.TrendChangeIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.TrendTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.ValidTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.VariableSurfaceWind;
import fi.fmi.avi.converter.tac.lexer.impl.token.Weather;
import fi.fmi.avi.converter.tac.lexer.impl.token.Whitespace;
import fi.fmi.avi.converter.tac.lexer.impl.token.WindShear;
import fi.fmi.avi.converter.tac.metar.ImmutableMETARTACParser;
import fi.fmi.avi.converter.tac.metar.METARTACParser;
import fi.fmi.avi.converter.tac.metar.METARTACSerializer;
import fi.fmi.avi.converter.tac.metar.SPECITACParser;
import fi.fmi.avi.converter.tac.metar.SPECITACSerializer;
import fi.fmi.avi.converter.tac.taf.ImmutableTAFTACParser;
import fi.fmi.avi.converter.tac.taf.TAFTACParser;
import fi.fmi.avi.converter.tac.taf.TAFTACSerializer;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

/**
 * TAC converter Spring configuration
 */
@Configuration
public class TACConverter {
    /**
     * Pre-configured spec for ICAO Annex 3 TAC format to {@link METAR} POJO.
     */
    public static final ConversionSpecification<String, METAR> TAC_TO_METAR_POJO = new ConversionSpecification<>(String.class, METAR.class,
            "ICAO Annex 3 TAC",
            null);

    /**
     * Pre-configured spec for {@link METAR} to ICAO Annex 3 TAC String.
     */
    public static final ConversionSpecification<METAR, String> METAR_POJO_TO_TAC = new ConversionSpecification<>(METAR.class, String.class, null,
            "ICAO Annex 3 TAC");

    /**
     * Pre-configured spec for ICAO Annex 3 TAC format to {@link SPECI} POJO.
     */
    public static final ConversionSpecification<String, SPECI> TAC_TO_SPECI_POJO = new ConversionSpecification<>(String.class, SPECI.class, "ICAO Annex 3 TAC",
            null);

    /**
     * Pre-configured spec for {@link SPECI} to ICAO Annex 3 TAC String.
     */
    public static final ConversionSpecification<SPECI, String> SPECI_POJO_TO_TAC = new ConversionSpecification<>(SPECI.class, String.class, null,
            "ICAO Annex 3 TAC");

    /**
     * Pre-configured spec for ICAO Annex 3 TAC format to {@link TAF} POJO.
     */
    public static final ConversionSpecification<String, TAF> TAC_TO_TAF_POJO = new ConversionSpecification<>(String.class, TAF.class, "ICAO Annex 3 TAC", null);

    /**
     * Pre-configured spec for {@link TAF} to ICAO Annex 3 TAC String.
     */
    public static final ConversionSpecification<TAF, String> TAF_POJO_TO_TAC = new ConversionSpecification<>(TAF.class, String.class, null, "ICAO Annex 3 TAC");

    /**
     * Pre-configured spec for ICAO Annex 3 TAC format to {@link METARImpl} POJO.
     */
    public static final ConversionSpecification<String, METARImpl> TAC_TO_IMMUTABLE_METAR_POJO = new ConversionSpecification<>(String.class, METARImpl.class,
            "ICAO Annex 3 TAC",
            null);


    /**
     * Pre-configured spec for ICAO Annex 3 TAC format to {@link TAFImpl} POJO.
     */
    public static final ConversionSpecification<String, TAFImpl> TAC_TO_IMMUTABLE_TAF_POJO = new ConversionSpecification<>(String.class, TAFImpl.class, "ICAO Annex 3 TAC", null);

    /**
     * Pre-configured spec for {@link fi.fmi.avi.model.taf.TAFBulletin} to TAC encoded TAF bulletin
     */
    public static final ConversionSpecification<TAFBulletin, String> TAF_BULLETIN_POJO_TO_TAC = new ConversionSpecification<>(TAFBulletin.class, String.class,
            null, "WMO GTS TAF Bulletin");

    /**
     * Pre-configured spec for {@link SIGMETBulletin} to TAC encoded TAF bulletin
     */
    public static final ConversionSpecification<SIGMETBulletin, String> SIGMET_BULLETIN_POJO_TO_TAC = new ConversionSpecification<>(SIGMETBulletin.class,
            String.class, null, "WMO GTS SIGMET Bulletin");


    /**
     * Pre-configured spec for WMO GTS text bulletin format to {@link fi.fmi.avi.model.GenericMeteorologicalBulletin} POJO.
     */
    public static final ConversionSpecification<String, GenericMeteorologicalBulletin> TAC_TO_GENERIC_BULLETIN_POJO = new ConversionSpecification<>(String.class,
            GenericMeteorologicalBulletin.class,
            "WMO GTS bulletin", null);

    /**
     * Pre-configured spec for {@link fi.fmi.avi.model.GenericMeteorologicalBulletin} POJO to WMO GTS text bulletin format.
     */
    public static final ConversionSpecification<GenericMeteorologicalBulletin, String> GENERIC_BULLETIN_POJO_TO_TAC = new ConversionSpecification<>(GenericMeteorologicalBulletin
            .class,
            String.class,
            null, "WMO GTS bulletin");



    private static final Pattern BULLETIN_START_PATTERN = Pattern.compile("^[A-Z]{4}[0-9]{2}$");

    @Bean
    AviMessageSpecificConverter<String, METAR> metarTACParser() {
        final TACParser<METAR> p = new METARTACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, METARImpl> immutableMetarTACParser() {
        final TACParser<METARImpl> p = new ImmutableMETARTACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, SPECI> speciTACParser() {
        final TACParser<SPECI> p = new SPECITACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, TAF> tafTACParser() {
        final TACParser<TAF> p = new TAFTACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, TAFImpl> immutableTafTACParser() {
        final TACParser<TAFImpl> p = new ImmutableTAFTACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser() {
        final TACParser<GenericMeteorologicalBulletin> p = new GenericMeteorologicalBulletinParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }
    
    @Bean
    AviMessageSpecificConverter<METAR, String> metarTACSerializer() {
        final METARTACSerializer s = new METARTACSerializer();
        addMetarAndSpeciCommonReconstructors(s);
        s.addReconstructor(Lexeme.Identity.METAR_START, new MetarStart.Reconstructor());
        return s;
    }

    @Bean
    AviMessageSpecificConverter<SPECI, String> speciTACSerializer() {
        final SPECITACSerializer s = new SPECITACSerializer();
        addMetarAndSpeciCommonReconstructors(s);
        s.addReconstructor(Lexeme.Identity.SPECI_START, new SpeciStart.Reconstructor());
        return s;
    }

    @Bean
    AviMessageSpecificConverter<TAF, String> tafTACSerializer() {
        return spawnTAFTACSerializer();
    }

    @Bean
    @Scope("prototype")
    AviMessageSpecificConverter<TAF, String> bulletinTAFTACSerializer() {
        return spawnTAFTACSerializer();
    }

    @Bean
    AviMessageSpecificConverter<TAFBulletin, String> tafBulletinTACSerializer() {
        final TAFBulletinTACSerializer s = new TAFBulletinTACSerializer();
        addCommonBulletinReconstructors(s);
        final TAFTACSerializer tafSerializer = (TAFTACSerializer) bulletinTAFTACSerializer();
        s.setTafSerializer(tafSerializer);
        return s;
    }

    @Bean
    AviMessageSpecificConverter<SIGMETBulletin, String> sigmetBulletinTACSerializer() {
        final SIGMETBulletinTACSerializer s = new SIGMETBulletinTACSerializer();
        addCommonBulletinReconstructors(s);
        return s;
    }

    @Bean
    AviMessageSpecificConverter<GenericMeteorologicalBulletin, String> genericBulletinTACSerializer() {
        final GenericMeteorologicalBulletinTACSerializer s = new GenericMeteorologicalBulletinTACSerializer();
        addCommonBulletinReconstructors(s);
        s.setLexer(aviMessageLexer());
        return s;
    }

    @Bean
    public AviMessageLexer aviMessageLexer() {
        final AviMessageLexerImpl l = new AviMessageLexerImpl();
        l.setLexingFactory(lexingFactory());
        l.addTokenLexer(metarTokenLexer());
        l.addTokenLexer(speciTokenLexer());
        l.addTokenLexer(tafTokenLexer());
        l.addTokenLexer(genericMeteorologicalBulletinTokenLexer());
        l.addTokenLexer(genericAviationWeathermessageTokenLexer()); //Keep this last, matches anything
        return l;
    }

    @Bean
    public AviMessageTACTokenizer tacTokenizer() {
        final AviMessageTACTokenizerImpl tokenizer = new AviMessageTACTokenizerImpl();
        tokenizer.setMETARSerializer((METARTACSerializer) metarTACSerializer());
        tokenizer.setSPECISerializer((SPECITACSerializer) speciTACSerializer());
        tokenizer.setTAFSerializer((TAFTACSerializer) tafTACSerializer());
        tokenizer.setTAFBulletinSerializer((TAFBulletinTACSerializer) tafBulletinTACSerializer());
        tokenizer.setSIGMETBulletinSerializer((SIGMETBulletinTACSerializer) sigmetBulletinTACSerializer());
        tokenizer.setGenericBulletinSerializer((GenericMeteorologicalBulletinTACSerializer) genericBulletinTACSerializer());
        return tokenizer;
    }
    
    @Bean
    public LexingFactory lexingFactory() {
        return new LexingFactoryImpl();
    }


    private void addMetarAndSpeciCommonReconstructors(final AbstractTACSerializer<?> s) {
        s.setLexingFactory(lexingFactory());
        s.addReconstructor(Lexeme.Identity.CORRECTION, new Correction.Reconstructor());
        s.addReconstructor(Lexeme.Identity.AERODROME_DESIGNATOR, new ICAOCode.Reconstructor());
        s.addReconstructor(Lexeme.Identity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(Lexeme.Identity.NIL, new Nil.Reconstructor());
        s.addReconstructor(Lexeme.Identity.ROUTINE_DELAYED_OBSERVATION, new RoutineDelayedObservation.Reconstructor());
        s.addReconstructor(Lexeme.Identity.AUTOMATED, new AutoMetar.Reconstructor());
        s.addReconstructor(Lexeme.Identity.SURFACE_WIND, new SurfaceWind.Reconstructor());
        s.addReconstructor(Lexeme.Identity.VARIABLE_WIND_DIRECTION, new VariableSurfaceWind.Reconstructor());
        s.addReconstructor(Lexeme.Identity.CAVOK, new CAVOK.Reconstructor());
        s.addReconstructor(Lexeme.Identity.HORIZONTAL_VISIBILITY, new MetricHorizontalVisibility.Reconstructor());
        s.addReconstructor(Lexeme.Identity.WEATHER, new Weather.Reconstructor(false));
        s.addReconstructor(Lexeme.Identity.NO_SIGNIFICANT_WEATHER, new NoSignificantWeather.Reconstructor());
        s.addReconstructor(Lexeme.Identity.CLOUD, new CloudLayer.Reconstructor());
        s.addReconstructor(Lexeme.Identity.TREND_CHANGE_INDICATOR, new TrendChangeIndicator.Reconstructor());
        s.addReconstructor(Lexeme.Identity.NO_SIGNIFICANT_CHANGES, new NoSignificantChanges.Reconstructor());
        s.addReconstructor(Lexeme.Identity.TREND_TIME_GROUP, new TrendTimeGroup.Reconstructor());
        s.addReconstructor(Lexeme.Identity.RUNWAY_VISUAL_RANGE, new RunwayVisualRange.Reconstructor());
        s.addReconstructor(Lexeme.Identity.AIR_DEWPOINT_TEMPERATURE, new AirDewpointTemperature.Reconstructor());
        s.addReconstructor(Lexeme.Identity.AIR_PRESSURE_QNH, new AtmosphericPressureQNH.Reconstructor());
        s.addReconstructor(Lexeme.Identity.RECENT_WEATHER, new Weather.Reconstructor(true));
        s.addReconstructor(Lexeme.Identity.WIND_SHEAR, new WindShear.Reconstructor());
        s.addReconstructor(Lexeme.Identity.SEA_STATE, new SeaState.Reconstructor());
        s.addReconstructor(Lexeme.Identity.RUNWAY_STATE, new RunwayState.Reconstructor());
        s.addReconstructor(Lexeme.Identity.SNOW_CLOSURE, new SnowClosure.Reconstructor());
        s.addReconstructor(Lexeme.Identity.COLOR_CODE, new ColorCode.Reconstructor());
        s.addReconstructor(Lexeme.Identity.REMARKS_START, new RemarkStart.Reconstructor());
        s.addReconstructor(Lexeme.Identity.REMARK, new Remark.Reconstructor());
        s.addReconstructor(Lexeme.Identity.END_TOKEN, new EndToken.Reconstructor());
    }

    private TAFTACSerializer spawnTAFTACSerializer() {
        final TAFTACSerializer s = new TAFTACSerializer();
        s.setLexingFactory(lexingFactory());
        s.addReconstructor(Lexeme.Identity.TAF_START, new TAFStart.Reconstructor());
        s.addReconstructor(Lexeme.Identity.AMENDMENT, new Amendment.Reconstructor());
        s.addReconstructor(Lexeme.Identity.CORRECTION, new Correction.Reconstructor());
        s.addReconstructor(Lexeme.Identity.AERODROME_DESIGNATOR, new ICAOCode.Reconstructor());
        s.addReconstructor(Lexeme.Identity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(Lexeme.Identity.NIL, new Nil.Reconstructor());
        s.addReconstructor(Lexeme.Identity.VALID_TIME, new ValidTime.Reconstructor());
        s.addReconstructor(Lexeme.Identity.CANCELLATION, new Cancellation.Reconstructor());
        s.addReconstructor(Lexeme.Identity.SURFACE_WIND, new SurfaceWind.Reconstructor());
        s.addReconstructor(Lexeme.Identity.CAVOK, new CAVOK.Reconstructor());
        s.addReconstructor(Lexeme.Identity.HORIZONTAL_VISIBILITY, new MetricHorizontalVisibility.Reconstructor());
        s.addReconstructor(Lexeme.Identity.WEATHER, new Weather.Reconstructor(false));
        s.addReconstructor(Lexeme.Identity.NO_SIGNIFICANT_WEATHER, new NoSignificantWeather.Reconstructor());
        s.addReconstructor(Lexeme.Identity.CLOUD, new CloudLayer.Reconstructor());
        s.addReconstructor(Lexeme.Identity.MAX_TEMPERATURE, new ForecastMaxMinTemperature.MaxReconstructor());
        s.addReconstructor(Lexeme.Identity.MIN_TEMPERATURE, new ForecastMaxMinTemperature.MinReconstructor());
        s.addReconstructor(Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR, new TAFForecastChangeIndicator.Reconstructor());
        s.addReconstructor(Lexeme.Identity.TAF_CHANGE_FORECAST_TIME_GROUP, new TAFChangeForecastTimeGroup.Reconstructor());
        s.addReconstructor(Lexeme.Identity.REMARKS_START, new RemarkStart.Reconstructor());
        s.addReconstructor(Lexeme.Identity.REMARK, new Remark.Reconstructor());
        s.addReconstructor(Lexeme.Identity.END_TOKEN, new EndToken.Reconstructor());
        return s;
    }

    private void addCommonBulletinReconstructors(final AbstractTACSerializer<?> s) {
        s.setLexingFactory(lexingFactory());
        s.addReconstructor(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, new BulletinHeaderDataDesignators.Reconstructor());
        s.addReconstructor(Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, new BulletinLocationIndicator.Reconstructor());
        s.addReconstructor(Lexeme.Identity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR, new BulletinHeadingBBBIndicator.Reconstructor());
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
            public AviationCodeListUser.MessageType getMessageType() {
                return AviationCodeListUser.MessageType.METAR;
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
            public AviationCodeListUser.MessageType getMessageType() {
                return AviationCodeListUser.MessageType.SPECI;
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
            public AviationCodeListUser.MessageType getMessageType() {
                return AviationCodeListUser.MessageType.TAF;
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
            public AviationCodeListUser.MessageType getMessageType() {
                return AviationCodeListUser.MessageType.BULLETIN;
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

    private RecognizingAviMessageTokenLexer genericAviationWeathermessageTokenLexer() {
        final RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        //Lambdas not allowed in Spring 3.x Java config files:
        l.setSuitabilityTester(new RecognizingAviMessageTokenLexer.SuitabilityTester() {
            @Override
            public boolean test(final LexemeSequence sequence) {
              return true;
            }

            @Override
            public AviationCodeListUser.MessageType getMessageType() {
                return AviationCodeListUser.MessageType.GENERIC;
            }

        });
        l.teach(new EndToken(Priority.HIGH));
        l.teach(new Whitespace(Priority.HIGH));
        return l;
    }

}
