package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetForecastAt extends RegexMatchingLexemeVisitor {

    public SigmetForecastAt(final OccurrenceFrequency prio) {
        super("^FCST\\s+AT\\s+([0-9]{2})([0-9]{2})Z$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (LexemeIdentity.SIGMET_START.equals(token.getFirst().getIdentity()) ||
                LexemeIdentity.AIRMET_START.equals(token.getFirst().getIdentity())) {
            boolean afterLMC = false;
            Lexeme l = token.getPrevious();
            while (l != null) {
                if (LexemeIdentity.SIGMET_LEVEL.equals(l.getIdentity()) ||
                        LexemeIdentity.SIGMET_MOVING.equals(l.getIdentity()) ||
                        LexemeIdentity.SIGMET_INTENSITY.equals(l.getIdentity())) {
                    afterLMC = true;
                    break;
                }
                l = l.getPrevious();
            }
            if (afterLMC) {
                token.identify(LexemeIdentity.SIGMET_FCST_AT);
                if ((match.group(1) != null) && (!match.group(1).isEmpty()) &&
                        (match.group(2) != null) && (!match.group(2).isEmpty())) {
                    token.setParsedValue(HOUR1, Integer.valueOf(match.group(1)));
                    token.setParsedValue(MINUTE1, Integer.valueOf(match.group(2)));
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                final SIGMET sigmet = (SIGMET) msg;
                final int forecastIndex = ctx.getMandatoryParameter("forecastIndex", Integer.class);
                final String forecastTimeString = sigmet.getForecastGeometries()
                        .flatMap(geometries -> geometries.get(forecastIndex).getTime())
                        .filter(time -> time.getHour().isPresent() && time.getMinute().isPresent())
                        .map(time -> String.format(Locale.US, " AT %02d%02dZ", time.getHour().getAsInt(), time.getMinute().getAsInt()))
                        .orElse("");
                return Optional.of(this.createLexeme("FCST" + forecastTimeString, LexemeIdentity.OBS_OR_FORECAST));
            }

            return Optional.empty();
        }
    }
}
