package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.Reference;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIRMET_CANCEL;

/**
 * Created by rinne on 10/02/17.
 */
public class AirmetCancel extends RegexMatchingLexemeVisitor {

    public AirmetCancel(final OccurrenceFrequency prio) {
        super("^CNL AIRMET (\\w?\\d?\\d) (\\d{2})(\\d{2})(\\d{2})/(\\d{2})(\\d{2})(\\d{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(AIRMET_CANCEL);
        token.setParsedValue(ParsedValueName.SEQUENCE_DESCRIPTOR, match.group(1));
        token.setParsedValue(ParsedValueName.DAY1, Integer.valueOf(match.group(2)));
        token.setParsedValue(ParsedValueName.HOUR1, Integer.valueOf(match.group(3)));
        token.setParsedValue(ParsedValueName.MINUTE1, Integer.valueOf(match.group(4)));
        token.setParsedValue(ParsedValueName.DAY2, Integer.valueOf(match.group(5)));
        token.setParsedValue(ParsedValueName.HOUR2, Integer.valueOf(match.group(6)));
        token.setParsedValue(ParsedValueName.MINUTE2, Integer.valueOf(match.group(7)));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (AIRMET.class.isAssignableFrom(clz)) {
                AIRMET airmet = (AIRMET)msg;
                if (airmet.isCancelMessage()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("CNL");
                    sb.append(" ");
                    sb.append("AIRMET");
                    sb.append(" ");
                    sb.append(airmet.getCancelledReference().get().getSequenceNumber());
                    sb.append(" ");
                    PartialOrCompleteTimeInstant start = airmet.getCancelledReference().get().getValidityPeriod().getStartTime().get();
                    PartialOrCompleteTimeInstant end = airmet.getCancelledReference().get().getValidityPeriod().getEndTime().get();


                    sb.append(String.format("%02d%02d%02d",
                            start.getDay().getAsInt(),
                            start.getHour().getAsInt(),
                            start.getMinute().getAsInt()));
                    sb.append("/");
                    sb.append(String.format("%02d%02d%02d",
                            end.getDay().getAsInt(),
                            end.getHour().getAsInt(),
                            end.getMinute().getAsInt()));
                    return Optional.of(createLexeme(sb.toString(), AIRMET_CANCEL));
                }
            }
            return Optional.empty();
        }
    }
}
