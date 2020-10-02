package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COVER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CLOUD;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser.CloudAmount;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.ObservedCloudLayer;
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
        TOWERING_CUMULUS("TCU"), CUMULONIMBUS("CB");

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
        AMOUNT_AND_HEIGHT_UNOBSERVABLE_BY_AUTO_SYSTEM, CLOUD_BASE_UNOBSERVABLE, CLOUD_AMOUNT_UNOBSERVABLE, CLOUD_TYPE_UNOBSERVABLE
    }

    public CloudLayer(final OccurrenceFrequency prio) {
        super("^(?<iscloud>(?<amount>[A-Z]{3}|VV|/{3})(?<height>[0-9]{3}|/{3})(?<type>CB|TCU|/{3})?)|(?<nocloud>SKC|NSC|NCD|CLR)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if ("///".equals(match.group("amount")) && "///".equals(match.group("height"))) {
            if ("///".equals(match.group("type"))) {
                token.identify(LexemeIdentity.CLOUD, Lexeme.Status.SYNTAX_ERROR, "Cloud type cannot be missing '///' if also amount and height are missing");
            } else {
                token.identify(LexemeIdentity.CLOUD);
                token.setParsedValue(VALUE, SpecialValue.AMOUNT_AND_HEIGHT_UNOBSERVABLE_BY_AUTO_SYSTEM);
                token.setParsedValue(TYPE, CloudType.forCode(match.group("type")));
            }
        } else {
            if (match.group("amount") != null) {
                if ("///".equals(match.group("amount"))) {
                    token.identify(LexemeIdentity.CLOUD);
                    token.setParsedValue(COVER, SpecialValue.CLOUD_AMOUNT_UNOBSERVABLE);
                } else {
                    CloudCover cloudCover = CloudCover.forCode(match.group("amount"));
                    if (cloudCover != null) {
                        token.identify(LexemeIdentity.CLOUD);
                        token.setParsedValue(COVER, cloudCover);
                    } else {
                        token.identify(CLOUD, Lexeme.Status.SYNTAX_ERROR, "Unknown cloud cover " + match.group("amount"));
                    }
                }
            } else if (match.group("nocloud") != null) {
                CloudCover cloudCover = CloudCover.forCode(match.group("nocloud"));
                token.identify(LexemeIdentity.CLOUD);
                token.setParsedValue(COVER, cloudCover);
            }

            if (match.group("height") != null) {
                if ("///".equals(match.group("height"))) {
                    token.setParsedValue(VALUE, SpecialValue.CLOUD_BASE_UNOBSERVABLE);
                } else {
                    token.setParsedValue(VALUE, Integer.parseInt(match.group("height")));
                    token.setParsedValue(UNIT, "hft");
                }
            }

            if (match.group("type") != null) {
                if ("///".equals(match.group("type"))) {
                    token.setParsedValue(TYPE, SpecialValue.CLOUD_TYPE_UNOBSERVABLE);
                } else {
                    CloudType type = CloudType.forCode(match.group("type"));
                    if (CloudCover.SKY_OBSCURED.code.equals(match.group("amount")) && (CloudType.CUMULONIMBUS == type || CloudType.TOWERING_CUMULUS == type)) {
                        token.identify(CLOUD, Lexeme.Status.SYNTAX_ERROR, "'CB' and 'TCU' not allowed with 'VV'");
                    }
                    token.setParsedValue(TYPE, type);
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            Optional<fi.fmi.avi.model.CloudLayer> layer = ctx.getParameter("layer", fi.fmi.avi.model.CloudLayer.class);

            Optional<NumericMeasure> verVis = Optional.empty();
            boolean noSignificantClouds = false;
            boolean noCloudsDetected = false;
            boolean verticalVisibilityNotObservable = false;

            if (TAF.class.isAssignableFrom(clz)) {
                Optional<CloudForecast> cFct = Optional.empty();
                Optional<TAFBaseForecast> baseFct = ctx.getParameter("forecast", TAFBaseForecast.class);
                if (baseFct.isPresent()) {
                    cFct = baseFct.get().getCloud();
                } else {
                    Optional<TAFChangeForecast> changeFct = ctx.getParameter("forecast", TAFChangeForecast.class);
                    if (changeFct.isPresent()) {
                        cFct = changeFct.get().getCloud();
                    }
                }
                if (cFct.isPresent()) {
                    Optional<Boolean> verticalVisibility = ctx.getParameter("verticalVisibility", Boolean.class);
                    if (verticalVisibility.isPresent() && verticalVisibility.get()) {
                        verVis = cFct.get().getVerticalVisibility();
                    } else if (cFct.get().isNoSignificantCloud()) {
                        noSignificantClouds = true;
                    }
                }
            } else if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                Optional<TrendForecast> trend = ctx.getParameter("trend", TrendForecast.class);
                Optional<Boolean> verticalVisibility = ctx.getParameter("verticalVisibility", Boolean.class);
                if (trend.isPresent()) {
                    if (trend.get().getCloud().isPresent()) {
                        fi.fmi.avi.model.CloudForecast cloud = trend.get().getCloud().get();
                        if (verticalVisibility.isPresent() && verticalVisibility.get()) {
                            verVis = cloud.getVerticalVisibility();
                        } else if (cloud.isNoSignificantCloud()) {
                            noSignificantClouds = true;
                        }
                    }
                } else {

                    MeteorologicalTerminalAirReport metar = (MeteorologicalTerminalAirReport) msg;
                    Optional<ObservedClouds> obsClouds = metar.getClouds();
                    if (obsClouds.isPresent()) {
                        if (obsClouds.get().isNoSignificantCloud()) {
                            noSignificantClouds = true;
                        } else if (obsClouds.get().isNoCloudsDetectedByAutoSystem()) {
                            noCloudsDetected = true;
                        }
                        if (!layer.isPresent()) {
                            if (verticalVisibility.isPresent() && verticalVisibility.get()) {
                                if (obsClouds.get().isVerticalVisibilityUnobservableByAutoSystem()) {
                                    verticalVisibilityNotObservable = true;
                                } else {
                                    verVis = obsClouds.get().getVerticalVisibility();
                                }
                            }
                        }
                    }
                }
            }

            if (noSignificantClouds) {
                retval = Optional.of(this.createLexeme("NSC", LexemeIdentity.CLOUD));
            } else if (noCloudsDetected) {
                retval = Optional.of(this.createLexeme("NCD", LexemeIdentity.CLOUD));
            } else if (verticalVisibilityNotObservable) {
                retval = Optional.of(this.createLexeme("VV///", LexemeIdentity.CLOUD));
            } else {
                if (layer.isPresent()) {
                    retval = Optional.of(this.createLexeme(getCloudLayerToken(layer.get()), LexemeIdentity.CLOUD));
                } else if (verVis.isPresent()) {
                    retval = Optional.of(this.createLexeme(getVerticalVisibilityToken(verVis.get()), LexemeIdentity.CLOUD));
                }
            }

            return retval;
        }

        private String getCloudLayerToken(final fi.fmi.avi.model.CloudLayer layer) throws SerializingException {
            StringBuilder sb = new StringBuilder();
            Optional<NumericMeasure> base = layer.getBase();
            Optional<fi.fmi.avi.model.AviationCodeListUser.CloudType> type = layer.getCloudType();
            Optional<CloudAmount> amount = layer.getAmount();

            if (amount.isPresent()) {
                sb.append(amount.get().name());
            } else {
                sb.append("///");
            }

            if (base.isPresent()) {
                sb.append(String.format("%03d", getAsHectoFeet(base.get())));
            } else if (!amount.isPresent() || CloudAmount.SKC != amount.get()) {
                sb.append("///");
            }
            if (layer instanceof ObservedCloudLayer && ((ObservedCloudLayer) layer).isCloudTypeUnobservableByAutoSystem()) {
                sb.append("///");
            } else {
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
