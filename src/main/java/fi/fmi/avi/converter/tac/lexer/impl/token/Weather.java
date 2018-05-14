package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.RECENT_WEATHER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.REMARKS_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Token parser for weather codes
 */
public class Weather extends RegexMatchingLexemeVisitor {

    private final static Set<String> weatherSkipWords = new HashSet<>(
            Arrays.asList("METAR", "RTD", "TAF", "COR", "AMD", "CNL", "NIL", "CAVOK", "TEMPO", "BECMG", "RMK", "NOSIG", "NSC", "NSW", "SKC", "NCD", "AUTO",
                    "SNOCLO", "BLU", "WHT", "GRN", "YLO1", "YLO2", "AMB", "RED", "BLACKWHT", "BLACKBLU", "BLACKGRN", "BLACKYLO1", "BLACKYLO2", "BLACKAMB",
                    "BLACKRED"));
    public final static Map<String, String> WEATHER_CODES;

    //Copied from https://codes.wmo.int/306/4678
    static {
    	Map<String, String> _WEATHER_CODES = new HashMap<>();
        _WEATHER_CODES.put("+DZ", "Heavy precipitation of drizzle");
        _WEATHER_CODES.put("+DZPL", "Heavy precipitation of drizzle and ice pellets");
        _WEATHER_CODES.put("+DZPLRA", "Heavy precipitation of drizzle, ice pellets and rain");
        _WEATHER_CODES.put("+DZRA", "Heavy precipitation of drizzle and rain");
        _WEATHER_CODES.put("+DZRAPL", "Heavy precipitation of drizzle, rain and ice pellets");
        _WEATHER_CODES.put("+DZRASG", "Heavy precipitation of drizzle, rain and snow grains");
        _WEATHER_CODES.put("+DZRASN", "Heavy precipitation of drizzle, rain and snow");
        _WEATHER_CODES.put("+DZSG", "Heavy precipitation of drizzle and snow grains");
        _WEATHER_CODES.put("+DZSGRA", "Heavy precipitation of drizzle, snow grains and rain");
        _WEATHER_CODES.put("+DZSN", "Heavy precipitation of drizzle and snow");
        _WEATHER_CODES.put("+DZSNRA", "Heavy precipitation of drizzle, snow and rain");
        _WEATHER_CODES.put("+FZDZ", "Heavy precipitation of freezing drizzle");
        _WEATHER_CODES.put("+FZDZRA", "Heavy precipitation of freezing drizzle and rain");
        _WEATHER_CODES.put("+FZRA", "Heavy precipitation of freezng rain");
        _WEATHER_CODES.put("+FZRADZ", "Heavy precipitation of freezing rain and drizzle");
        _WEATHER_CODES.put("+FZUP", "Heavy unidentified freezing precipitation");
        _WEATHER_CODES.put("+PL", "Heavy precipitation of ice pellets");
        _WEATHER_CODES.put("+PLDZ", "Heavy precipitation of ice pellets and drizzle");
        _WEATHER_CODES.put("+PLDZRA", "Heavy precipitation of ice pellets, drizzle and rain");
        _WEATHER_CODES.put("+PLRA", "Heavy precipitation of ice pellets and rain");
        _WEATHER_CODES.put("+PLRADZ", "Heavy precipitation of ice pellets, rain and drizzle");
        _WEATHER_CODES.put("+PLRASN", "Heavy precipitation of ice pellets, rain and snow");
        _WEATHER_CODES.put("+PLSG", "Heavy precipitation of ice pellets and snow grains");
        _WEATHER_CODES.put("+PLSGSN", "Heavy precipitation of ice pellets, snow grains and snow");
        _WEATHER_CODES.put("+PLSN", "Heavy precipitation of ice pellets and snow");
        _WEATHER_CODES.put("+PLSNRA", "Heavy precipitation of ice pellets, snow and rain");
        _WEATHER_CODES.put("+PLSNSG", "Heavy precipitation of ice pellets, snow and snow grains");
        _WEATHER_CODES.put("+RA", "Heavy precipitation of rain");
        _WEATHER_CODES.put("+RADZ", "Heavy precipitation of rain and drizzle");
        _WEATHER_CODES.put("+RADZPL", "Heavy precipitation of rain, drizzle and ice pellets");
        _WEATHER_CODES.put("+RADZSG", "Heavy precipitation of rain, drizzle and snow grains");
        _WEATHER_CODES.put("+RADZSN", "Heavy precipitation of rain, drizzle and snow");
        _WEATHER_CODES.put("+RAPL", "Heavy precipitation of rain and ice pellets");
        _WEATHER_CODES.put("+RAPLDZ", "Heavy precipitation of rain, ice pellets and drizzle");
        _WEATHER_CODES.put("+RAPLSN", "Heavy precipitation of rain, ice pellets and snow");
        _WEATHER_CODES.put("+RASG", "Heavy precipitation of rain and snow grains");
        _WEATHER_CODES.put("+RASGDZ", "Heavy precipitation of rain, snow grains and drizzle");
        _WEATHER_CODES.put("+RASGSN", "Heavy precipitation of rain, snow grains and snow");
        _WEATHER_CODES.put("+RASN", "Heavy precipitation of rain and snow");
        _WEATHER_CODES.put("+RASNDZ", "Heavy precipitation of rain, snow and drizzle");
        _WEATHER_CODES.put("+RASNPL", "Heavy precipitation of rain, snow and ice pellets");
        _WEATHER_CODES.put("+RASNSG", "Heavy precipitation of rain, snow and snow grains");
        _WEATHER_CODES.put("+SG", "Heavy precipitation of snow grains");
        _WEATHER_CODES.put("+SGDZ", "Heavy precipitation of snow grains and drizzle");
        _WEATHER_CODES.put("+SGDZRA", "Heavy precipitation of snow grains, drizzle and rain");
        _WEATHER_CODES.put("+SGPL", "Heavy precipitation of snow grains and ice pellets");
        _WEATHER_CODES.put("+SGPLSN", "Heavy precipitation of snow grains, ice pellets and snow");
        _WEATHER_CODES.put("+SGRA", "Heavy precipitation of snow grains and rain");
        _WEATHER_CODES.put("+SGRADZ", "Heavy precipitation of snow grains, rain and drizzle");
        _WEATHER_CODES.put("+SGRASN", "Heavy precipitation of snow grains, rain and snow");
        _WEATHER_CODES.put("+SGSN", "Heavy precipitation of snow grains and snow");
        _WEATHER_CODES.put("+SGSNPL", "Heavy precipitation of snow grains, snow and ice pellets");
        _WEATHER_CODES.put("+SGSNRA", "Heavy precipitation of snow grains, snow and rain");
        _WEATHER_CODES.put("+SHGR", "Heavy showery precipitation of hail");
        _WEATHER_CODES.put("+SHGRRA", "Heavy showery precipitation of hail and rain");
        _WEATHER_CODES.put("+SHGRRASN", "Heavy showery precipitation of hail, rain and snow");
        _WEATHER_CODES.put("+SHGRSN", "Heavy showery precipitation of hail and snow");
        _WEATHER_CODES.put("+SHGRSNRA", "Heavy showery precipitation of hail, snow and rain");
        _WEATHER_CODES.put("+SHGS", "Heavy showery precipitation of snow pellets/small hail");
        _WEATHER_CODES.put("+SHGSRA", "Heavy showery precipitation of snow pellets/small hail and rain");
        _WEATHER_CODES.put("+SHGSRASN", "Heavy showery precipitation of snow pellets/small hail, rain and snow");
        _WEATHER_CODES.put("+SHGSSN", "Heavy showery precipitation of snow pellets/small hail and snow");
        _WEATHER_CODES.put("+SHGSSNRA", "Heavy showery precipitation of snow pellets/small hail, snow and rain");
        _WEATHER_CODES.put("+SHRA", "Heavy showery precipitation of rain");
        _WEATHER_CODES.put("+SHRAGR", "Heavy showery precipitation of rain and hail");
        _WEATHER_CODES.put("+SHRAGRSN", "Heavy showery precipitation of rain, hail and snow");
        _WEATHER_CODES.put("+SHRAGS", "Heavy showery precipitation of rain and snow pellets/small hail");
        _WEATHER_CODES.put("+SHRAGSSN", "Heavy showery precipitation of rain, snow pellets/small hail and snow");
        _WEATHER_CODES.put("+SHRASN", "Heavy showery precipitation of rain and snow");
        _WEATHER_CODES.put("+SHRASNGR", "Heavy showery precipitation of rain, snow and hail");
        _WEATHER_CODES.put("+SHRASNGS", "Heavy showery precipitation of rain, snow and snow pellets/small hail");
        _WEATHER_CODES.put("+SHSN", "Heavy showery precipitation of snow");
        _WEATHER_CODES.put("+SHSNGR", "Heavy showery precipitation of snow and hail");
        _WEATHER_CODES.put("+SHSNGRRA", "Heavy showery precipitation of snow, hail and rain");
        _WEATHER_CODES.put("+SHSNGS", "Heavy showery precipitation of snow and snow pellets/small hail");
        _WEATHER_CODES.put("+SHSNGSRA", "Heavy showery precipitation of snow, snow pellets/small hail and rain");
        _WEATHER_CODES.put("+SHSNRA", "Heavy showery precipitation of snow and rain");
        _WEATHER_CODES.put("+SHSNRAGR", "Heavy showery precipitation of snow, rain and hail");
        _WEATHER_CODES.put("+SHSNRAGS", "Heavy showery precipitation of snow, rain and snow pellets/small hail");
        _WEATHER_CODES.put("+SHUP", "Heavy unidentified showery precipitation");
        _WEATHER_CODES.put("+SN", "Heavy precipitation of snow");
        _WEATHER_CODES.put("+SNDZ", "Heavy precipitation of snow and drizzle");
        _WEATHER_CODES.put("+SNDZRA", "Heavy precipitation of snow, drizzle and rain");
        _WEATHER_CODES.put("+SNPL", "Heavy precipitation of snow and ice pellets");
        _WEATHER_CODES.put("+SNPLRA", "Heavy precipitation of snow, ice pellets and rain");
        _WEATHER_CODES.put("+SNPLSG", "Heavy precipitation of snow, ice pellets and snow grains");
        _WEATHER_CODES.put("+SNRA", "Heavy precipitation of snow and rain");
        _WEATHER_CODES.put("+SNRADZ", "Heavy precipitation of snow, rain and drizzle");
        _WEATHER_CODES.put("+SNRAPL", "Heavy precipitation of snow, rain and ice pellets");
        _WEATHER_CODES.put("+SNRASG", "Heavy precipitation of snow, rain and snow grains");
        _WEATHER_CODES.put("+SNSG", "Heavy precipitation of snow and snow grains");
        _WEATHER_CODES.put("+SNSGPL", "Heavy precipitation of snow, snow grains and ice pellets");
        _WEATHER_CODES.put("+SNSGRA", "Heavy precipitation of snow, snow grains and rain");
        _WEATHER_CODES.put("+SS", "Heavy sandstorm");
        _WEATHER_CODES.put("+TSGR", "Thunderstorm with heavy precipitation of hail");
        _WEATHER_CODES.put("+TSGRRA", "Thunderstorm with heavy precipitation of hail and rain");
        _WEATHER_CODES.put("+TSGRRASN", "Thunderstorm with heavy precipitation of hail, rain and snow");
        _WEATHER_CODES.put("+TSGRSN", "Thunderstorm with heavy precipitation of hail and snow");
        _WEATHER_CODES.put("+TSGRSNRA", "Thunderstorm with heavy precipitation of hail, snow and rain");
        _WEATHER_CODES.put("+TSGS", "Thunderstorm with heavy precipitation of snow pellets/small hail");
        _WEATHER_CODES.put("+TSGSRA", "Thunderstorm with heavy precipitation of snow pellets/small hail and rain");
        _WEATHER_CODES.put("+TSGSRASN", "Thunderstorm with heavy precipitation of snow pellets/small hail, rain and snow");
        _WEATHER_CODES.put("+TSGSSN", "Thunderstorm with heavy precipitation of snow pellets/small hail and snow");
        _WEATHER_CODES.put("+TSGSSNRA", "Thunderstorm with heavy precipitation of snow pellets/small hail, snow and rain");
        _WEATHER_CODES.put("+TSRA", "Thunderstorm with heavy precipitation of rain");
        _WEATHER_CODES.put("+TSRAGR", "Thunderstorm with heavy precipitation of rain and hail");
        _WEATHER_CODES.put("+TSRAGRSN", "Thunderstorm with heavy precipitation of rain, hail and snow");
        _WEATHER_CODES.put("+TSRAGS", "Thunderstorm with heavy precipitation of rain and snow pellets/small hail");
        _WEATHER_CODES.put("+TSRAGSSN", "Thunderstorm with heavy precipitation of rain, snow pellets/small hail and snow");
        _WEATHER_CODES.put("+TSRASN", "Thunderstorm with heavy precipitation of rain and snow");
        _WEATHER_CODES.put("+TSRASNGR", "Thunderstorm with heavy precipitation of rain, snow and hail");
        _WEATHER_CODES.put("+TSRASNGS", "Thunderstorm with heavy precipitation of rain, snow and snow pellets/small hail");
        _WEATHER_CODES.put("+TSSN", "Thunderstorm with heavy precipitation of snow");
        _WEATHER_CODES.put("+TSSNGR", "Thunderstorm with heavy precipitation of snow and hail");
        _WEATHER_CODES.put("+TSSNGRRA", "Thunderstorm with heavy precipitation of snow, hail and rain");
        _WEATHER_CODES.put("+TSSNGS", "Thunderstorm with heavy precipitation of snow and snow pellets/small hail");
        _WEATHER_CODES.put("+TSSNGSRA", "Thunderstorm with heavy precipitation of snow, snow pellets/small hail and rain");
        _WEATHER_CODES.put("+TSSNRA", "Thunderstorm with heavy precipitation of snow and rain");
        _WEATHER_CODES.put("+TSSNRAGR", "Thunderstorm with heavy precipitation of snow, rain and hail");
        _WEATHER_CODES.put("+TSSNRAGS", "Thunderstorm with heavy precipitation of snow, rain and snow pellets/small hail");
        _WEATHER_CODES.put("+TSUP", "Thunderstorm with heavy precipitation of unidentified precipitation");
        _WEATHER_CODES.put("+UP", "Heavy unidentified precipitation");
        _WEATHER_CODES.put("-DS", "Light duststorm");
        _WEATHER_CODES.put("-DZ", "Light precipitation of drizzle");
        _WEATHER_CODES.put("-DZPL", "Light precipitation of drizzle and ice pellets");
        _WEATHER_CODES.put("-DZPLRA", "Light precipitation of drizzle, ice pellets and rain");
        _WEATHER_CODES.put("-DZRA", "Light precipitation of drizzle and rain");
        _WEATHER_CODES.put("-DZRAPL", "Light precipitation of drizzle, rain and ice pellets");
        _WEATHER_CODES.put("-DZRASG", "Light precipitation of drizzle, rain and snow grains");
        _WEATHER_CODES.put("-DZRASN", "Light precipitation of drizzle, rain and snow");
        _WEATHER_CODES.put("-DZSG", "Light precipitation of drizzle and snow grains");
        _WEATHER_CODES.put("-DZSGRA", "Light precipitation of drizzle, snow grains and rain");
        _WEATHER_CODES.put("-DZSN", "Light precipitation of drizzle and snow");
        _WEATHER_CODES.put("-DZSNRA", "Light precipitation of drizzle, snow and rain");
        _WEATHER_CODES.put("-FZDZ", "Light precipitation of freezing drizzle");
        _WEATHER_CODES.put("-FZDZRA", "Light precipitation of freezing drizzle and rain");
        _WEATHER_CODES.put("-FZRA", "Light precipitation of freezng rain");
        _WEATHER_CODES.put("-FZRADZ", "Light precipitation of freezing rain and drizzle");
        _WEATHER_CODES.put("-FZUP", "Light unidentified freezing precipitation");
        _WEATHER_CODES.put("-PL", "Light precipitation of ice pellets");
        _WEATHER_CODES.put("-PLDZ", "Light precipitation of ice pellets and drizzle");
        _WEATHER_CODES.put("-PLDZRA", "Light precipitation of ice pellets, drizzle and rain");
        _WEATHER_CODES.put("-PLRA", "Light precipitation of ice pellets and rain");
        _WEATHER_CODES.put("-PLRADZ", "Light precipitation of ice pellets, rain and drizzle");
        _WEATHER_CODES.put("-PLRASN", "Light precipitation of ice pellets, rain and snow");
        _WEATHER_CODES.put("-PLSG", "Light precipitation of ice pellets and snow grains");
        _WEATHER_CODES.put("-PLSGSN", "Light precipitation of ice pellets, snow grains and snow");
        _WEATHER_CODES.put("-PLSN", "Light precipitation of ice pellets and snow");
        _WEATHER_CODES.put("-PLSNRA", "Light precipitation of ice pellets, snow and rain");
        _WEATHER_CODES.put("-PLSNSG", "Light precipitation of ice pellets, snow and snow grains");
        _WEATHER_CODES.put("-RA", "Light precipitation of rain");
        _WEATHER_CODES.put("-RADZ", "Light precipitation of rain and drizzle");
        _WEATHER_CODES.put("-RADZPL", "Light precipitation of rain, drizzle and ice pellets");
        _WEATHER_CODES.put("-RADZSG", "Light precipitation of rain, drizzle and snow grains");
        _WEATHER_CODES.put("-RADZSN", "Light precipitation of rain, drizzle and snow");
        _WEATHER_CODES.put("-RAPL", "Light precipitation of rain and ice pellets");
        _WEATHER_CODES.put("-RAPLDZ", "Light precipitation of rain, ice pellets and drizzle");
        _WEATHER_CODES.put("-RAPLSN", "Light precipitation of rain, ice pellets and snow");
        _WEATHER_CODES.put("-RASG", "Light precipitation of rain and snow grains");
        _WEATHER_CODES.put("-RASGDZ", "Light precipitation of rain, snow grains and drizzle");
        _WEATHER_CODES.put("-RASGSN", "Light precipitation of rain, snow grains and snow");
        _WEATHER_CODES.put("-RASN", "Light precipitation of rain and snow");
        _WEATHER_CODES.put("-RASNDZ", "Light precipitation of rain, snow and drizzle");
        _WEATHER_CODES.put("-RASNPL", "Light precipitation of rain, snow and ice pellets");
        _WEATHER_CODES.put("-RASNSG", "Light precipitation of rain, snow and snow grains");
        _WEATHER_CODES.put("-SG", "Light precipitation of snow grains");
        _WEATHER_CODES.put("-SGDZ", "Light precipitation of snow grains and drizzle");
        _WEATHER_CODES.put("-SGDZRA", "Light precipitation of snow grains, drizzle and rain");
        _WEATHER_CODES.put("-SGPL", "Light precipitation of snow grains and ice pellets");
        _WEATHER_CODES.put("-SGPLSN", "Light precipitation of snow grains, ice pellets and snow");
        _WEATHER_CODES.put("-SGRA", "Light precipitation of snow grains and rain");
        _WEATHER_CODES.put("-SGRADZ", "Light precipitation of snow grains, rain and drizzle");
        _WEATHER_CODES.put("-SGRASN", "Light precipitation of snow grains, rain and snow");
        _WEATHER_CODES.put("-SGSN", "Light precipitation of snow grains and snow");
        _WEATHER_CODES.put("-SGSNPL", "Light precipitation of snow grains, snow and ice pellets");
        _WEATHER_CODES.put("-SGSNRA", "Light precipitation of snow grains, snow and rain");
        _WEATHER_CODES.put("-SHGR", "Light showery precipitation of hail");
        _WEATHER_CODES.put("-SHGRRA", "Light showery precipitation of hail and rain");
        _WEATHER_CODES.put("-SHGRRASN", "Light showery precipitation of hail, rain and snow");
        _WEATHER_CODES.put("-SHGRSN", "Light showery precipitation of hail and snow");
        _WEATHER_CODES.put("-SHGRSNRA", "Light showery precipitation of hail, snow and rain");
        _WEATHER_CODES.put("-SHGS", "Light showery precipitation of snow pellets/small hail");
        _WEATHER_CODES.put("-SHGSRA", "Light showery precipitation of snow pellets/small hail and rain");
        _WEATHER_CODES.put("-SHGSRASN", "Light showery precipitation of snow pellets/small hail, rain and snow");
        _WEATHER_CODES.put("-SHGSSN", "Light showery precipitation of snow pellets/small hail and snow");
        _WEATHER_CODES.put("-SHGSSNRA", "Light showery precipitation of snow pellets/small hail, snow and rain");
        _WEATHER_CODES.put("-SHRA", "Light showery precipitation of rain");
        _WEATHER_CODES.put("-SHRAGR", "Light showery precipitation of rain and hail");
        _WEATHER_CODES.put("-SHRAGRSN", "Light showery precipitation of rain, hail and snow");
        _WEATHER_CODES.put("-SHRAGS", "Light showery precipitation of rain and snow pellets/small hail");
        _WEATHER_CODES.put("-SHRAGSSN", "Light showery precipitation of rain, snow pellets/small hail and snow");
        _WEATHER_CODES.put("-SHRASN", "Light showery precipitation of rain and snow");
        _WEATHER_CODES.put("-SHRASNGR", "Light showery precipitation of rain, snow and hail");
        _WEATHER_CODES.put("-SHRASNGS", "Light showery precipitation of rain, snow and snow pellets/small hail");
        _WEATHER_CODES.put("-SHSN", "Light showery precipitation of snow");
        _WEATHER_CODES.put("-SHSNGR", "Light showery precipitation of snow and hail");
        _WEATHER_CODES.put("-SHSNGRRA", "Light showery precipitation of snow, hail and rain");
        _WEATHER_CODES.put("-SHSNGS", "Light showery precipitation of snow and snow pellets/small hail");
        _WEATHER_CODES.put("-SHSNGSRA", "Light showery precipitation of snow, snow pellets/small hail and rain");
        _WEATHER_CODES.put("-SHSNRA", "Light showery precipitation of snow and rain");
        _WEATHER_CODES.put("-SHSNRAGR", "Light showery precipitation of snow, rain and hail");
        _WEATHER_CODES.put("-SHSNRAGS", "Light showery precipitation of snow, rain and snow pellets/small hail");
        _WEATHER_CODES.put("-SHUP", "Light unidentified showery precipitation");
        _WEATHER_CODES.put("-SN", "Light precipitation of snow");
        _WEATHER_CODES.put("-SNDZ", "Light precipitation of snow and drizzle");
        _WEATHER_CODES.put("-SNDZRA", "Light precipitation of snow, drizzle and rain");
        _WEATHER_CODES.put("-SNPL", "Light precipitation of snow and ice pellets");
        _WEATHER_CODES.put("-SNPLRA", "Light precipitation of snow, ice pellets and rain");
        _WEATHER_CODES.put("-SNPLSG", "Light precipitation of snow, ice pellets and snow grains");
        _WEATHER_CODES.put("-SNRA", "Light precipitation of snow and rain");
        _WEATHER_CODES.put("-SNRADZ", "Light precipitation of snow, rain and drizzle");
        _WEATHER_CODES.put("-SNRAPL", "Light precipitation of snow, rain and ice pellets");
        _WEATHER_CODES.put("-SNRASG", "Light precipitation of snow, rain and snow grains");
        _WEATHER_CODES.put("-SNSG", "Light precipitation of snow and snow grains");
        _WEATHER_CODES.put("-SNSGPL", "Light precipitation of snow, snow grains and ice pellets");
        _WEATHER_CODES.put("-SNSGRA", "Light precipitation of snow, snow grains and rain");
        _WEATHER_CODES.put("-SS", "Light sandstorm");
        _WEATHER_CODES.put("-TSGR", "Thunderstorm with light precipitation of hail");
        _WEATHER_CODES.put("-TSGRRA", "Thunderstorm with light precipitation of hail and rain");
        _WEATHER_CODES.put("-TSGRRASN", "Thunderstorm with light precipitation of hail, rain and snow");
        _WEATHER_CODES.put("-TSGRSN", "Thunderstorm with light precipitation of hail and snow");
        _WEATHER_CODES.put("-TSGRSNRA", "Thunderstorm with light precipitation of hail, snow and rain");
        _WEATHER_CODES.put("-TSGS", "Thunderstorm with light precipitation of snow pellets/small hail");
        _WEATHER_CODES.put("-TSGSRA", "Thunderstorm with light precipitation of snow pellets/small hail and rain");
        _WEATHER_CODES.put("-TSGSRASN", "Thunderstorm with light precipitation of snow pellets/small hail, rain and snow");
        _WEATHER_CODES.put("-TSGSSN", "Thunderstorm with light precipitation of snow pellets/small hail and snow");
        _WEATHER_CODES.put("-TSGSSNRA", "Thunderstorm with light precipitation of snow pellets/small hail, snow and rain");
        _WEATHER_CODES.put("-TSRA", "Thunderstorm with light precipitation of rain");
        _WEATHER_CODES.put("-TSRAGR", "Thunderstorm with light precipitation of rain and hail");
        _WEATHER_CODES.put("-TSRAGRSN", "Thunderstorm with light precipitation of rain, hail and snow");
        _WEATHER_CODES.put("-TSRAGS", "Thunderstorm with light precipitation of rain and snow pellets/small hail");
        _WEATHER_CODES.put("-TSRAGSSN", "Thunderstorm with light precipitation of rain, snow pellets/small hail and snow");
        _WEATHER_CODES.put("-TSRASN", "Thunderstorm with light precipitation of rain and snow");
        _WEATHER_CODES.put("-TSRASNGR", "Thunderstorm with light precipitation of rain, snow and hail");
        _WEATHER_CODES.put("-TSRASNGS", "Thunderstorm with light precipitation of rain, snow and snow pellets/small hail");
        _WEATHER_CODES.put("-TSSN", "Thunderstorm with light precipitation of snow");
        _WEATHER_CODES.put("-TSSNGR", "Thunderstorm with light precipitation of snow and hail");
        _WEATHER_CODES.put("-TSSNGRRA", "Thunderstorm with light precipitation of snow, hail and rain");
        _WEATHER_CODES.put("-TSSNGS", "Thunderstorm with light precipitation of snow and snow pellets/small hail");
        _WEATHER_CODES.put("-TSSNGSRA", "Thunderstorm with light precipitation of snow, snow pellets/small hail and rain");
        _WEATHER_CODES.put("-TSSNRA", "Thunderstorm with light precipitation of snow and rain");
        _WEATHER_CODES.put("-TSSNRAGR", "Thunderstorm with light precipitation of snow, rain and hail");
        _WEATHER_CODES.put("-TSSNRAGS", "Thunderstorm with light precipitation of snow, rain and snow pellets/small hail");
        _WEATHER_CODES.put("-TSUP", "Thunderstorm with light unidentified precipitation");
        _WEATHER_CODES.put("-UP", "Light unidentified precipitation");
        _WEATHER_CODES.put("BCFG", "Patches of fog");
        _WEATHER_CODES.put("BLDU", "Blowing dust");
        _WEATHER_CODES.put("BLSA", "Blowing sand");
        _WEATHER_CODES.put("BLSN", "Blowing snow");
        _WEATHER_CODES.put("BR", "Mist");
        _WEATHER_CODES.put("DRDU", "Low drifting dust");
        _WEATHER_CODES.put("DRSA", "Low drifting sand");
        _WEATHER_CODES.put("DRSN", "Low drifting snow");
        _WEATHER_CODES.put("DS", "Duststorm");
        _WEATHER_CODES.put("DU", "Dust");
        _WEATHER_CODES.put("DZ", "Precipitation of drizzle");
        _WEATHER_CODES.put("DZPL", "Precipitation of drizzle and ice pellets");
        _WEATHER_CODES.put("DZPLRA", "Precipitation of drizzle, ice pellets and rain");
        _WEATHER_CODES.put("DZRA", "Precipitation of drizzle and rain");
        _WEATHER_CODES.put("DZRAPL", "Precipitation of drizzle, rain and ice pellets");
        _WEATHER_CODES.put("DZRASG", "Precipitation of drizzle, rain and snow grains");
        _WEATHER_CODES.put("DZRASN", "Precipitation of drizzle, rain and snow");
        _WEATHER_CODES.put("DZSG", "Precipitation of drizzle and snow grains");
        _WEATHER_CODES.put("DZSGRA", "Precipitation of drizzle, snow grains and rain");
        _WEATHER_CODES.put("DZSN", "Precipitation of drizzle and snow");
        _WEATHER_CODES.put("DZSNRA", "Precipitation of drizzle, snow and rain");
        _WEATHER_CODES.put("FC", "Funnel cloud(s) (tornado or water-spout)");
        _WEATHER_CODES.put("FG", "Fog");
        _WEATHER_CODES.put("FU", "Smoke");
        _WEATHER_CODES.put("FZDZ", "Precipitation of freezing drizzle");
        _WEATHER_CODES.put("FZDZRA", "Precipitation of freezing drizzle and rain");
        _WEATHER_CODES.put("FZFG", "Freezing fog");
        _WEATHER_CODES.put("FZRA", "Precipitation of freezing rain");
        _WEATHER_CODES.put("FZRADZ", "Precipitation of freezing rain and drizzle");
        _WEATHER_CODES.put("FZUP", "Unidentified freezing precipitation");
        _WEATHER_CODES.put("HZ", "Haze");
        _WEATHER_CODES.put("MIFG", "Shallow fog");
        _WEATHER_CODES.put("PL", "Precipitation of ice pellets");
        _WEATHER_CODES.put("PLDZ", "Precipitation of ice pellets and drizzle");
        _WEATHER_CODES.put("PLDZRA", "Precipitation of ice pellets, drizzle and rain");
        _WEATHER_CODES.put("PLRA", "Precipitation of ice pellets and rain");
        _WEATHER_CODES.put("PLRADZ", "Precipitation of ice pellets, rain and drizzle");
        _WEATHER_CODES.put("PLRASN", "Precipitation of ice pellets, rain and snow");
        _WEATHER_CODES.put("PLSG", "Precipitation of ice pellets and snow grains");
        _WEATHER_CODES.put("PLSGSN", "Precipitation of ice pellets, snow grains and snow");
        _WEATHER_CODES.put("PLSN", "Precipitation of ice pellets and snow");
        _WEATHER_CODES.put("PLSNRA", "Precipitation of ice pellets, snow and rain");
        _WEATHER_CODES.put("PLSNSG", "Precipitation of ice pellets, snow and snow grains");
        _WEATHER_CODES.put("PO", "Dust/sand whirls (dust devils)");
        _WEATHER_CODES.put("PRFG", "Partial fog (covering part of the aerodrome)");
        _WEATHER_CODES.put("RA", "Precipitation of rain");
        _WEATHER_CODES.put("RADZ", "Precipitation of rain and drizzle");
        _WEATHER_CODES.put("RADZPL", "Precipitation of rain, drizzle and ice pellets");
        _WEATHER_CODES.put("RADZSG", "Precipitation of rain, drizzle and snow grains");
        _WEATHER_CODES.put("RADZSN", "Precipitation of rain, drizzle and snow");
        _WEATHER_CODES.put("RAPL", "Precipitation of rain and ice pellets");
        _WEATHER_CODES.put("RAPLDZ", "Precipitation of rain, ice pellets and drizzle");
        _WEATHER_CODES.put("RAPLSN", "Precipitation of rain, ice pellets and snow");
        _WEATHER_CODES.put("RASG", "Precipitation of rain and snow grains");
        _WEATHER_CODES.put("RASGDZ", "Precipitation of rain, snow grains and drizzle");
        _WEATHER_CODES.put("RASGSN", "Precipitation of rain, snow grains and snow");
        _WEATHER_CODES.put("RASN", "Precipitation of rain and snow");
        _WEATHER_CODES.put("RASNDZ", "Precipitation of rain, snow and drizzle");
        _WEATHER_CODES.put("RASNPL", "Precipitation of rain, snow and ice pellets");
        _WEATHER_CODES.put("RASNSG", "Precipitation of rain, snow and snow grains");
        _WEATHER_CODES.put("SA", "Sand");
        _WEATHER_CODES.put("SG", "Precipitation of snow grains");
        _WEATHER_CODES.put("SGDZ", "Precipitation of snow grains and drizzle");
        _WEATHER_CODES.put("SGDZRA", "Precipitation of snow grains, drizzle and rain");
        _WEATHER_CODES.put("SGPL", "Precipitation of snow grains and ice pellets");
        _WEATHER_CODES.put("SGPLSN", "Precipitation of snow grains, ice pellets and snow");
        _WEATHER_CODES.put("SGRA", "Precipitation of snow grains and rain");
        _WEATHER_CODES.put("SGRADZ", "Precipitation of snow grains, rain and drizzle");
        _WEATHER_CODES.put("SGRASN", "Precipitation of snow grains, rain and snow");
        _WEATHER_CODES.put("SGSN", "Precipitation of snow grains and snow");
        _WEATHER_CODES.put("SGSNPL", "Precipitation of snow grains, snow and ice pellets");
        _WEATHER_CODES.put("SGSNRA", "Precipitation of snow grains, snow and rain");
        _WEATHER_CODES.put("SHGR", "Showery precipitation of hail");
        _WEATHER_CODES.put("SHGRRA", "Showery precipitation of hail and rain");
        _WEATHER_CODES.put("SHGRRASN", "Showery precipitation of hail, rain and snow");
        _WEATHER_CODES.put("SHGRSN", "Showery precipitation of hail and snow");
        _WEATHER_CODES.put("SHGRSNRA", "Showery precipitation of hail, snow and rain");
        _WEATHER_CODES.put("SHGS", "Showery precipitation of snow pellets/small hail");
        _WEATHER_CODES.put("SHGSRA", "Showery precipitation of snow pellets/small hail and rain");
        _WEATHER_CODES.put("SHGSRASN", "Showery precipitation of snow pellets/small hail, rain and snow");
        _WEATHER_CODES.put("SHGSSN", "Showery precipitation of snow pellets/small hail and snow");
        _WEATHER_CODES.put("SHGSSNRA", "Showery precipitation of snow pellets/small hail, snow and rain");
        _WEATHER_CODES.put("SHRA", "Showery precipitation of rain");
        _WEATHER_CODES.put("SHRAGR", "Showery precipitation of rain and hail");
        _WEATHER_CODES.put("SHRAGRSN", "Showery precipitation of rain, hail and snow");
        _WEATHER_CODES.put("SHRAGS", "Showery precipitation of rain and snow pellets/small hail");
        _WEATHER_CODES.put("SHRAGSSN", "Showery precipitation of rain, snow pellets/small hail and snow");
        _WEATHER_CODES.put("SHRASN", "Showery precipitation of rain and snow");
        _WEATHER_CODES.put("SHRASNGR", "Showery precipitation of rain, snow and hail");
        _WEATHER_CODES.put("SHRASNGS", "Showery precipitation of rain, snow and snow pellets/small hail");
        _WEATHER_CODES.put("SHSN", "Showery precipitation of snow");
        _WEATHER_CODES.put("SHSNGR", "Showery precipitation of snow and hail");
        _WEATHER_CODES.put("SHSNGRRA", "Showery precipitation of snow, hail and rain");
        _WEATHER_CODES.put("SHSNGS", "Showery precipitation of snow and snow pellets/small hail");
        _WEATHER_CODES.put("SHSNGSRA", "Showery precipitation of snow, snow pellets/small hail and rain");
        _WEATHER_CODES.put("SHSNRA", "Showery precipitation of snow and rain");
        _WEATHER_CODES.put("SHSNRAGR", "Showery precipitation of snow, rain and hail");
        _WEATHER_CODES.put("SHSNRAGS", "Showery precipitation of snow, rain and snow pellets/small hail");
        _WEATHER_CODES.put("SHUP", "Unidentified showery precipitation|Showery precipitation of unidentified precipitation");
        _WEATHER_CODES.put("SN", "Precipitation of snow");
        _WEATHER_CODES.put("SNDZ", "Precipitation of snow and drizzle");
        _WEATHER_CODES.put("SNDZRA", "Precipitation of snow, drizzle and rain");
        _WEATHER_CODES.put("SNPL", "Precipitation of snow and ice pellets");
        _WEATHER_CODES.put("SNPLRA", "Precipitation of snow, ice pellets and rain");
        _WEATHER_CODES.put("SNPLSG", "Precipitation of snow, ice pellets and snow grains");
        _WEATHER_CODES.put("SNRA", "Precipitation of snow and rain");
        _WEATHER_CODES.put("SNRADZ", "Precipitation of snow, rain and drizzle");
        _WEATHER_CODES.put("SNRAPL", "Precipitation of snow, rain and ice pellets");
        _WEATHER_CODES.put("SNRASG", "Precipitation of snow, rain and snow grains");
        _WEATHER_CODES.put("SNSG", "Precipitation of snow and snow grains");
        _WEATHER_CODES.put("SNSGPL", "Precipitation of snow, snow grains and ice pellets");
        _WEATHER_CODES.put("SNSGRA", "Precipitation of snow, snow grains and rain");
        _WEATHER_CODES.put("SQ", "Squalls");
        _WEATHER_CODES.put("SS", "Sandstorm");
        _WEATHER_CODES.put("TS", "Thunderstorm");
        _WEATHER_CODES.put("TSGR", "Thunderstorm with precipitation of hail");
        _WEATHER_CODES.put("TSGRRA", "Thunderstorm with precipitation of hail and rain");
        _WEATHER_CODES.put("TSGRRASN", "Thunderstorm with precipitation of hail, rain and snow");
        _WEATHER_CODES.put("TSGRSN", "Thunderstorm with precipitation of hail and snow");
        _WEATHER_CODES.put("TSGRSNRA", "Thunderstorm with precipitation of hail, snow and rain");
        _WEATHER_CODES.put("TSGS", "Thunderstorm with precipitation of snow pellets/small hail");
        _WEATHER_CODES.put("TSGSRA", "Thunderstorm with precipitation of snow pellets/small hail and rain");
        _WEATHER_CODES.put("TSGSRASN", "Thunderstorm with precipitation of snow pellets/small hail, rain and snow");
        _WEATHER_CODES.put("TSGSSN", "Thunderstorm with precipitation of snow pellets/small hail and snow");
        _WEATHER_CODES.put("TSGSSNRA", "Thunderstorm with precipitation of snow pellets/small hail, snow and rain");
        _WEATHER_CODES.put("TSRA", "Thunderstorm with precipitation of rain");
        _WEATHER_CODES.put("TSRAGR", "Thunderstorm with precipitation of rain and hail");
        _WEATHER_CODES.put("TSRAGRSN", "Thunderstorm with precipitation of rain, hail and snow");
        _WEATHER_CODES.put("TSRAGS", "Thunderstorm with precipitation of rain and snow pellets/small hail");
        _WEATHER_CODES.put("TSRAGSSN", "Thunderstorm with precipitation of rain, snow pellets/small hail and snow");
        _WEATHER_CODES.put("TSRASN", "Thunderstorm with precipitation of rain and snow");
        _WEATHER_CODES.put("TSRASNGR", "Thunderstorm with precipitation of rain, snow and hail");
        _WEATHER_CODES.put("TSRASNGS", "Thunderstorm with precipitation of rain, snow and snow pellets/small hail");
        _WEATHER_CODES.put("TSSN", "Thunderstorm with precipitation of snow");
        _WEATHER_CODES.put("TSSNGR", "Thunderstorm with precipitation of snow and hail");
        _WEATHER_CODES.put("TSSNGRRA", "Thunderstorm with precipitation of snow, hail and rain");
        _WEATHER_CODES.put("TSSNGS", "Thunderstorm with precipitation of snow and snow pellets/small hail");
        _WEATHER_CODES.put("TSSNGSRA", "Thunderstorm with precipitation of snow, snow pellets/small hail and rain");
        _WEATHER_CODES.put("TSSNRA", "Thunderstorm with precipitation of snow and rain");
        _WEATHER_CODES.put("TSSNRAGR", "Thunderstorm with precipitation of snow, rain and hail");
        _WEATHER_CODES.put("TSSNRAGS", "Thunderstorm with precipitation of snow, rain and snow pellets/small hail");
        _WEATHER_CODES.put("TSUP", "Thunderstorm with unidentified precipitation");
        _WEATHER_CODES.put("UP", "Unidentified precipitation");
        _WEATHER_CODES.put("VA", "Volcanic ash");
        _WEATHER_CODES.put("VCBLDU", "Blowing dust in the vicinity");
        _WEATHER_CODES.put("VCBLSA", "Blowing sand in the vicinity");
        _WEATHER_CODES.put("VCBLSN", "Blowing snow in the vicinity");
        _WEATHER_CODES.put("VCDS", "Duststorm in the vicinity");
        _WEATHER_CODES.put("VCFC", "Funnel cloud(s) (tornado or water-spout) in the vicinity");
        _WEATHER_CODES.put("VCFG", "Fog in the vicinity");
        _WEATHER_CODES.put("VCPO", "Dust/sand whirls (dust devils) in the vicinity");
        _WEATHER_CODES.put("VCSH", "Shower(s) in the vicinity");
        _WEATHER_CODES.put("VCSS", "Sandstorm in the vicinity");
        _WEATHER_CODES.put("VCTS", "Thunderstorm in the vicinity");
        _WEATHER_CODES.put("VCVA", "Volcanic ash in the vicinity");
        WEATHER_CODES = Collections.unmodifiableMap(_WEATHER_CODES);
    }

    public Weather(final Priority prio) {
        super("^(RE)?([+-]?[A-Z]{2,8})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        boolean isPreceededWithAerodromeCode = false;
        boolean isPreceededWithRemarkStart = false;
        Lexeme l = token.getPrevious();
        while (l != null) {
            if (!isPreceededWithAerodromeCode && AERODROME_DESIGNATOR == l.getIdentityIfAcceptable()) {
                isPreceededWithAerodromeCode = true;
            }
            if (!isPreceededWithRemarkStart && REMARKS_START == l.getIdentityIfAcceptable()) {
                isPreceededWithRemarkStart = true;
            }
            l = l.getPrevious();
        }
        if (isPreceededWithRemarkStart) {
            return;
        }
        if (isPreceededWithAerodromeCode) {
            boolean isRecent = match.group(1) != null;
            String code = match.group(2);

            if (!weatherSkipWords.contains(code)) {
                if (hints == null || hints.isEmpty() || !hints.containsKey(ConversionHints.KEY_WEATHER_CODES)
                        || ConversionHints.VALUE_WEATHER_CODES_STRICT_WMO_4678.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    // Only the official codes allowed by default
                    if (WEATHER_CODES.containsKey(code)) {
                        token.identify(isRecent ? RECENT_WEATHER : WEATHER);
                        token.setParsedValue(Lexeme.ParsedValueName.VALUE, code);
                    } else {
                        token.identify(isRecent ? RECENT_WEATHER : WEATHER, Lexeme.Status.SYNTAX_ERROR, "Unknown weather code " + code);
                    }
                } else {
                    if (ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                        token.identify(isRecent ? RECENT_WEATHER : WEATHER);
                        token.setParsedValue(Lexeme.ParsedValueName.VALUE, code);
                    } else if (ConversionHints.VALUE_WEATHER_CODES_IGNORE_NON_WMO_4678.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                        if (WEATHER_CODES.containsKey(code)) {
                            token.identify(isRecent ? RECENT_WEATHER : WEATHER);
                            token.setParsedValue(Lexeme.ParsedValueName.VALUE, code);
                        } else {
                            token.setIgnored(true);
                            if (token.hasNext(true) && Lexeme.Identity.WHITE_SPACE == token.getNext(true).getIdentity()) {
                                token.getNext(true).setIgnored(true);
                            }
                        }
                    }
                }
            }
        }
    }
    
    public static class Reconstructor extends FactoryBasedReconstructor {
    	private final boolean recentWeather;
    	
    	public Reconstructor(boolean recentWeather) {
    		this.recentWeather = recentWeather;
		}
    	
        @Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ConversionHints hints,
                final Object... specifier)
                throws SerializingException {
            Optional<fi.fmi.avi.model.Weather> weather;
            if (TAF.class.isAssignableFrom(clz)) {
                weather = getAs(specifier, 1, fi.fmi.avi.model.Weather.class);
                if (weather.isPresent() && isCodeAllowed(weather.get(), hints)) {
                    Optional<TAFBaseForecast> baseFct = getAs(specifier, 0, TAFBaseForecast.class);
                    Optional<TAFChangeForecast> changeFct = getAs(specifier, 0, TAFChangeForecast.class);
                    if ((baseFct.isPresent() || changeFct.isPresent())) {
                        return Optional.of(this.createLexeme(weather.get().getCode(), Lexeme.Identity.WEATHER));
                    }
                }
            } else if (METAR.class.isAssignableFrom(clz)) {
                weather = getAs(specifier, 0, fi.fmi.avi.model.Weather.class);
                if (weather.isPresent() && isCodeAllowed(weather.get(), hints)) {
                    if (recentWeather) {
                        return Optional.of(this.createLexeme("RE" + weather.get().getCode(), Lexeme.Identity.RECENT_WEATHER));
                    } else {
                        return Optional.of(this.createLexeme(weather.get().getCode(), Lexeme.Identity.WEATHER));
                    }
                }
            }
            return Optional.empty();
        }

        private boolean isCodeAllowed(final fi.fmi.avi.model.Weather weather, final ConversionHints hints) throws SerializingException {
            boolean retval = false;
            if (weather == null) {
                return false;
            }
            if (hints == null || hints.isEmpty() || !hints.containsKey(ConversionHints.KEY_WEATHER_CODES) || ConversionHints.VALUE_WEATHER_CODES_STRICT_WMO_4678
                    .equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                // Only the official codes allowed by default
                if (WEATHER_CODES.containsKey(weather.getCode())) {
                    retval = true;
                } else {
                    throw new SerializingException("Illegal weather code " + weather.getCode());
                }
            } else if (hints != null) {
                if (ConversionHints.VALUE_WEATHER_CODES_ALLOW_ANY.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    retval = true;
                } else if (ConversionHints.VALUE_WEATHER_CODES_IGNORE_NON_WMO_4678.equals(hints.get(ConversionHints.KEY_WEATHER_CODES))) {
                    if (WEATHER_CODES.containsKey(weather.getCode())) {
                        retval = true;
                    }
                }
            }
            return retval;
        }
    }
}
