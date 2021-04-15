package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE2;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.sigmet.SIGMET;

public class SigmetValidTime extends TimeHandlingRegex {

    public SigmetValidTime(final OccurrenceFrequency prio) {
        super("^VALID\\s(?<startDay>[0-9]{2})(?<startHour>[0-9]{2})(?<startMinute>[0-9]{2})[/-](?<endDay>[0-9]{2})(?<endHour>[0-9]{2})"
                + "(?<endMinute>[0-9]{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        final int fromDay = Integer.parseInt(match.group("startDay"));
        final int fromHour = Integer.parseInt(match.group("startHour"));
        final int fromMinute = Integer.parseInt(match.group("startMinute"));
        final int toDay = Integer.parseInt(match.group("endDay"));
        final int toHour = Integer.parseInt(match.group("endHour"));
        final int toMinute = Integer.parseInt(match.group("endMinute"));
        if (timeOkDayHourMinute(fromDay, fromHour, fromMinute) && timeOkDayHourMinute(toDay, toHour, toMinute)) {
            token.identify(LexemeIdentity.VALID_TIME);
            token.setParsedValue(DAY1, fromDay);
            token.setParsedValue(DAY2, toDay);
            token.setParsedValue(HOUR1, fromHour);
            token.setParsedValue(HOUR2, toHour);
            token.setParsedValue(MINUTE1, fromMinute);
            token.setParsedValue(MINUTE2, toMinute);
        } else {
            token.identify(LexemeIdentity.VALID_TIME, Lexeme.Status.SYNTAX_ERROR, "Invalid date and/or time");
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET)msg;
                if (sigmet.getValidityPeriod().getStartTime().isPresent()&&
                    sigmet.getValidityPeriod().getEndTime().isPresent()) {
                    PartialOrCompleteTimeInstant start = sigmet.getValidityPeriod().getStartTime().get();
                    PartialOrCompleteTimeInstant end = sigmet.getValidityPeriod().getEndTime().get();

                    StringBuilder sb=new StringBuilder("VALID ");
                    sb.append(String.format("%02d%02d%02d/", start.getDay().getAsInt(),
                              start.getHour().getAsInt(), start.getMinute().getAsInt()));
                    sb.append(String.format("%02d%02d%02d", end.getDay().getAsInt(),
                              end.getHour().getAsInt(), end.getMinute().getAsInt()));
                    return Optional.of(createLexeme(sb.toString(), LexemeIdentity.VALID_TIME));
                }
            }
            return Optional.empty();
        }
    }
}
