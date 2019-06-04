package fi.fmi.avi.converter.tac.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.bulletin.GenericMeteorologicalBulletinTACSerializer;
import fi.fmi.avi.converter.tac.bulletin.SIGMETBulletinTACSerializer;
import fi.fmi.avi.converter.tac.bulletin.TAFBulletinTACSerializer;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageTACTokenizerImpl;
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
import fi.fmi.avi.converter.tac.lexer.impl.token.WindShear;
import fi.fmi.avi.converter.tac.metar.METARTACSerializer;
import fi.fmi.avi.converter.tac.metar.SPECITACSerializer;
import fi.fmi.avi.converter.tac.taf.TAFTACSerializer;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;

/**
 * TAC converter serializing Spring configuration
 */
@Configuration
@Import(Lexing.class)
public class Serializing {

    @Autowired
    private AviMessageLexer aviMessageLexer;

    @Autowired
    private LexingFactory lexingFactory;

    
    @Bean
    AviMessageSpecificConverter<METAR, String> metarTACSerializer() {
        final METARTACSerializer s = new METARTACSerializer();
        addMetarAndSpeciCommonReconstructors(s);
        s.addReconstructor(LexemeIdentity.METAR_START, new MetarStart.Reconstructor());
        return s;
    }

    @Bean
    AviMessageSpecificConverter<SPECI, String> speciTACSerializer() {
        final SPECITACSerializer s = new SPECITACSerializer();
        addMetarAndSpeciCommonReconstructors(s);
        s.addReconstructor(LexemeIdentity.SPECI_START, new SpeciStart.Reconstructor());
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
        s.setLexer(aviMessageLexer);
        return s;
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

    private void addMetarAndSpeciCommonReconstructors(final AbstractTACSerializer<?> s) {
        s.setLexingFactory(lexingFactory);
        s.addReconstructor(LexemeIdentity.CORRECTION, new Correction.Reconstructor());
        s.addReconstructor(LexemeIdentity.AERODROME_DESIGNATOR, new ICAOCode.Reconstructor());
        s.addReconstructor(LexemeIdentity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.NIL, new Nil.Reconstructor());
        s.addReconstructor(LexemeIdentity.ROUTINE_DELAYED_OBSERVATION, new RoutineDelayedObservation.Reconstructor());
        s.addReconstructor(LexemeIdentity.AUTOMATED, new AutoMetar.Reconstructor());
        s.addReconstructor(LexemeIdentity.SURFACE_WIND, new SurfaceWind.Reconstructor());
        s.addReconstructor(LexemeIdentity.VARIABLE_WIND_DIRECTION, new VariableSurfaceWind.Reconstructor());
        s.addReconstructor(LexemeIdentity.CAVOK, new CAVOK.Reconstructor());
        s.addReconstructor(LexemeIdentity.HORIZONTAL_VISIBILITY, new MetricHorizontalVisibility.Reconstructor());
        s.addReconstructor(LexemeIdentity.WEATHER, new Weather.Reconstructor(false));
        s.addReconstructor(LexemeIdentity.NO_SIGNIFICANT_WEATHER, new NoSignificantWeather.Reconstructor());
        s.addReconstructor(LexemeIdentity.CLOUD, new CloudLayer.Reconstructor());
        s.addReconstructor(LexemeIdentity.TREND_CHANGE_INDICATOR, new TrendChangeIndicator.Reconstructor());
        s.addReconstructor(LexemeIdentity.NO_SIGNIFICANT_CHANGES, new NoSignificantChanges.Reconstructor());
        s.addReconstructor(LexemeIdentity.TREND_TIME_GROUP, new TrendTimeGroup.Reconstructor());
        s.addReconstructor(LexemeIdentity.RUNWAY_VISUAL_RANGE, new RunwayVisualRange.Reconstructor());
        s.addReconstructor(LexemeIdentity.AIR_DEWPOINT_TEMPERATURE, new AirDewpointTemperature.Reconstructor());
        s.addReconstructor(LexemeIdentity.AIR_PRESSURE_QNH, new AtmosphericPressureQNH.Reconstructor());
        s.addReconstructor(LexemeIdentity.RECENT_WEATHER, new Weather.Reconstructor(true));
        s.addReconstructor(LexemeIdentity.WIND_SHEAR, new WindShear.Reconstructor());
        s.addReconstructor(LexemeIdentity.SEA_STATE, new SeaState.Reconstructor());
        s.addReconstructor(LexemeIdentity.RUNWAY_STATE, new RunwayState.Reconstructor());
        s.addReconstructor(LexemeIdentity.SNOW_CLOSURE, new SnowClosure.Reconstructor());
        s.addReconstructor(LexemeIdentity.COLOR_CODE, new ColorCode.Reconstructor());
        s.addReconstructor(LexemeIdentity.REMARKS_START, new RemarkStart.Reconstructor());
        s.addReconstructor(LexemeIdentity.REMARK, new Remark.Reconstructor());
        s.addReconstructor(LexemeIdentity.END_TOKEN, new EndToken.Reconstructor());
    }

    private TAFTACSerializer spawnTAFTACSerializer() {
        final TAFTACSerializer s = new TAFTACSerializer();
        s.setLexingFactory(lexingFactory);
        s.addReconstructor(LexemeIdentity.TAF_START, new TAFStart.Reconstructor());
        s.addReconstructor(LexemeIdentity.AMENDMENT, new Amendment.Reconstructor());
        s.addReconstructor(LexemeIdentity.CORRECTION, new Correction.Reconstructor());
        s.addReconstructor(LexemeIdentity.AERODROME_DESIGNATOR, new ICAOCode.Reconstructor());
        s.addReconstructor(LexemeIdentity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.NIL, new Nil.Reconstructor());
        s.addReconstructor(LexemeIdentity.VALID_TIME, new ValidTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.CANCELLATION, new Cancellation.Reconstructor());
        s.addReconstructor(LexemeIdentity.SURFACE_WIND, new SurfaceWind.Reconstructor());
        s.addReconstructor(LexemeIdentity.CAVOK, new CAVOK.Reconstructor());
        s.addReconstructor(LexemeIdentity.HORIZONTAL_VISIBILITY, new MetricHorizontalVisibility.Reconstructor());
        s.addReconstructor(LexemeIdentity.WEATHER, new Weather.Reconstructor(false));
        s.addReconstructor(LexemeIdentity.NO_SIGNIFICANT_WEATHER, new NoSignificantWeather.Reconstructor());
        s.addReconstructor(LexemeIdentity.CLOUD, new CloudLayer.Reconstructor());
        s.addReconstructor(LexemeIdentity.MAX_TEMPERATURE, new ForecastMaxMinTemperature.MaxReconstructor());
        s.addReconstructor(LexemeIdentity.MIN_TEMPERATURE, new ForecastMaxMinTemperature.MinReconstructor());
        s.addReconstructor(LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR, new TAFForecastChangeIndicator.Reconstructor());
        s.addReconstructor(LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP, new TAFChangeForecastTimeGroup.Reconstructor());
        s.addReconstructor(LexemeIdentity.REMARKS_START, new RemarkStart.Reconstructor());
        s.addReconstructor(LexemeIdentity.REMARK, new Remark.Reconstructor());
        s.addReconstructor(LexemeIdentity.END_TOKEN, new EndToken.Reconstructor());
        return s;
    }

    private void addCommonBulletinReconstructors(final AbstractTACSerializer<?> s) {
        s.setLexingFactory(lexingFactory);
        s.addReconstructor(LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS, new BulletinHeaderDataDesignators.Reconstructor());
        s.addReconstructor(LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, new BulletinLocationIndicator.Reconstructor());
        s.addReconstructor(LexemeIdentity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR, new BulletinHeadingBBBIndicator.Reconstructor());
    }

}
