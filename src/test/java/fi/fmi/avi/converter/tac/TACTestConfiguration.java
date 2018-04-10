package fi.fmi.avi.converter.tac;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.taf.TAF;

@Configuration
@Import(TACConverter.class)
public class TACTestConfiguration {
    
    @Autowired
    private AviMessageSpecificConverter<String, METAR> metarTACParser;
    
    @Autowired
    private AviMessageSpecificConverter<String, TAF> tafTACParser;

    @Autowired
    private AviMessageSpecificConverter<String, SPECI> speciTACParser;
    
    @Autowired
    private AviMessageSpecificConverter<METAR, String> metarTACSerializer;
    
    @Autowired
    private AviMessageSpecificConverter<TAF, String> tafTACSerializer;

    @Autowired
    private AviMessageSpecificConverter<SPECI, String> speciTACSerializer;
    
    @Bean
    public AviMessageConverter aviMessageConverter() {
        AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(TACConverter.TAC_TO_METAR_POJO, metarTACParser);
        p.setMessageSpecificConverter(TACConverter.TAC_TO_TAF_POJO, tafTACParser);
        p.setMessageSpecificConverter(TACConverter.METAR_POJO_TO_TAC, metarTACSerializer);
        p.setMessageSpecificConverter(TACConverter.TAF_POJO_TO_TAC, tafTACSerializer);
        p.setMessageSpecificConverter(TACConverter.TAC_TO_SPECI_POJO, speciTACParser);
        p.setMessageSpecificConverter(TACConverter.SPECI_POJO_TO_TAC, speciTACSerializer);
        return p;
    }
  
}
