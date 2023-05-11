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
import fi.fmi.avi.model.AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_CANCEL;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetCancel extends RegexMatchingLexemeVisitor {

    public SigmetCancel(final OccurrenceFrequency prio) {
        super("^CNL SIGMET (\\w?\\d?\\d) (\\d{2})(\\d{2})(\\d{2})/(\\d{2})(\\d{2})(\\d{2})(\\sVA\\sMOV\\sTO\\s(\\w{4})\\sFIR)?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_CANCEL);
        token.setParsedValue(ParsedValueName.SEQUENCE_DESCRIPTOR, match.group(1));
        token.setParsedValue(ParsedValueName.DAY1, Integer.valueOf(match.group(2)));
        token.setParsedValue(ParsedValueName.HOUR1, Integer.valueOf(match.group(3)));
        token.setParsedValue(ParsedValueName.MINUTE1, Integer.valueOf(match.group(4)));
        token.setParsedValue(ParsedValueName.DAY2, Integer.valueOf(match.group(5)));
        token.setParsedValue(ParsedValueName.HOUR2, Integer.valueOf(match.group(6)));
        token.setParsedValue(ParsedValueName.MINUTE2, Integer.valueOf(match.group(7)));
        token.setParsedValue(ParsedValueName.MOVED_TO, match.group(9));
    }

	public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET)msg;
                if (sigmet.isCancelMessage()) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("CNL");
                    sb.append(" ");
                    sb.append("SIGMET");
                    sb.append(" ");
                    sb.append(sigmet.getCancelledReference().get().getSequenceNumber());
                    sb.append(" ");
                    PartialOrCompleteTimeInstant start = sigmet.getCancelledReference().get().getValidityPeriod().getStartTime().get();
                    PartialOrCompleteTimeInstant end = sigmet.getCancelledReference().get().getValidityPeriod().getEndTime().get();


                    sb.append(String.format(Locale.US, "%02d%02d%02d",
                            start.getDay().getAsInt(),
                            start.getHour().getAsInt(),
                            start.getMinute().getAsInt()));
                    sb.append("/");
                    sb.append(String.format(Locale.US, "%02d%02d%02d",
                            end.getDay().getAsInt(),
                            end.getHour().getAsInt(),
                            end.getMinute().getAsInt()));

                    if (sigmet.getVAInfo().isPresent()&& sigmet.getVAInfo().get().getVolcanicAshMovedToFIR().isPresent()) {
                        sb.append(" VA MOV TO ");
                        sb.append(sigmet.getVAInfo().get().getVolcanicAshMovedToFIR().get().getDesignator());
                        sb.append(" FIR");
                    }
                    return Optional.of(createLexeme(sb.toString(), SIGMET_CANCEL));
                }
            }
            return Optional.empty();
        }
    }
}
