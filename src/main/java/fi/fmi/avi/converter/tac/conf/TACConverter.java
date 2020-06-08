package fi.fmi.avi.converter.tac.conf;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.SPECI;
import fi.fmi.avi.model.metar.immutable.METARImpl;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

/**
 * TAC converter Spring configuration
 */
@Configuration
@Import({Parsing.class, Serializing.class})

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
     * Pre-configured spec for WMO GTS text bulletin format to {@link GenericMeteorologicalBulletin} POJO.
     */
    public static final ConversionSpecification<String, GenericMeteorologicalBulletin> TAC_TO_GENERIC_BULLETIN_POJO = new ConversionSpecification<>(String.class,
            GenericMeteorologicalBulletin.class,
            "WMO GTS bulletin", null);

    /**
     * Pre-configured spec for {@link GenericMeteorologicalBulletin} POJO to WMO GTS text bulletin format.
     */
    public static final ConversionSpecification<GenericMeteorologicalBulletin, String> GENERIC_BULLETIN_POJO_TO_TAC = new ConversionSpecification<>(
            GenericMeteorologicalBulletin.class, String.class, null, "WMO GTS bulletin");

    /**
     * Pre-configured spec for ICAO Annex 3 TAC format to {@link fi.fmi.avi.model.swx.SpaceWeatherAdvisory} POJO.
     */
    public static final ConversionSpecification<String, SpaceWeatherAdvisory> TAC_TO_SWX_POJO = new ConversionSpecification<>(String.class,
            SpaceWeatherAdvisory.class, "ICAO Annex 3 TAC", null);

}
