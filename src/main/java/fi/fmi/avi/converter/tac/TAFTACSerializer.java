package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

import java.util.Optional;

/**
 * Serializes TAF POJO to TAC format
 */
public class TAFTACSerializer extends AbstractTACSerializer<TAF> {

	@Override
    public ConversionResult<String> convertMessage(final TAF input, final ConversionHints hints) {
        ConversionResult<String> result = new ConversionResult<>();
        try {
        	LexemeSequence seq = tokenizeMessage(input, hints);
        	result.setConvertedMessage(seq.getTAC());
        } catch (SerializingException se) {
        	result.addIssue(new ConversionIssue(Type.OTHER, se.getMessage()));
        }
    	return result;
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessage msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessage msg, final ConversionHints hints) throws SerializingException {
        if (!(msg instanceof TAF)) {
            throw new SerializingException("I can only tokenize TAFs!");
        }
        TAF input = (TAF) msg;
        LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        appendToken(retval, Identity.TAF_START, input, TAF.class, hints);
        appendWhitespace(retval, ' ', hints);
        if (appendToken(retval, Identity.AMENDMENT, input, TAF.class, hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.CORRECTION, input, TAF.class, hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.AERODROME_DESIGNATOR, input, TAF.class, hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.ISSUE_TIME, input, TAF.class, hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }

        if (AviationCodeListUser.TAFStatus.MISSING != input.getStatus()) {
            if (appendToken(retval, Identity.VALID_TIME, input, TAF.class, hints) > 0) {
                appendWhitespace(retval, ' ', hints);
            }

            if (appendToken(retval, Identity.CANCELLATION, input, TAF.class, hints) > 0) {
                appendWhitespace(retval, ' ', hints);
            }
            if (AviationCodeListUser.TAFStatus.CANCELLATION != input.getStatus()) {
                Optional<TAFBaseForecast> baseFct = input.getBaseForecast();
                if (!baseFct.isPresent()) {
                    throw new SerializingException("Missing base forecast");
                }
                if (appendToken(retval, Identity.SURFACE_WIND, input, TAF.class, hints, baseFct) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (appendToken(retval, Identity.CAVOK, input, TAF.class, hints, baseFct) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, TAF.class, hints, baseFct) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (baseFct.get().getForecastWeather().isPresent()) {
                    for (Weather weather : baseFct.get().getForecastWeather().get()) {
                        appendToken(retval, Identity.WEATHER, input, TAF.class, hints, baseFct, weather);
                        appendWhitespace(retval, ' ', hints);
                    }
                }
                Optional<CloudForecast> clouds = baseFct.get().getCloud();
                if (clouds.isPresent()) {
                    appendClouds(retval, clouds.get(), input, baseFct, hints);
                }

                if (baseFct.get().getTemperatures().isPresent()) {
                    for (TAFAirTemperatureForecast tempFct : baseFct.get().getTemperatures().get()) {
                        appendToken(retval, Identity.MAX_TEMPERATURE, input, TAF.class, hints, baseFct, tempFct);
                        appendWhitespace(retval, ' ', hints);
                        // No MIN_TEMPERATURE needed as they are produced together
                    }
                }

                if (input.getChangeForecasts().isPresent()) {
                    for (TAFChangeForecast changeFct : input.getChangeForecasts().get()) {
                        retval.removeLast(); //last whitespace
                        appendWhitespace(retval, '\n', hints);
                        if (appendToken(retval, Identity.TAF_FORECAST_CHANGE_INDICATOR, input, TAF.class, hints, changeFct) > 0) {
                            appendWhitespace(retval, ' ', hints);
                        }
                        if (appendToken(retval, Identity.TAF_CHANGE_FORECAST_TIME_GROUP, input, TAF.class, hints, changeFct) > 0) {
                            appendWhitespace(retval, ' ', hints);
                        }
                        if (appendToken(retval, Identity.SURFACE_WIND, input, TAF.class, hints, changeFct) > 0) {
                            appendWhitespace(retval, ' ', hints);
                        }
                        if (appendToken(retval, Identity.CAVOK, input, TAF.class, hints, changeFct) > 0) {
                            appendWhitespace(retval, ' ', hints);
                        }
                        if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, TAF.class, hints, changeFct) > 0) {
                            appendWhitespace(retval, ' ', hints);
                        }
                        if (appendToken(retval, Identity.NO_SIGNIFICANT_WEATHER, input, TAF.class, hints, changeFct) > 0) {
                            appendWhitespace(retval, ' ', hints);
                        }
                        if (changeFct.getForecastWeather().isPresent()) {
                            for (Weather weather : changeFct.getForecastWeather().get()) {
                                appendToken(retval, Identity.WEATHER, input, TAF.class, hints, changeFct, weather);
                                appendWhitespace(retval, ' ', hints);
                            }
                        }
                        if (changeFct.getCloud().isPresent()) {
                            appendClouds(retval, changeFct.getCloud().get(), input, changeFct, hints);
                        }
                    }
                }
                if (input.getRemarks().isPresent()) {
                    appendToken(retval, Identity.REMARKS_START, input, TAF.class, hints);
                    appendWhitespace(retval, ' ', hints);
                    for (String remark : input.getRemarks().get()) {
                        this.appendToken(retval, Identity.REMARK, input, TAF.class, hints, remark);
                        appendWhitespace(retval, ' ', hints);
                    }
                }
            }
        } else {
            appendToken(retval, Identity.NIL, input, TAF.class, hints);
            appendWhitespace(retval, ' ', hints);
        }
        retval.removeLast();
        appendToken(retval, Identity.END_TOKEN, input, TAF.class, hints);
        return retval.build();
    }

    private void appendClouds(final LexemeSequenceBuilder builder, final CloudForecast clouds, final TAF input, final Object specifier, final ConversionHints hints) throws SerializingException {
        if (clouds != null) {
            if (clouds.getVerticalVisibility() != null) {
                this.appendToken(builder, Lexeme.Identity.CLOUD, input, TAF.class, hints, "VV", specifier);
                appendWhitespace(builder, ' ', hints);
            } else if (clouds.getLayers().isPresent()){
                this.appendCloudLayers(builder, input, TAF.class, clouds.getLayers().get(), hints, specifier);
            }
        }
    }
}
