package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFAirTemperatureForecast;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Serializes TAF POJO to TAC format
 */
public class TAFTACSerializer extends AbstractTACSerializer<TAF> {

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

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
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        if (!(msg instanceof TAF)) {
            throw new SerializingException("I can only tokenize TAFs!");
        }
        TAF input = (TAF) msg;
        LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        ReconstructorContext<TAF> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, Identity.TAF_START, input, TAF.class, baseCtx);
        appendWhitespace(retval, ' ');
        if (appendToken(retval, Identity.AMENDMENT, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.CORRECTION, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.AERODROME_DESIGNATOR, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.ISSUE_TIME, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }

        if (AviationCodeListUser.TAFStatus.MISSING != input.getStatus()) {
            if (appendToken(retval, Identity.VALID_TIME, input, TAF.class, baseCtx) > 0) {
                appendWhitespace(retval, ' ');
            }

            if (appendToken(retval, Identity.CANCELLATION, input, TAF.class, baseCtx) > 0) {
                appendWhitespace(retval, ' ');
            }
            if (AviationCodeListUser.TAFStatus.CANCELLATION != input.getStatus()) {
                Optional<TAFBaseForecast> baseFct = input.getBaseForecast();
                if (!baseFct.isPresent()) {
                    throw new SerializingException("Missing base forecast");
                }
                ReconstructorContext<TAF> baseFctCtx = baseCtx.copyWithParameter("forecast", baseFct.get());
                if (appendToken(retval, Identity.SURFACE_WIND, input, TAF.class, baseFctCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (appendToken(retval, Identity.CAVOK, input, TAF.class, baseFctCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, TAF.class, baseFctCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (baseFct.get().getForecastWeather().isPresent()) {
                    for (Weather weather : baseFct.get().getForecastWeather().get()) {
                        appendToken(retval, Identity.WEATHER, input, TAF.class, baseFctCtx.copyWithParameter("weather", weather));
                        appendWhitespace(retval, ' ');
                    }
                }
                Optional<CloudForecast> clouds = baseFct.get().getCloud();
                if (clouds.isPresent()) {
                    appendClouds(retval, clouds.get(), input, baseFctCtx);
                }

                if (baseFct.get().getTemperatures().isPresent()) {
                    for (TAFAirTemperatureForecast tempFct : baseFct.get().getTemperatures().get()) {
                        ReconstructorContext<TAF> tempCtx = baseFctCtx.copyWithParameter("temp", tempFct);
                        appendToken(retval, Identity.MAX_TEMPERATURE, input, TAF.class, tempCtx);
                        appendWhitespace(retval, ' ');
                        appendToken(retval, Identity.MIN_TEMPERATURE, input, TAF.class, tempCtx);
                        appendWhitespace(retval, ' ');
                    }
                }

                if (input.getChangeForecasts().isPresent()) {
                    for (TAFChangeForecast changeFct : input.getChangeForecasts().get()) {
                        ReconstructorContext<TAF> changeFctCtx = baseCtx.copyWithParameter("forecast", changeFct);
                        retval.removeLast(); //last whitespace
                        appendWhitespace(retval, '\n');
                        if (appendToken(retval, Identity.TAF_FORECAST_CHANGE_INDICATOR, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, ' ');
                        }
                        if (appendToken(retval, Identity.TAF_CHANGE_FORECAST_TIME_GROUP, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, ' ');
                        }
                        if (appendToken(retval, Identity.SURFACE_WIND, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, ' ');
                        }
                        if (appendToken(retval, Identity.CAVOK, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, ' ');
                        }
                        if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, ' ');
                        }
                        if (appendToken(retval, Identity.NO_SIGNIFICANT_WEATHER, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, ' ');
                        }
                        if (changeFct.getForecastWeather().isPresent()) {
                            for (Weather weather : changeFct.getForecastWeather().get()) {
                                appendToken(retval, Identity.WEATHER, input, TAF.class, changeFctCtx.copyWithParameter("weather", weather));
                                appendWhitespace(retval, ' ');
                            }
                        }
                        if (changeFct.getCloud().isPresent()) {
                            appendClouds(retval, changeFct.getCloud().get(), input, changeFctCtx);
                        }
                    }
                }
                if (input.getRemarks().isPresent()) {
                    appendToken(retval, Identity.REMARKS_START, input, TAF.class, baseCtx);
                    appendWhitespace(retval, ' ');
                    for (String remark : input.getRemarks().get()) {
                        this.appendToken(retval, Identity.REMARK, input, TAF.class, baseCtx.copyWithParameter("remark", remark));
                        appendWhitespace(retval, ' ');
                    }
                }
            }
        } else {
            appendToken(retval, Identity.NIL, input, TAF.class, baseCtx);
            appendWhitespace(retval, ' ');
        }
        retval.removeLast();
        appendToken(retval, Identity.END_TOKEN, input, TAF.class, baseCtx);
        return retval.build();
    }

    private void appendClouds(final LexemeSequenceBuilder builder, final CloudForecast clouds, final TAF input, final ReconstructorContext<TAF> ctx) throws SerializingException {
        if (clouds != null) {
            if (clouds.getVerticalVisibility().isPresent()) {
                this.appendToken(builder, Lexeme.Identity.CLOUD, input, TAF.class, ctx.copyWithParameter("verticalVisibility", Boolean.TRUE));
                appendWhitespace(builder, ' ');
            } else if (clouds.getLayers().isPresent()){
                this.appendCloudLayers(builder, input, TAF.class, clouds.getLayers().get(), ctx);
            }
        }
    }
}
