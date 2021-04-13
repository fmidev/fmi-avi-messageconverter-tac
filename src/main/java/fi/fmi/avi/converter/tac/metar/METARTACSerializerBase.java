package fi.fmi.avi.converter.tac.metar;

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
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg) throws SerializingException {
        return tokenizeMessage(msg, null);
    }

    protected abstract T narrow(final AviationWeatherMessageOrCollection msg, final ConversionHints hints);

    protected abstract LexemeIdentity getStartTokenIdentity();

    protected abstract Class<T> getMessageClass();

    @Override
    public LexemeSequence tokenizeMessage(final AviationWeatherMessageOrCollection msg, final ConversionHints hints) throws SerializingException {
        final T input = narrow(msg, hints);
        final ReconstructorContext<T> baseCtx = new ReconstructorContext<>(input, hints);
        final LexemeSequenceBuilder retval = this.getLexingFactory().createLexemeSequenceBuilder();
        appendToken(retval, getStartTokenIdentity(), input, getMessageClass(), baseCtx);
        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        if (appendToken(retval, LexemeIdentity.CORRECTION, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.AERODROME_DESIGNATOR, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.ISSUE_TIME, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (appendToken(retval, LexemeIdentity.ROUTINE_DELAYED_OBSERVATION, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }

        if (appendToken(retval, LexemeIdentity.NIL, input, getMessageClass(), baseCtx) > 0) {
            appendToken(retval, LexemeIdentity.END_TOKEN, input, getMessageClass(), baseCtx);
            return retval.build();
        }

        if (appendToken(retval, LexemeIdentity.AUTOMATED, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.SURFACE_WIND, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.VARIABLE_WIND_DIRECTION, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.CAVOK, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.HORIZONTAL_VISIBILITY, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (input.getRunwayVisualRanges().isPresent()) {
            for (final RunwayVisualRange range : input.getRunwayVisualRanges().get()) {
                appendToken(retval, LexemeIdentity.RUNWAY_VISUAL_RANGE, input, getMessageClass(), baseCtx.copyWithParameter("rvr", range));
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
        if (input.getPresentWeather().isPresent()) {
            for (final Weather weather : input.getPresentWeather().get()) {
                appendToken(retval, LexemeIdentity.WEATHER, input, getMessageClass(), baseCtx.copyWithParameter("weather", weather));
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
        final Optional<ObservedClouds> obsClouds = input.getClouds();
        if (obsClouds.isPresent()) {
            if (obsClouds.get().getVerticalVisibility().isPresent() || obsClouds.get().isVerticalVisibilityUnobservableByAutoSystem()) {
                this.appendToken(retval, LexemeIdentity.CLOUD, input, getMessageClass(), baseCtx.copyWithParameter("verticalVisibility", Boolean.TRUE));
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            } else if (obsClouds.get().getLayers().isPresent()) {
                this.appendCloudLayers(retval, input, getMessageClass(), obsClouds.get().getLayers().get(), baseCtx);
            } else if (obsClouds.get().isNoCloudsDetectedByAutoSystem() || obsClouds.get().isNoSignificantCloud()) {
                this.appendToken(retval, LexemeIdentity.CLOUD, input, getMessageClass(), baseCtx);
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
        if (appendToken(retval, LexemeIdentity.AIR_DEWPOINT_TEMPERATURE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.AIR_PRESSURE_QNH, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (input.getRecentWeather().isPresent()) {
            for (final Weather weather : input.getRecentWeather().get()) {
                appendToken(retval, LexemeIdentity.RECENT_WEATHER, input, getMessageClass(), baseCtx.copyWithParameter("weather", weather));
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
        if (appendToken(retval, LexemeIdentity.WIND_SHEAR, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.SEA_STATE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (input.getRunwayStates().isPresent()) {
            for (final RunwayState state : input.getRunwayStates().get()) {
                appendToken(retval, LexemeIdentity.RUNWAY_STATE, input, getMessageClass(), baseCtx.copyWithParameter("state", state));
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
        if (appendToken(retval, LexemeIdentity.SNOW_CLOSURE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.NO_SIGNIFICANT_WEATHER, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.COLOR_CODE, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (appendToken(retval, LexemeIdentity.NO_SIGNIFICANT_CHANGES, input, getMessageClass(), baseCtx) > 0) {
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
        }
        if (input.getTrends().isPresent()) {
            for (final TrendForecast trend : input.getTrends().get()) {
                final ReconstructorContext trendCtx = baseCtx.copyWithParameter("trend", trend);
                if (appendToken(retval, LexemeIdentity.TREND_CHANGE_INDICATOR, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (appendToken(retval, LexemeIdentity.TREND_TIME_GROUP, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (appendToken(retval, LexemeIdentity.SURFACE_WIND, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (appendToken(retval, LexemeIdentity.CAVOK, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (appendToken(retval, LexemeIdentity.NO_SIGNIFICANT_WEATHER, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (appendToken(retval, LexemeIdentity.HORIZONTAL_VISIBILITY, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
                if (trend.getForecastWeather().isPresent()) {
                    for (final Weather weather : trend.getForecastWeather().get()) {
                        appendToken(retval, LexemeIdentity.WEATHER, input, getMessageClass(), trendCtx.copyWithParameter("weather", weather));
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    }
                }

                final Optional<CloudForecast> clouds = trend.getCloud();
                if (clouds.isPresent()) {
                    if (clouds.get().getVerticalVisibility().isPresent()) {
                        this.appendToken(retval, LexemeIdentity.CLOUD, input, getMessageClass(),
                                trendCtx.copyWithParameter("verticalVisibility", Boolean.TRUE));
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    } else if (clouds.get().isNoSignificantCloud()) {
                        this.appendToken(retval, LexemeIdentity.CLOUD, input, getMessageClass(), trendCtx);
                        appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                    } else if (clouds.get().getLayers().isPresent()) {
                        this.appendCloudLayers(retval, input, getMessageClass(), clouds.get().getLayers().get(), trendCtx);
                    }
                }
                if (appendToken(retval, LexemeIdentity.COLOR_CODE, input, getMessageClass(), trendCtx) > 0) {
                    appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
                }
            }
        }
        if (input.getRemarks().isPresent()) {
            appendToken(retval, LexemeIdentity.REMARKS_START, input, getMessageClass(), baseCtx);
            appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            for (final String remark : input.getRemarks().get()) {
                this.appendToken(retval, LexemeIdentity.REMARK, input, getMessageClass(), baseCtx.copyWithParameter("remark", remark));
                appendWhitespace(retval, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE);
            }
        }
        retval.removeLast();
        appendToken(retval, LexemeIdentity.END_TOKEN, input, getMessageClass(), baseCtx);
        return retval.build();
    }
}

