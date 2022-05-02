package fi.fmi.avi.converter.tac.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.AviMessageSpecificConverter;
import fi.fmi.avi.converter.tac.TACParser;
import fi.fmi.avi.converter.tac.bulletin.GenericAviationWeatherMessageParser;
import fi.fmi.avi.converter.tac.bulletin.GenericMeteorologicalBulletinParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.metar.ImmutableMETARTACParser;
import fi.fmi.avi.converter.tac.metar.METARTACParser;
import fi.fmi.avi.converter.tac.metar.SPECITACParser;
import fi.fmi.avi.converter.tac.swx.SWXTACParser;
import fi.fmi.avi.converter.tac.taf.ImmutableTAFTACParser;
import fi.fmi.avi.converter.tac.taf.TAFTACParser;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;
import fi.fmi.avi.converter.tac.sigmet.ImmutableSIGMETTACParser;
import fi.fmi.avi.converter.tac.sigmet.SIGMETTACParser;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import fi.fmi.avi.converter.tac.airmet.ImmutableAIRMETTACParser;
import fi.fmi.avi.converter.tac.airmet.AIRMETTACParser;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;

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
    AviMessageSpecificConverter<String, SIGMET> sigmetTACParser() {
        final TACParser<SIGMET> p = new SIGMETTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, SIGMETImpl> immutableSigmetTACParser() {
        final TACParser<SIGMETImpl> p = new ImmutableSIGMETTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, AIRMET> airmetTACParser() {
        final TACParser<AIRMET> p = new AIRMETTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, AIRMETImpl> immutableAirmetTACParser() {
        final TACParser<AIRMETImpl> p = new ImmutableAIRMETTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, GenericMeteorologicalBulletin> genericBulletinTACParser() {
        final TACParser<GenericMeteorologicalBulletin> p = new GenericMeteorologicalBulletinParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, SpaceWeatherAdvisory> swxTACParser() {
        final TACParser<SpaceWeatherAdvisory> p = new SWXTACParser();
        p.setTACLexer(aviMessageLexer);
        return p;
    }

    @Bean
    AviMessageSpecificConverter<String, GenericAviationWeatherMessage> genericAviationWeatherMessageTACParser() {
        return new GenericAviationWeatherMessageParser(aviMessageLexer);
    }

}
