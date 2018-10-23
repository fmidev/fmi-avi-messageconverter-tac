package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.RECENT_WEATHER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.REMARKS_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;
import static fi.fmi.avi.model.immutable.WeatherImpl.WEATHER_CODES;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

/**
 * Token parser for weather codes
 */
public class Weather extends RegexMatchingLexemeVisitor {

    private final static Set<String> weatherSkipWords = new HashSet<>(
            Arrays.asList("METAR", "RTD", "TAF", "COR", "AMD", "CNL", "NIL", "CAVOK", "TEMPO", "BECMG", "RMK", "NOSIG", "NSC", "NSW", "SKC", "NCD", "AUTO",
                    "SNOCLO", "BLU", "WHT", "GRN", "YLO1", "YLO2", "AMB", "RED", "BLACKWHT", "BLACKBLU", "BLACKGRN", "BLACKYLO1", "BLACKYLO2", "BLACKAMB",
                    "BLACKRED"));

    public Weather(final Priority prio) {
        super("^(RE)?([+-]?[A-Z]{2,8})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        boolean isPreceededWithAerodromeCode = false;
        boolean isPreceededWithRemarkStart = false;
        Lexeme.Identity[] messageStartTokens = { Lexeme.Identity.METAR_START, Lexeme.Identity.TAF_START, Lexeme.Identity.SPECI_START };
        Lexeme l = token.getPrevious();
        boolean startTokenFound = false;
        while (l != null) {
            for (Lexeme.Identity id : messageStartTokens) {
                if (id == l.getIdentityIfAcceptable()) {
                    startTokenFound = true;
                }
            }
            if (startTokenFound) {
                l = null;
            } else {
                if (!isPreceededWithAerodromeCode && AERODROME_DESIGNATOR == l.getIdentityIfAcceptable()) {
                    isPreceededWithAerodromeCode = true;
                }
                if (!isPreceededWithRemarkStart && REMARKS_START == l.getIdentityIfAcceptable()) {
                    isPreceededWithRemarkStart = true;
                }
                l = l.getPrevious();
            }

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
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<fi.fmi.avi.model.Weather> weather = ctx.getParameter("weather", fi.fmi.avi.model.Weather.class);
            if (weather.isPresent() && isCodeAllowed(weather.get(), ctx.getHints())) {
                if (recentWeather) {
                    return Optional.of(this.createLexeme("RE" + weather.get().getCode(), Lexeme.Identity.RECENT_WEATHER));
                } else {
                    return Optional.of(this.createLexeme(weather.get().getCode(), Lexeme.Identity.WEATHER));
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
            } else {
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
