package fi.fmi.avi.converter.tac;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

@Configuration
@Import(TACConverter.class)
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
    private AviMessageSpecificConverter<LexemeSequence, TAF> tafLexemeSequenceParser;

    @Autowired
    private AviMessageSpecificConverter<LexemeSequence, TAFImpl> immutableTafLexemeSequenceParser;

    @Bean
    public AviMessageConverter aviMessageConverter() {
        AviMessageConverter p = new AviMessageConverter();
        p.setMessageSpecificConverter(TACConverter.TAC_TO_METAR_POJO, metarTACParser);
        p.setMessageSpecificConverter(TACConverter.TAC_TO_IMMUTABLE_METAR_POJO, immutableMetarTACParser);
        p.setMessageSpecificConverter(TACConverter.METAR_POJO_TO_TAC, metarTACSerializer);

        p.setMessageSpecificConverter(TACConverter.TAC_TO_TAF_POJO, tafTACParser);
        p.setMessageSpecificConverter(TACConverter.TAC_TO_IMMUTABLE_TAF_POJO, immutableTafTACParser);
        p.setMessageSpecificConverter(TACConverter.TAF_POJO_TO_TAC, tafTACSerializer);

        p.setMessageSpecificConverter(TACConverter.TAC_TO_SPECI_POJO, speciTACParser);
        p.setMessageSpecificConverter(TACConverter.SPECI_POJO_TO_TAC, speciTACSerializer);

        p.setMessageSpecificConverter(TACConverter.TAF_BULLETIN_POJO_TO_TAC, tafBulletinTACSerializer);

        p.setMessageSpecificConverter(TACConverter.LEXEME_SEQUENCE_TO_TAF_POJO, tafLexemeSequenceParser);
        p.setMessageSpecificConverter(TACConverter.LEXEME_SEQUENCE_TO_IMMUTABLE_TAF_POJO, immutableTafLexemeSequenceParser);
        return p;
    }
  
}
