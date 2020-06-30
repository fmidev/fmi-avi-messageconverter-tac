package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.tac.swx.SWXTACSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.json.conf.JSONConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

@Configuration
@Import({TACConverter.class, JSONConverter.class })
public class TACTestConfiguration {
    
    @Autowired
    private AviMessageSpecificConverter<String, METAR> metarTACParser;

    @Autowired
    private AviMessageSpecificConverter<String, METARImpl> immutableMetarTACParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafTACParser;

    @Autowired
    private AviMessageSpecificConverter<String, TAFImpl> immutableTafTACParser;

    @Autowired
    private AviMessageSpecificConverter<String, SPECI> speciTACParser;

    @Autowired
    private AviMessageSpecificConverter<METAR, String> metarTACSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafTACSerializer;

    @Autowired
    private AviMessageSpecificConverter<SPECI, String> speciTACSerializer;

    @Autowired
    private AviMessageSpecificConverter<TAFBulletin, String> tafBulletinTACSerializer;

    @Autowired
    private AviMessageSpecificConverter<SIGMETBulletin, String> sigmetBulletinTACSerializer;

    @Autowired
    private AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser;

    @Autowired
    private AviMessageSpecificConverter<GenericMeteorologicalBulletin, String> genericBulletinTACSerializer;

    @Autowired
    @Qualifier("genericBulletinJSONSerializer")
    private AviMessageSpecificConverter<GenericMeteorologicalBulletin, String> genericBulletinJSONSerializer;

    @Autowired
    private AviMessageSpecificConverter<String, SpaceWeatherAdvisory> swxTACParser;

    //@Autowired
    //private AviMessageSpecificConverter<SpaceWeatherAdvisory, String> swxTACSerializer;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        final AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(TACConverter.TAC_TO_METAR_POJO, metarTACParser);
        p.setMessageSpecificConverter(TACConverter.TAC_TO_IMMUTABLE_METAR_POJO, immutableMetarTACParser);
        p.setMessageSpecificConverter(TACConverter.METAR_POJO_TO_TAC, metarTACSerializer);

        p.setMessageSpecificConverter(TACConverter.TAC_TO_TAF_POJO, tafTACParser);
        p.setMessageSpecificConverter(TACConverter.TAC_TO_IMMUTABLE_TAF_POJO, immutableTafTACParser);
        p.setMessageSpecificConverter(TACConverter.TAF_POJO_TO_TAC, tafTACSerializer);

        p.setMessageSpecificConverter(TACConverter.TAC_TO_SPECI_POJO, speciTACParser);
        p.setMessageSpecificConverter(TACConverter.SPECI_POJO_TO_TAC, speciTACSerializer);

        p.setMessageSpecificConverter(TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, genericBulletinTACParser);
        p.setMessageSpecificConverter(TACConverter.TAF_BULLETIN_POJO_TO_TAC, tafBulletinTACSerializer);
        p.setMessageSpecificConverter(TACConverter.SIGMET_BULLETIN_POJO_TO_TAC, sigmetBulletinTACSerializer);
        p.setMessageSpecificConverter(TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, genericBulletinTACSerializer);

        p.setMessageSpecificConverter(JSONConverter.GENERIC_METEOROLOGICAL_BULLETIN_POJO_TO_JSON_STRING, genericBulletinJSONSerializer);

        p.setMessageSpecificConverter(TACConverter.TAC_TO_SWX_POJO, swxTACParser);
        //p.setMessageSpecificConverter(TACConverter.SWX_POJO_TO_TAC, swxTACSerializer);

        return p;
    }
  
}
