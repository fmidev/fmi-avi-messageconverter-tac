package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR2;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser.TAFChangeIndicator;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Token parser for TAF change forecast time groups. Handles both the long (ddhh/ddhh) and short format (hhhh) times.
 */
public class TAFChangeForecastTimeGroup extends TimeHandlingRegex {

    public TAFChangeForecastTimeGroup(final OccurrenceFrequency prio) {
        super("^(([0-9]{2})([0-9]{2}))|(([0-9]{2})([0-9]{2})/([0-9]{2})([0-9]{2}))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() && LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR.equals(token.getPrevious().getIdentity())) {
            if (match.group(1) != null) {
                //old 24h TAF: HHHH
                double certainty = 0.5; //could also be horizontal visibility
                final Lexeme l = token.getNext();
                if (l != null && (LexemeIdentity.SURFACE_WIND.equals(l.getIdentity()) || LexemeIdentity.HORIZONTAL_VISIBILITY.equals(l.getIdentity()))) {
                    certainty = 1.0;
                }
                final int fromHour = Integer.parseInt(match.group(2));
                final int toHour = Integer.parseInt(match.group(3));
                if (timeOkHour(fromHour) && timeOkHour(toHour)) {
                    token.identify(LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP, certainty);
                    token.setParsedValue(HOUR1, fromHour);
                    token.setParsedValue(HOUR2, toHour);
                } else {
                    token.identify(LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP, Lexeme.Status.SYNTAX_ERROR, "Invalid time(s)", 0.3);
                }

            } else if (match.group(4) != null) {
                //30h TAF
                final int fromDay = Integer.parseInt(match.group(5));
                final int fromHour = Integer.parseInt(match.group(6));
                final int toDay = Integer.parseInt(match.group(7));
                final int toHour = Integer.parseInt(match.group(8));
                if (timeOkDayHour(fromDay, fromHour) && timeOkDayHour(toDay, toHour)) {
                    token.identify(LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP);
                    token.setParsedValue(DAY1, fromDay);
                    token.setParsedValue(DAY2, toDay);
                    token.setParsedValue(HOUR1, fromHour);
                    token.setParsedValue(HOUR2, toHour);
                } else {
                    token.identify(LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP, Lexeme.Status.SYNTAX_ERROR, "Invalid date and/or time");
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {

            if (TAF.class.isAssignableFrom(clz)) {
                final Optional<TAFChangeForecast> forecast = ctx.getParameter("forecast", TAFChangeForecast.class);
                if (forecast.isPresent() && forecast.get().getChangeIndicator() != TAFChangeIndicator.FROM) {
                    final PartialOrCompleteTimePeriod time = forecast.get().getPeriodOfChange();
                    final Optional<PartialOrCompleteTimeInstant> start = time.getStartTime();
                    final Optional<PartialOrCompleteTimeInstant> end = time.getEndTime();
                    if (start.isPresent() && end.isPresent()) {
                        final String timeStr;
                        if (!start.get().getDay().isPresent() && !end.get().getDay().isPresent()) {
                            timeStr = String.format(Locale.US, "%02d%02d", start.get().getHour().orElse(-1), end.get().getHour().orElse(-1));
                        } else {
                            timeStr = String.format(Locale.US, "%02d%02d/%02d%02d", start.get().getDay().orElse(-1), start.get().getHour().orElse(-1),
                                    end.get().getDay().orElse(-1), end.get().getHour().orElse(-1));
                        }
                        return Optional.of(this.createLexeme(timeStr, LexemeIdentity.TAF_CHANGE_FORECAST_TIME_GROUP));
                    } else {
                        throw new SerializingException("Unable to serialize TAF change group validity time period, both start and end time must be "
                                + "available when group type is not " + TAFChangeIndicator.FROM);
                    }
                }
            }
            return Optional.empty();
        }

    }

}
