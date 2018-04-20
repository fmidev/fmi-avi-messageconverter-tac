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
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.RunwayState;
import fi.fmi.avi.model.metar.RunwayVisualRange;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.metar.immutable.METARImpl;

/**
 * Serializes METARImpl POJO to TAC format
 */
public abstract class METARTACSerializerBase<S extends METARImpl> extends AbstractTACSerializer<S> {

    @Override
    public ConversionResult<String> convertMessage(final S input, final ConversionHints hints) {
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

    protected abstract S narrow(final AviationWeatherMessage msg, final ConversionHints hints) throws SerializingException;

    protected abstract Class<S> getMessageClass();

    protected abstract Identity getStartTokenIdentity();

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessage msg, final ConversionHints hints) throws SerializingException {
        S input = narrow(msg, hints);
        LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        appendToken(retval, getStartTokenIdentity(), input, getMessageClass(), hints);
        appendWhitespace(retval, ' ', hints);
        if (appendToken(retval, Identity.CORRECTION, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.AERODROME_DESIGNATOR, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.ISSUE_TIME, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }

        if (appendToken(retval, Identity.ROUTINE_DELAYED_OBSERVATION, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }

        if (appendToken(retval, Identity.NIL, input, getMessageClass(), hints) > 0) {
            appendToken(retval, Identity.END_TOKEN, input, getMessageClass(), hints);
            return retval.build();
        }

        if (appendToken(retval, Identity.AUTOMATED, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.SURFACE_WIND, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.VARIABLE_WIND_DIRECTION, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.CAVOK, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (input.getRunwayVisualRanges() != null) {
            for (RunwayVisualRange range : input.getRunwayVisualRanges()) {
                appendToken(retval, Identity.RUNWAY_VISUAL_RANGE, input, getMessageClass(), hints, range);
                appendWhitespace(retval, ' ', hints);
            }
        }
        if (input.getPresentWeather() != null) {
            for (Weather weather : input.getPresentWeather()) {
                appendToken(retval, Identity.WEATHER, input, getMessageClass(), hints, weather);
                appendWhitespace(retval, ' ', hints);
            }
        }
        ObservedClouds obsClouds = input.getClouds();
        if (obsClouds != null) {
            if (obsClouds.getVerticalVisibility() != null) {
                this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), hints, "VV");
                appendWhitespace(retval, ' ', hints);
            } else if (obsClouds.isAmountAndHeightUnobservableByAutoSystem()) {
                this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), hints, "//////");
                appendWhitespace(retval, ' ', hints);
            } else if (obsClouds.isNoSignificantCloud()) {
                this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), hints, "NSC");
                appendWhitespace(retval, ' ', hints);
            } else {
                this.appendCloudLayers(retval, input, getMessageClass(), obsClouds.getLayers(), hints);
            }
        }
        if (appendToken(retval, Identity.AIR_DEWPOINT_TEMPERATURE, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.AIR_PRESSURE_QNH, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (input.getRecentWeather() != null) {
            for (Weather weather : input.getRecentWeather()) {
                appendToken(retval, Identity.RECENT_WEATHER, input, getMessageClass(), hints, weather);
                appendWhitespace(retval, ' ', hints);
            }
        }
        if (appendToken(retval, Identity.WIND_SHEAR, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.SEA_STATE, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (input.getRunwayStates() != null) {
            for (RunwayState state : input.getRunwayStates()) {
                appendToken(retval, Identity.RUNWAY_STATE, input, getMessageClass(), hints, state);
                appendWhitespace(retval, ' ', hints);
            }
        }
        if (appendToken(retval, Identity.NO_SIGNIFICANT_WEATHER, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (appendToken(retval, Identity.COLOR_CODE, input, getMessageClass(), hints) > 0) {
            appendWhitespace(retval, ' ', hints);
        }
        if (input.getTrends() != null) {
            for (TrendForecast trend : input.getTrends()) {
                if (appendToken(retval, Identity.TREND_CHANGE_INDICATOR, input, getMessageClass(), hints, trend) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (appendToken(retval, Identity.TREND_TIME_GROUP, input, getMessageClass(), hints, trend) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (appendToken(retval, Identity.SURFACE_WIND, input, getMessageClass(), hints, trend) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (appendToken(retval, Identity.CAVOK, input, getMessageClass(), hints, trend) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (appendToken(retval, Identity.NO_SIGNIFICANT_WEATHER, input, getMessageClass(), hints, trend) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, getMessageClass(), hints, trend) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
                if (trend.getForecastWeather() != null) {
                    for (Weather weather : trend.getForecastWeather()) {
                        appendToken(retval, Identity.WEATHER, input, getMessageClass(), hints, trend, weather);
                        appendWhitespace(retval, ' ', hints);
                    }
                }

                CloudForecast clouds = trend.getCloud();
                if (clouds != null) {
                    if (clouds.getVerticalVisibility() != null) {
                        this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), hints, "VV", trend);
                        appendWhitespace(retval, ' ', hints);
                    } else if (clouds.isNoSignificantCloud()) {
                        this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), hints, trend);
                        appendWhitespace(retval, ' ', hints);
                    } else {
                        this.appendCloudLayers(retval, input, getMessageClass(), clouds.getLayers(), hints, trend);
                    }
                }
                if (appendToken(retval, Identity.COLOR_CODE, input, getMessageClass(), hints, trend) > 0) {
                    appendWhitespace(retval, ' ', hints);
                }
            }
        }
        if (input.getRemarks() != null && !input.getRemarks().isEmpty()) {
            appendToken(retval, Identity.REMARKS_START, input, getMessageClass(), hints);
            appendWhitespace(retval, ' ', hints);
            for (String remark : input.getRemarks()) {
                this.appendToken(retval, Identity.REMARK, input, getMessageClass(), hints, remark);
                appendWhitespace(retval, ' ', hints);
            }
        }
        retval.removeLast();
        appendToken(retval, Identity.END_TOKEN, input, getMessageClass(), hints);
        return retval.build();
    }
}

