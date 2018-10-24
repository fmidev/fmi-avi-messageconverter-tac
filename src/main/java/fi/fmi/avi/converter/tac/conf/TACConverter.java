package fi.fmi.avi.converter.tac.conf;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.ImmutableMETARTACParser;
import fi.fmi.avi.converter.tac.ImmutableTAFLexemeSequenceParser;
import fi.fmi.avi.converter.tac.ImmutableTAFTACParser;
import fi.fmi.avi.converter.tac.LexemeSequenceParser;
import fi.fmi.avi.converter.tac.METARTACParser;
import fi.fmi.avi.converter.tac.METARTACSerializer;
import fi.fmi.avi.converter.tac.SPECITACParser;
import fi.fmi.avi.converter.tac.SPECITACSerializer;
import fi.fmi.avi.converter.tac.TACParser;
import fi.fmi.avi.converter.tac.TAFBulletinTACSerializer;
import fi.fmi.avi.converter.tac.TAFLexemeSequenceParser;
import fi.fmi.avi.converter.tac.TAFTACParser;
import fi.fmi.avi.converter.tac.TAFTACSerializer;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageLexerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageTACTokenizerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.METARDetector;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.Priority;
import fi.fmi.avi.converter.tac.lexer.impl.RecognizingAviMessageTokenLexer;
import fi.fmi.avi.converter.tac.lexer.impl.SPECIDetector;
import fi.fmi.avi.converter.tac.lexer.impl.TAFBulletinDetector;
import fi.fmi.avi.converter.tac.lexer.impl.TAFDetector;
import fi.fmi.avi.converter.tac.lexer.impl.token.AirDewpointTemperature;
import fi.fmi.avi.converter.tac.lexer.impl.token.Amendment;
import fi.fmi.avi.converter.tac.lexer.impl.token.AtmosphericPressureQNH;
import fi.fmi.avi.converter.tac.lexer.impl.token.AutoMetar;
import fi.fmi.avi.converter.tac.lexer.impl.token.BulletinHeadingBBBIndicator;
import fi.fmi.avi.converter.tac.lexer.impl.token.BulletinHeadingDataDesignators;
import fi.fmi.avi.converter.tac.lexer.impl.token.BulletinHeadingLocationIndicator;
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
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
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
     * Pre-configured spec for {@link fi.fmi.avi.model.taf.TAFBulletin} to TAC encoded TAF bulletin
     */
    public static final ConversionSpecification<String, TAFBulletin> TAC_TO_TAF_BULLETIN_POJO = new ConversionSpecification<>(String.class, TAFBulletin.class,
            "WMO GTS TAF Bulletin", null);

    /**
     * Pre-configured spec for LexemeSequence to {@link TAF} POJO.
     */
    public static final ConversionSpecification<LexemeSequence, TAF> LEXEME_SEQUENCE_TO_TAF_POJO = new ConversionSpecification<>(LexemeSequence.class,
            TAF.class, null, null);

    /**
     * Pre-configured spec for LexemeSequence to {@link TAFImpl} POJO.
     */
    public static final ConversionSpecification<LexemeSequence, TAFImpl> LEXEME_SEQUENCE_TO_IMMUTABLE_TAF_POJO = new ConversionSpecification<>(
            LexemeSequence.class, TAFImpl.class, null, null);




    @Bean
    AviMessageSpecificConverter<String, METAR> metarTACParser() {
        TACParser<METAR> p = new METARTACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, METARImpl> immutableMetarTACParser() {
        TACParser<METARImpl> p = new ImmutableMETARTACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, SPECI> speciTACParser() {
        TACParser<SPECI> p = new SPECITACParser();
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, TAF> tafTACParser() {
        TAFTACParser p = new TAFTACParser();
        p.setLexemeSequenceParser(tafLexemeSequenceParserInternal());
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, TAFImpl> immutableTafTACParser() {
        ImmutableTAFTACParser p = new ImmutableTAFTACParser();
        p.setLexemeSequenceParser(immutableTafLexemeSequenceParserInternal());
        p.setTACLexer(aviMessageLexer());
        return p;
    }

    @Bean
    AviMessageSpecificConverter<LexemeSequence, TAFImpl> immutableTafLexemeSequenceParser() {
        return immutableTafLexemeSequenceParserInternal();
    }

    @Bean
    AviMessageSpecificConverter<LexemeSequence, TAF> tafLexemeSequenceParser() {
        return tafLexemeSequenceParserInternal();
    }
    
    @Bean
    AviMessageSpecificConverter<METAR, String> metarTACSerializer() {
        METARTACSerializer s = new METARTACSerializer();
        addMetarAndSpeciCommonReconstructors(s);
        s.addReconstructor(Lexeme.Identity.METAR_START, new MetarStart.Reconstructor());
        return s;
    }

    @Bean
    AviMessageSpecificConverter<SPECI, String> speciTACSerializer() {
        SPECITACSerializer s = new SPECITACSerializer();
        addMetarAndSpeciCommonReconstructors(s);
        s.addReconstructor(Lexeme.Identity.SPECI_START, new SpeciStart.Reconstructor());
        return s;
    }

    private LexemeSequenceParser<TAFImpl> immutableTafLexemeSequenceParserInternal() {
        return new ImmutableTAFLexemeSequenceParser();
    }

    private LexemeSequenceParser<TAF> tafLexemeSequenceParserInternal() {
        return new TAFLexemeSequenceParser();
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


    @Bean
    AviMessageSpecificConverter<TAF, String> tafTACSerializer() {
        return spawnTAFTACSerializer();
    }

    @Bean
    @Scope("prototype")
    AviMessageSpecificConverter<TAF, String> bulletinTAFTACSerializer() {
        return spawnTAFTACSerializer();
    }

    private TAFTACSerializer spawnTAFTACSerializer() {
        TAFTACSerializer s = new TAFTACSerializer();
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
        s.addReconstructor(Lexeme.Identity.END_TOKEN,new EndToken.Reconstructor());
        return s;
    }

    @Bean
    AviMessageSpecificConverter<TAFBulletin, String> tafBulletinTACSerializer() {
        TAFBulletinTACSerializer s = new TAFBulletinTACSerializer();
        s.setLexingFactory(lexingFactory());
        TAFTACSerializer tafSerializer = (TAFTACSerializer) bulletinTAFTACSerializer();
        s.setTafSerializer(tafSerializer);
        s.addReconstructor(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, new BulletinHeadingDataDesignators.Reconstructor());
        s.addReconstructor(Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, new BulletinHeadingLocationIndicator.Reconstructor());
        s.addReconstructor(Lexeme.Identity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR, new BulletinHeadingBBBIndicator.Reconstructor());
        return s;
    }
    
    @Bean
    public AviMessageLexer aviMessageLexer() {
        AviMessageLexerImpl l = new AviMessageLexerImpl();
        l.setLexingFactory(lexingFactory());
        l.addTokenLexer(new METARDetector(), metarTokenLexer());
        l.addTokenLexer(new SPECIDetector(), speciTokenLexer());
        l.addTokenLexer(new TAFDetector(), tafTokenLexer());
        l.addTokenLexer(new TAFBulletinDetector(), tafBulletinTokenLexer());
        return l;
    }

    @Bean
    public AviMessageTACTokenizer tacTokenizer() {
        AviMessageTACTokenizerImpl tokenizer = new AviMessageTACTokenizerImpl();
        tokenizer.setMETARSerializer((METARTACSerializer) metarTACSerializer());
        tokenizer.setSPECISerializer((SPECITACSerializer) speciTACSerializer());
        tokenizer.setTAFSerializer((TAFTACSerializer)tafTACSerializer());
        tokenizer.setTAFBUlletinSerializer((TAFBulletinTACSerializer) tafBulletinTACSerializer());
        return tokenizer;
    }
    
    @Bean
    public LexingFactory lexingFactory() {
        return new LexingFactoryImpl();
    }

    private RecognizingAviMessageTokenLexer metarTokenLexer() {
        RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.teach(new MetarStart(Priority.HIGH));
        teachMetarAndSpeciCommonTokens(l);
        return l;
    }

    private RecognizingAviMessageTokenLexer speciTokenLexer() {
        RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
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
        RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        teachTafCommonTokens(l);
        return l;
    }

    private RecognizingAviMessageTokenLexer tafBulletinTokenLexer() {
        RecognizingAviMessageTokenLexer l = new RecognizingAviMessageTokenLexer();
        l.teach(new BulletinHeadingDataDesignators(Priority.LOW));
        l.teach(new BulletinHeadingLocationIndicator(Priority.LOW));
        l.teach(new BulletinHeadingBBBIndicator(Priority.LOW));
        teachTafCommonTokens(l);
        return l;
    }

    private void teachTafCommonTokens(final RecognizingAviMessageTokenLexer l) {
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
    }

}
