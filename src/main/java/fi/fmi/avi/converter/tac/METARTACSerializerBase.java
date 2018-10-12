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
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CloudForecast;
import fi.fmi.avi.model.Weather;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.metar.ObservedClouds;
import fi.fmi.avi.model.metar.RunwayState;
import fi.fmi.avi.model.metar.RunwayVisualRange;
import fi.fmi.avi.model.metar.TrendForecast;

/**
 * Serializes METAR POJO to TAC format
 */
public abstract class METARTACSerializerBase<T extends MeteorologicalTerminalAirReport> extends AbstractTACSerializer<T> {

    @Override
    public ConversionResult<String> convertMessage(final T input, final ConversionHints hints) {
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
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    protected abstract T narrow(final AviationWeatherMessageOrCollection msg, final ConversionHints hints);

    protected abstract Lexeme.Identity getStartTokenIdentity();

    protected abstract Class<T> getMessageClass();

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        T input = narrow(msg, hints);
        ReconstructorContext<T> baseCtx = new ReconstructorContext<>(input, hints);
        LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        appendToken(retval, getStartTokenIdentity(), input, getMessageClass(), baseCtx);
        appendWhitespace(retval, ' ');
        if (appendToken(retval, Identity.CORRECTION, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.AERODROME_DESIGNATOR, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.ISSUE_TIME, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }

        if (appendToken(retval, Identity.ROUTINE_DELAYED_OBSERVATION, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }

        if (appendToken(retval, Identity.NIL, input, getMessageClass(), baseCtx) > 0) {
            appendToken(retval, Identity.END_TOKEN, input, getMessageClass(), baseCtx);
            return retval.build();
        }

        if (appendToken(retval, Identity.AUTOMATED, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.SURFACE_WIND, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.VARIABLE_WIND_DIRECTION, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.CAVOK, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (input.getRunwayVisualRanges().isPresent()) {
            for (RunwayVisualRange range : input.getRunwayVisualRanges().get()) {
                appendToken(retval, Identity.RUNWAY_VISUAL_RANGE, input, getMessageClass(), baseCtx.copyWithParameter("rvr", range));
                appendWhitespace(retval, ' ');
            }
        }
        if (input.getPresentWeather().isPresent()) {
            for (Weather weather : input.getPresentWeather().get()) {
                appendToken(retval, Identity.WEATHER, input, getMessageClass(), baseCtx.copyWithParameter("weather", weather));
                appendWhitespace(retval, ' ');
            }
        }
        Optional<ObservedClouds> obsClouds = input.getClouds();
        if (obsClouds.isPresent()) {
            if (obsClouds.get().getVerticalVisibility().isPresent() || obsClouds.get().isVerticalVisibilityUnobservableByAutoSystem()) {
                this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(),baseCtx.copyWithParameter("verticalVisibility", Boolean.TRUE));
                appendWhitespace(retval, ' ');
            } else if (obsClouds.get().getLayers().isPresent()){
                this.appendCloudLayers(retval, input, getMessageClass(), obsClouds.get().getLayers().get(), baseCtx);
            } else if (obsClouds.get().isNoCloudsDetectedByAutoSystem() || obsClouds.get().isNoSignificantCloud()) {
                this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), baseCtx);
                appendWhitespace(retval, ' ');
            }
        }
        if (appendToken(retval, Identity.AIR_DEWPOINT_TEMPERATURE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.AIR_PRESSURE_QNH, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (input.getRecentWeather().isPresent()) {
            for (Weather weather : input.getRecentWeather().get()) {
                appendToken(retval, Identity.RECENT_WEATHER, input, getMessageClass(), baseCtx.copyWithParameter("weather", weather));
                appendWhitespace(retval, ' ');
            }
        }
        if (appendToken(retval, Identity.WIND_SHEAR, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.SEA_STATE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (input.getRunwayStates().isPresent()) {
            for (RunwayState state : input.getRunwayStates().get()) {
                appendToken(retval, Identity.RUNWAY_STATE, input, getMessageClass(), baseCtx.copyWithParameter("state", state));
                appendWhitespace(retval, ' ');
            }
        }
        if (appendToken(retval, Identity.SNOW_CLOSURE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.NO_SIGNIFICANT_WEATHER, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.COLOR_CODE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (appendToken(retval, Identity.NO_SIGNIFICANT_CHANGES, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, ' ');
        }
        if (input.getTrends().isPresent()) {
            for (TrendForecast trend : input.getTrends().get()) {
                ReconstructorContext trendCtx = baseCtx.copyWithParameter("trend", trend);
                if (appendToken(retval, Identity.TREND_CHANGE_INDICATOR, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (appendToken(retval, Identity.TREND_TIME_GROUP, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (appendToken(retval, Identity.SURFACE_WIND, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (appendToken(retval, Identity.CAVOK, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (appendToken(retval, Identity.NO_SIGNIFICANT_WEATHER, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (appendToken(retval, Identity.HORIZONTAL_VISIBILITY, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
                if (trend.getForecastWeather().isPresent()) {
                    for (Weather weather : trend.getForecastWeather().get()) {
                        appendToken(retval, Identity.WEATHER, input, getMessageClass(), trendCtx.copyWithParameter("weather", weather));
                        appendWhitespace(retval, ' ');
                    }
                }

                Optional<CloudForecast> clouds = trend.getCloud();
                if (clouds.isPresent()) {
                    if (clouds.get().getVerticalVisibility().isPresent()) {
                        this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), trendCtx.copyWithParameter("verticalVisibility", Boolean.TRUE));
                        appendWhitespace(retval, ' ');
                    } else if (clouds.get().isNoSignificantCloud()) {
                        this.appendToken(retval, Lexeme.Identity.CLOUD, input, getMessageClass(), trendCtx);
                        appendWhitespace(retval, ' ');
                    } else if (clouds.get().getLayers().isPresent()){
                        this.appendCloudLayers(retval, input, getMessageClass(), clouds.get().getLayers().get(), trendCtx);
                    }
                }
                if (appendToken(retval, Identity.COLOR_CODE, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, ' ');
                }
            }
        }
        if (input.getRemarks().isPresent()) {
            appendToken(retval, Identity.REMARKS_START, input, getMessageClass(), baseCtx);
            appendWhitespace(retval, ' ');
            for (String remark : input.getRemarks().get()) {
                this.appendToken(retval, Identity.REMARK, input, getMessageClass(), baseCtx.copyWithParameter("remark", remark));
                appendWhitespace(retval, ' ');
            }
        }
        retval.removeLast();
        appendToken(retval, Identity.END_TOKEN, input, getMessageClass(), baseCtx);
        return retval.build();
    }
}

