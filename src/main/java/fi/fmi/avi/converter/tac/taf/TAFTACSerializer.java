package fi.fmi.avi.converter.tac.taf;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionIssue.Type;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACSerializer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
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
        final ConversionResult<String> result = new ConversionResult<>();
        try {
            final LexemeSequence seq = tokenizeMessage(input, hints);
            result.setConvertedMessage(seq.getTAC());
        } catch (final SerializingException se) {
            result.addIssue(new ConversionIssue(Type.OTHER, se.getMessage()));
        }
        return result;
    }

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        if (!(msg instanceof TAF)) {
            throw new SerializingException("I can only tokenize TAFs!");
        }
        final TAF input = (TAF) msg;
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        final ReconstructorContext<TAF> baseCtx = new ReconstructorContext<>(input, hints);
        appendToken(retval, LexemeIdentity.TAF_START, input, TAF.class, baseCtx);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        if (appendToken(retval, LexemeIdentity.AMENDMENT, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.CORRECTION, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.AERODROME_DESIGNATOR, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.ISSUE_TIME, input, TAF.class, baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (!input.isMissingMessage()) {
            if (appendToken(retval, LexemeIdentity.VALID_TIME, input, TAF.class, baseCtx) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }

            if (appendToken(retval, LexemeIdentity.CANCELLATION, input, TAF.class, baseCtx) > 0) {
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
            if (!input.isCancelMessage()) {
                final Optional<TAFBaseForecast> baseFct = input.getBaseForecast();
                if (!baseFct.isPresent()) {
                    throw new SerializingException("Missing base forecast");
                }
                final ReconstructorContext<TAF> baseFctCtx = baseCtx.copyWithParameter("forecast", baseFct.get());
                if (appendToken(retval, LexemeIdentity.SURFACE_WIND, input, TAF.class, baseFctCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (appendToken(retval, LexemeIdentity.CAVOK, input, TAF.class, baseFctCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (appendToken(retval, LexemeIdentity.HORIZONTAL_VISIBILITY, input, TAF.class, baseFctCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (baseFct.get().getForecastWeather().isPresent()) {
                    for (final Weather weather : baseFct.get().getForecastWeather().get()) {
                        appendToken(retval, LexemeIdentity.WEATHER, input, TAF.class, baseFctCtx.copyWithParameter("weather", weather));
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                }
                final Optional<CloudForecast> clouds = baseFct.get().getCloud();
                if (clouds.isPresent()) {
                    appendClouds(retval, clouds.get(), input, baseFctCtx);
                }

                if (baseFct.get().getTemperatures().isPresent()) {
                    for (final TAFAirTemperatureForecast tempFct : baseFct.get().getTemperatures().get()) {
                        final ReconstructorContext<TAF> tempCtx = baseFctCtx.copyWithParameter("temp", tempFct);
                        appendToken(retval, LexemeIdentity.MAX_TEMPERATURE, input, TAF.class, tempCtx);
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        appendToken(retval, LexemeIdentity.MIN_TEMPERATURE, input, TAF.class, tempCtx);
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                }

                if (input.getChangeForecasts().isPresent()) {
                    for (final TAFChangeForecast changeFct : input.getChangeForecasts().get()) {
                        final ReconstructorContext<TAF> changeFctCtx = baseCtx.copyWithParameter("forecast", changeFct);
                        retval.removeLast(); //last whitespace
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN);
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED);
                        if (appendToken(retval, LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval, LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval, LexemeIdentity.SURFACE_WIND, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval, LexemeIdentity.CAVOK, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval, LexemeIdentity.HORIZONTAL_VISIBILITY, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (appendToken(retval, LexemeIdentity.NO_SIGNIFICANT_WEATHER, input, TAF.class, changeFctCtx) > 0) {
                            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                        }
                        if (changeFct.getForecastWeather().isPresent()) {
                            for (final Weather weather : changeFct.getForecastWeather().get()) {
                                appendToken(retval, LexemeIdentity.WEATHER, input, TAF.class, changeFctCtx.copyWithParameter("weather", weather));
                                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                            }
                        }
                        if (changeFct.getCloud().isPresent()) {
                            appendClouds(retval, changeFct.getCloud().get(), input, changeFctCtx);
                        }
                    }
                }
                if (input.getRemarks().isPresent()) {
                    appendToken(retval, LexemeIdentity.REMARKS_START, input, TAF.class, baseCtx);
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    for (final String remark : input.getRemarks().get()) {
                        this.appendToken(retval, LexemeIdentity.REMARK, input, TAF.class, baseCtx.copyWithParameter("remark", remark));
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                }
            }
        } else {
            appendToken(retval, LexemeIdentity.NIL, input, TAF.class, baseCtx);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        retval.removeLast();
        appendToken(retval, LexemeIdentity.END_TOKEN, input, TAF.class, baseCtx);
        return retval.build();
    }

    private void appendClouds(final LexemeSequenceBuilder builder, final CloudForecast clouds, final TAF input, final ReconstructorContext<TAF> ctx)
            throws SerializingException {
        if (clouds != null) {
            if (clouds.getVerticalVisibility().isPresent()) {
                this.appendToken(builder, LexemeIdentity.CLOUD, input, TAF.class, ctx.copyWithParameter("verticalVisibility", Boolean.TRUE));
                appendWhitespace(builder, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            } else if (clouds.getLayers().isPresent()) {
                this.appendCloudLayers(builder, input, TAF.class, clouds.getLayers().get(), ctx);
            } else if (clouds.isNoSignificantCloud()) {
                this.appendToken(builder, LexemeIdentity.CLOUD, input, TAF.class, ctx);
                appendWhitespace(builder, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
    }
}
