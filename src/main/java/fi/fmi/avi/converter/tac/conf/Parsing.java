package fi.fmi.avi.converter.tac.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.TACParser;
import fi.fmi.avi.converter.tac.bulletin.GenericMeteorologicalBulletinParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.metar.ImmutableMETARTACParser;
import fi.fmi.avi.converter.tac.metar.METARTACParser;
import fi.fmi.avi.converter.tac.metar.SPECITACParser;
import fi.fmi.avi.converter.tac.taf.ImmutableTAFTACParser;
import fi.fmi.avi.converter.tac.taf.TAFTACParser;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

/**
 * TAC converter parsing Spring configuration
 */
@Configuration
@Import(Lexing.class)
public class Parsing {

    @Autowired
    private AviMessageLexer aviMessageLexer;

    @Bean
    AviMessageSpecificConverter<String, METAR> metarTACParser() {
        final TACParser<METAR> p = new METARTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, METARImpl> immutableMetarTACParser() {
        final TACParser<METARImpl> p = new ImmutableMETARTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, SPECI> speciTACParser() {
        final TACParser<SPECI> p = new SPECITACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, TAF> tafTACParser() {
        final TACParser<TAF> p = new TAFTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, TAFImpl> immutableTafTACParser() {
        final TACParser<TAFImpl> p = new ImmutableTAFTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser() {
        final TACParser<GenericMeteorologicalBulletin> p = new GenericMeteorologicalBulletinParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

}
