package fi.fmi.avi.converter.tac.conf;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.airmet.AIRMETBulletinTACSerializer;
import fi.fmi.avi.converter.tac.airmet.AIRMETTACSerializer;
import fi.fmi.avi.converter.tac.bulletin.GenericMeteorologicalBulletinTACSerializer;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.AviMessageTACTokenizerImpl;
import fi.fmi.avi.converter.tac.lexer.impl.token.*;
import fi.fmi.avi.converter.tac.metar.METARTACSerializer;
import fi.fmi.avi.converter.tac.metar.SPECITACSerializer;
import fi.fmi.avi.converter.tac.sigmet.SIGMETBulletinTACSerializer;
import fi.fmi.avi.converter.tac.sigmet.SIGMETTACSerializer;
import fi.fmi.avi.converter.tac.swx.amd79.SWXAmd79BulletinTACSerializer;
import fi.fmi.avi.converter.tac.swx.amd79.SWXAmd79TACSerializer;
import fi.fmi.avi.converter.tac.swx.amd82.SWXAmd82BulletinTACSerializer;
import fi.fmi.avi.converter.tac.swx.amd82.SWXAmd82TACSerializer;
import fi.fmi.avi.converter.tac.taf.TAFBulletinTACSerializer;
import fi.fmi.avi.converter.tac.taf.TAFTACSerializer;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.AIRMETBulletin;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAmd79Bulletin;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAmd82Bulletin;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * TAC converter serializing Spring configuration.
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

    @Bean(name = "tafTACSerializer")
    AviMessageSpecificConverter<TAF, String> tafTACSerializer() {
        return spawnTAFTACSerializer();
    }

    @Bean
    AviMessageSpecificConverter<TAFBulletin, String> tafBulletinTACSerializer() {
        final TAFBulletinTACSerializer s = new TAFBulletinTACSerializer();
        addCommonBulletinReconstructors(s);
        s.setTafSerializer(spawnTAFTACSerializer());
        return s;
    }

    @Bean
    AviMessageSpecificConverter<SIGMETBulletin, String> sigmetBulletinTACSerializer() {
        final SIGMETBulletinTACSerializer s = new SIGMETBulletinTACSerializer();
        s.setSigmetSerializer(spawnSIGMETTACSerializer());
        addCommonBulletinReconstructors(s);
        return s;
    }

    @Bean
    @Qualifier("sigmetTACSerializer")
    AviMessageSpecificConverter<SIGMET, String> sigmetTACSerializer() {
        final SIGMETTACSerializer s = spawnSIGMETTACSerializer();
        addCommonBulletinReconstructors(s);
        return s;
    }

    @Bean
    AviMessageSpecificConverter<AIRMETBulletin, String> airmetBulletinTACSerializer() {
        final AIRMETBulletinTACSerializer s = new AIRMETBulletinTACSerializer();
        s.setAirmetSerializer(spawnAIRMETTACSerializer());
        addCommonBulletinReconstructors(s);
        return s;
    }

    @Bean
    @Qualifier("airmetTACSerializer")
    AviMessageSpecificConverter<AIRMET, String> airmetTACSerializer() {
        final AIRMETTACSerializer s = spawnAIRMETTACSerializer();
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
    @Qualifier("swxAmd82Serializer")
    AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd82, String> swxAmd82TACSerializer() {
        return spawnSWXAmd82TACSerializer();
    }

    @Bean
    @Qualifier("swxAmd79Serializer")
    AviMessageSpecificConverter<SpaceWeatherAdvisoryAmd79, String> swxAmd79TACSerializer() {
        return spawnSWXAmd79TACSerializer();
    }

    @Bean
    AviMessageSpecificConverter<SpaceWeatherAmd82Bulletin, String> swxAmd82BulletinTACSerializer() {
        final SWXAmd82BulletinTACSerializer s = new SWXAmd82BulletinTACSerializer();
        addCommonBulletinReconstructors(s);
        s.setSWXSerializer(spawnSWXAmd82TACSerializer());
        return s;
    }

    @Bean
    AviMessageSpecificConverter<SpaceWeatherAmd79Bulletin, String> swxAmd79BulletinTACSerializer() {
        final SWXAmd79BulletinTACSerializer s = new SWXAmd79BulletinTACSerializer();
        addCommonBulletinReconstructors(s);
        s.setSWXSerializer(spawnSWXAmd79TACSerializer());
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
        tokenizer.setAIRMETBulletinSerializer((AIRMETBulletinTACSerializer) airmetBulletinTACSerializer());
        tokenizer.setGenericBulletinSerializer((GenericMeteorologicalBulletinTACSerializer) genericBulletinTACSerializer());
        tokenizer.setSWXAmd82TacSerializer((SWXAmd82TACSerializer) swxAmd82TACSerializer());
        tokenizer.setSWXAmd79TacSerializer((SWXAmd79TACSerializer) swxAmd79TACSerializer());
        tokenizer.setSIGMETTacSerializer((SIGMETTACSerializer) sigmetTACSerializer());
        tokenizer.setAIRMETTacSerializer((AIRMETTACSerializer) airmetTACSerializer());
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

    // Creates an instance of the TAFTACSerializer to be used for two separate bean instances
    // (tafTACSerializer and tafBulletinTACSerializer):
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

    // Creates an instance of the SWXAmd82TACSerializer to be used for two separate bean instances
    // (swxAmd82TACSerializer and swxAmd82BulletinTACSerializer):
    private SWXAmd82TACSerializer spawnSWXAmd82TACSerializer() {
        final SWXAmd82TACSerializer s = new SWXAmd82TACSerializer();
        addSWXCommonReconstructors(s);
        s.addReconstructor(LexemeIdentity.SWX_EFFECT, new SWXEffect.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_INTENSITY, new SWXIntensity.Reconstructor());
        return s;
    }

    // Creates an instance of the SWXAmd79TACSerializer to be used for two separate bean instances
    // (swxAmd79TACSerializer and swxAmd79BulletinTACSerializer):
    private SWXAmd79TACSerializer spawnSWXAmd79TACSerializer() {
        final SWXAmd79TACSerializer s = new SWXAmd79TACSerializer();
        addSWXCommonReconstructors(s);
        s.addReconstructor(LexemeIdentity.SWX_EFFECT_AND_INTENSITY, new SWXEffectAndIntensity.Reconstructor());
        return s;
    }

    private void addSWXCommonReconstructors(final AbstractTACSerializer<? extends AviationWeatherMessage> s) {
        s.setLexingFactory(lexingFactory);
        s.addReconstructor(LexemeIdentity.SPACE_WEATHER_ADVISORY_START, new SWXAdvisoryStart.Reconstructor());
        s.addReconstructor(LexemeIdentity.DTG_ISSUE_TIME_LABEL, new DTGIssueTimeLabel.Reconstructor());
        s.addReconstructor(LexemeIdentity.ISSUE_TIME, new DTGIssueTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.ADVISORY_STATUS_LABEL, new AdvisoryStatusLabel.Reconstructor());
        s.addReconstructor(LexemeIdentity.ADVISORY_STATUS, new AdvisoryStatus.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_CENTRE_LABEL, new SWXCenterLabel.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_CENTRE, new SWXCenter.Reconstructor());
        s.addReconstructor(LexemeIdentity.ADVISORY_NUMBER_LABEL, new AdvisoryNumberLabel.Reconstructor());
        s.addReconstructor(LexemeIdentity.ADVISORY_NUMBER, new AdvisoryNumber.Reconstructor());
        s.addReconstructor(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, new ReplaceAdvisoryNumberLabel.Reconstructor());
        s.addReconstructor(LexemeIdentity.REPLACE_ADVISORY_NUMBER, new ReplaceAdvisoryNumber.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_EFFECT_LABEL, new SWXEffectLabel.Reconstructor());
        s.addReconstructor(LexemeIdentity.ADVISORY_PHENOMENA_LABEL, new SWXPhenomena.Reconstructor());
        s.addReconstructor(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, new AdvisoryPhenomenaTimeGroup.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION, new SWXPresetLocation.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_NOT_EXPECTED, new SWXNotExpected.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_NOT_AVAILABLE, new SWXNotAvailable.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT, new SWXVerticalLimit.Reconstructor());
        s.addReconstructor(LexemeIdentity.POLYGON_COORDINATE_PAIR, new PolygonCoordinatePair.Reconstructor());
        s.addReconstructor(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT, new SWXPhenonmenonLongitudeLimit.Reconstructor());
        s.addReconstructor(LexemeIdentity.REMARKS_START, new AdvisoryRemarkStart.Reconstructor());
        s.addReconstructor(LexemeIdentity.REMARK, new Remark.Reconstructor());
        s.addReconstructor(LexemeIdentity.NEXT_ADVISORY, new NextAdvisory.Reconstructor());
        s.addReconstructor(LexemeIdentity.NEXT_ADVISORY_LABEL, new NextAdvisoryLabel.Reconstructor());
        s.addReconstructor(LexemeIdentity.END_TOKEN, new EndToken.Reconstructor());
    }

    private void addCommonBulletinReconstructors(final AbstractTACSerializer<?> s) {
        s.setLexingFactory(lexingFactory);
        s.addReconstructor(LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS, new BulletinHeaderDataDesignators.Reconstructor());
        s.addReconstructor(LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, new BulletinLocationIndicator.Reconstructor());
        s.addReconstructor(LexemeIdentity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR, new BulletinHeadingBBBIndicator.Reconstructor());
    }

    // Creates an instance of the SIGMETTACSerializer to be used for two separate bean instances
    // (sigmetTACSerializer and sigmetBulletinTACSerializer):
    private SIGMETTACSerializer spawnSIGMETTACSerializer() {
        final SIGMETTACSerializer s = new SIGMETTACSerializer();
        s.setLexingFactory(lexingFactory);
        s.addReconstructor(LexemeIdentity.SIGMET_START, new SigmetStart.Reconstructor());
        s.addReconstructor(LexemeIdentity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.SEQUENCE_DESCRIPTOR, new SigmetSequenceDescriptor.Reconstructor());
        s.addReconstructor(LexemeIdentity.VALID_TIME, new SigmetValidTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.MWO_DESIGNATOR, new MWODesignator.Reconstructor());
        s.addReconstructor(LexemeIdentity.FIR_DESIGNATOR, new FIRDesignator.Reconstructor());
        s.addReconstructor(LexemeIdentity.FIR_NAME, new FIRName.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_USAGE, new SigmetUsage.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_PHENOMENON, new SigmetPhenomenon.Reconstructor());
        s.addReconstructor(LexemeIdentity.OBS_OR_FORECAST, new AirSigmetObsOrForecast.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_FCST_AT, new SigmetForecastAt.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_TAC_ELEMENT, new SigmetTacElement.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_ENTIRE_AREA, new SigmetEntireFir.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_INTENSITY, new SigmetIntensity.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_WITHIN, new SigmetWithin.Reconstructor());
        s.addReconstructor(LexemeIdentity.POLYGON_COORDINATE_PAIR, new PolygonCoordinatePair.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_LEVEL, new SigmetLevel.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_MOVING, new SigmetMoving.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_AND, new SigmetAnd.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_CANCEL, new SigmetCancel.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_VA_ERUPTION, new SigmetVaEruption.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_VA_NAME, new SigmetVaName.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_VA_POSITION, new SigmetVaPosition.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_NO_VA_EXP, new SigmetNoVaExp.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT, new SigmetWithinRadius.Reconstructor());

        s.addReconstructor(LexemeIdentity.END_TOKEN, new EndToken.Reconstructor());
        return s;
    }

    // Creates an instance of the AIRMETTACSerializer to be used for two separate bean instances
    // (airmetTACSerializer and airmetBulletinTACSerializer):
    private AIRMETTACSerializer spawnAIRMETTACSerializer() {
        final AIRMETTACSerializer s = new AIRMETTACSerializer();
        s.setLexingFactory(lexingFactory);
        s.addReconstructor(LexemeIdentity.AIRMET_START, new AirmetStart.Reconstructor());
        s.addReconstructor(LexemeIdentity.ISSUE_TIME, new IssueTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.SEQUENCE_DESCRIPTOR, new SigmetSequenceDescriptor.Reconstructor());
        s.addReconstructor(LexemeIdentity.VALID_TIME, new SigmetValidTime.Reconstructor());
        s.addReconstructor(LexemeIdentity.MWO_DESIGNATOR, new MWODesignator.Reconstructor());
        s.addReconstructor(LexemeIdentity.FIR_DESIGNATOR, new FIRDesignator.Reconstructor());
        s.addReconstructor(LexemeIdentity.FIR_NAME, new FIRName.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_USAGE, new SigmetUsage.Reconstructor());
        s.addReconstructor(LexemeIdentity.AIRMET_PHENOMENON, new AirmetPhenomenon.Reconstructor());
        s.addReconstructor(LexemeIdentity.OBS_OR_FORECAST, new AirSigmetObsOrForecast.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_TAC_ELEMENT, new SigmetTacElement.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_ENTIRE_AREA, new SigmetEntireFir.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_INTENSITY, new SigmetIntensity.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_WITHIN, new SigmetWithin.Reconstructor());
        s.addReconstructor(LexemeIdentity.POLYGON_COORDINATE_PAIR, new PolygonCoordinatePair.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_LEVEL, new SigmetLevel.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_MOVING, new SigmetMoving.Reconstructor());
        s.addReconstructor(LexemeIdentity.SIGMET_AND, new SigmetAnd.Reconstructor());
        s.addReconstructor(LexemeIdentity.AIRMET_CANCEL, new AirmetCancel.Reconstructor());
        s.addReconstructor(LexemeIdentity.END_TOKEN, new EndToken.Reconstructor());
        return s;
    }
}
