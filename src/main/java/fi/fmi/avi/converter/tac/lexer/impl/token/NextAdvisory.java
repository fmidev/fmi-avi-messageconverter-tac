package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.time.format.DateTimeFormatter;
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
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class NextAdvisory extends TimeHandlingRegex {
    public NextAdvisory(final OccurrenceFrequency prio) {
        super("NXT\\sADVISORY\\:\\s(?<type>NO\\sFURTHER\\sADVISORIES|WILL\\sBE\\sISSUED\\sBY)?\\s?((?<year>[0-9]{4})(?<month>[0-1][0-9])(?<day>[0-3][0-9])\\/"
                + "(?<hour>[0-2][0-9])(?<minute>[0-5][0-9])Z)?", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.NEXT_ADVISORY);

        String type = match.group("type");
        if (type == null) {
            token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_AT);
            setParsedDateValues(token, match, hints);
        } else if (type.equals("WILL BE ISSUED BY")) {
            token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_BY);
            setParsedDateValues(token, match, hints);
        } else {
            token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES);
        }
    }

    private void setParsedDateValues(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.setParsedValue(Lexeme.ParsedValueName.YEAR, Integer.parseInt(match.group("year")));
        token.setParsedValue(Lexeme.ParsedValueName.MONTH, Integer.parseInt(match.group("month")));
        token.setParsedValue(Lexeme.ParsedValueName.DAY1, Integer.parseInt(match.group("day")));
        token.setParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.parseInt(match.group("hour")));
        token.setParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.parseInt(match.group("minute")));

    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                fi.fmi.avi.model.swx.NextAdvisory nextAdvisory = ((SpaceWeatherAdvisory) msg).getNextAdvisory();
                if (nextAdvisory == null) {
                    throw new SerializingException("Next advisory is missing");
                }
                StringBuilder builder = new StringBuilder();
                builder.append("NXT ADVISORY:");
                appendWhiteSpaceToString(builder, 21);

                if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_BY)) {
                    builder.append("WILL BE ISSUED BY");
                } else if (nextAdvisory.getTimeSpecifier().equals(fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES)) {
                    builder.append("NO FURTHER ADVISORIES");
                }

                if (nextAdvisory.getTime().isPresent()) {
                    PartialOrCompleteTimeInstant time = nextAdvisory.getTime().get();
                    builder.append(time.getCompleteTime().get().format(DateTimeFormatter.ofPattern("yyyyMMdd/HHmm'Z'")));
                }

                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.NEXT_ADVISORY));
            }
            return retval;
        }
    }
}
