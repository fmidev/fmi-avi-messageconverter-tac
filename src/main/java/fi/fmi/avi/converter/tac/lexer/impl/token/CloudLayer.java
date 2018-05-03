package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COVER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser.CloudAmount;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Token parser for clouds
 */
public class CloudLayer extends RegexMatchingLexemeVisitor {

    public enum CloudCover {
		SKY_CLEAR("SKC"),
		NO_LOW_CLOUDS("CLR"),
		NO_CLOUD_DETECTED("NCD"),
		NO_SIG_CLOUDS("NSC"),
		FEW("FEW"),
		SCATTERED("SCT"),
		BROKEN("BKN"),
		OVERCAST("OVC"),
		SKY_OBSCURED("VV");

        private final String code;

        CloudCover(final String code) {
            this.code = code;
        }

        public static CloudCover forCode(final String code) {
            for (CloudCover w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }

    public enum CloudType {
        TOWERING_CUMULUS("TCU"), CUMULONIMBUS("CB"), MISSING("///");

        private final String code;

        CloudType(final String code) {
            this.code = code;
        }

        public static CloudType forCode(final String code) {
            for (CloudType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

        public String getCode() {
        	return this.code;
		}

    }
    
    public enum SpecialValue {
		AMOUNT_AND_HEIGHT_UNOBSERVABLE_BY_AUTO_SYSTEM, CLOUD_BASE_BELOW_AERODROME
	}

    public CloudLayer(final Priority prio) {
		super("^(([A-Z]{3}|VV)([0-9]{3}|/{3})(CB|TCU|/{3})?)|(/{6})|(SKC|NSC|NCD|CLR)$", prio);
	}

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (match.group(5) != null) {
        	token.identify(CLOUD);
        	//Amount And Height Unobservable By Auto System
        	token.setParsedValue(ParsedValueName.VALUE, SpecialValue.AMOUNT_AND_HEIGHT_UNOBSERVABLE_BY_AUTO_SYSTEM);
        
        } else { 
	    	CloudCover cloudCover;
	    	if (match.group(6) != null) {
	    		cloudCover = CloudCover.forCode(match.group(6));
	    	} else {
	    		cloudCover = CloudCover.forCode(match.group(2));
	    	}
	        if (cloudCover != null) {
	            token.identify(Lexeme.Identity.CLOUD);
	            token.setParsedValue(COVER, cloudCover);
	        } else {
	            token.identify(CLOUD, Lexeme.Status.SYNTAX_ERROR, "Unknown cloud cover " + match.group(2));
	        }
	        if (match.group(3) != null) {
	            if ("///".equals(match.group(3))) {
	                token.setParsedValue(VALUE, SpecialValue.CLOUD_BASE_BELOW_AERODROME);
	            } else {
	                token.setParsedValue(VALUE, Integer.parseInt(match.group(3)));
	                token.setParsedValue(UNIT, "hft");
	            }
	        }
            if (match.group(4) != null) {
	    		CloudType type = CloudType.forCode(match.group(4));
	    		if (CloudType.MISSING == type && hints != null && hints.containsValue(ConversionHints.VALUE_PARSING_MODE_STRICT)) {
					token.identify(CLOUD, Lexeme.Status.SYNTAX_ERROR, "Cloud token may only be postfixed with 'TCU' or 'CB', not '" + CloudType.MISSING.getCode());
				}
	            token.setParsedValue(TYPE, CloudType.forCode(match.group(4)));
            }
        }
    }
    
    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ConversionHints hints,
                final Object... specifier)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            Optional<fi.fmi.avi.model.CloudLayer> layer = getAs(specifier, 0, fi.fmi.avi.model.CloudLayer.class);
            Optional<String> specialValue = getAs(specifier, 0, String.class);

            Optional<NumericMeasure> verVis = null;
            boolean nsc = false;
            boolean cloudsUnobservable = false;
            
            if (TAF.class.isAssignableFrom(clz)) {
                Optional<CloudForecast> cFct = Optional.empty();
                Optional<TAFBaseForecast> baseFct = getAs(specifier, 1, TAFBaseForecast.class);
                if (baseFct.isPresent()) {
                    cFct = baseFct.get().getCloud();
                } else {
                    Optional<TAFChangeForecast> changeFct = getAs(specifier, 1, TAFChangeForecast.class);
                    if (changeFct.isPresent()) {
                        cFct = changeFct.get().getCloud();
                    }
                }
                if (cFct.isPresent()) {
                    if (specialValue.isPresent() && "VV".equals(specialValue.get())) {
                        verVis = cFct.get().getVerticalVisibility();
                    } else if (cFct.get().isNoSignificantCloud()) {
                        nsc = true;
                    }
                }
            } else if (METAR.class.isAssignableFrom(clz)) {
            	METAR metar = (METAR)msg;
                Optional<ObservedClouds> obsClouds = metar.getClouds();
                if (obsClouds.isPresent() && obsClouds.get().isNoSignificantCloud()) {
                    nsc = true;
            	}
                if (obsClouds.isPresent() && obsClouds.get().isAmountAndHeightUnobservableByAutoSystem()) {
                    cloudsUnobservable = true;
				}
                Optional<TrendForecast> trend = getAs(specifier, TrendForecast.class);
                if (trend.isPresent() && trend.get().getCloud().isPresent()) {
                    fi.fmi.avi.model.CloudForecast cloud = trend.get().getCloud().get();
                    if (specialValue.isPresent() && "VV".equals(specialValue.get())) {
                        verVis = cloud.getVerticalVisibility();
                    } else if (cloud.isNoSignificantCloud()) {
                        nsc = true;
            		}
            	} else {
                    if (metar.getClouds().isPresent() && specialValue.isPresent() && "VV".equals(specialValue.get())) {
                        verVis = metar.getClouds().get().getVerticalVisibility();
                    }
            	}
            }
            if (cloudsUnobservable) {
                retval = Optional.of(this.createLexeme("//////", Identity.CLOUD));
            } else if (nsc) {
                retval = Optional.of(this.createLexeme("NSC", Identity.CLOUD));
            } else {
                if (layer.isPresent()) {
                    retval = Optional.of(this.createLexeme(getCloundLayerToken(layer.get()), Identity.CLOUD));
                } else if (verVis.isPresent()) {
                    retval = Optional.of(this.createLexeme(getVerticalVisibilityToken(verVis.get()), Identity.CLOUD));
                }
            }
            return retval;
        }

        private String getCloundLayerToken(final fi.fmi.avi.model.CloudLayer layer) throws SerializingException {
            StringBuilder sb = new StringBuilder();
            Optional<NumericMeasure> base = layer.getBase();
            Optional<fi.fmi.avi.model.AviationCodeListUser.CloudType> type = layer.getCloudType();

            CloudAmount amount = layer.getAmount();
            sb.append(amount.name());
            if (CloudAmount.SKC != amount) {
                if (!base.isPresent()) {
                    sb.append("///");
                } else {
                    sb.append(String.format("%03d", getAsHectoFeet(base.get())));
                }
                type.ifPresent(sb::append);
            }
            return sb.toString();
        }

        private String getVerticalVisibilityToken(final NumericMeasure verVis) throws SerializingException {
            StringBuilder sb = new StringBuilder();
            sb.append("VV");
            sb.append(String.format("%03d", getAsHectoFeet(verVis)));
            return sb.toString();
        }

        private long getAsHectoFeet(final NumericMeasure value) throws SerializingException {
            long hftValue = -1L;
            if (value != null) {
				if ("hft".equalsIgnoreCase(value.getUom())) {
					hftValue = Math.round(value.getValue());
				} else if ("[ft_i]".equalsIgnoreCase(value.getUom())) {
					hftValue = Math.round(value.getValue() / 100.0);
				} else {
                    throw new SerializingException("Unable to reconstruct cloud layer / vertical visibility height with UoM '" + value.getUom() + "'");
                }
        	} else {
                throw new SerializingException("Unable to reconstruct cloud layer / vertical visibility height with null value");
            }
			return hftValue;
        }
    }
}
