package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetForecastAt extends RegexMatchingLexemeVisitor {

    public SigmetForecastAt(final OccurrenceFrequency prio) {
        super("^FCST AT ([0-9]{2})([0-9]{2})Z$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (LexemeIdentity.SIGMET_START.equals(token.getFirst().getIdentity())||
                LexemeIdentity.AIRMET_START.equals(token.getFirst().getIdentity())) {
            boolean afterLMC=false;
            Lexeme l = token.getPrevious();
            while (l!=null) {
                if (LexemeIdentity.SIGMET_LEVEL.equals(l.getIdentity())||
                LexemeIdentity.SIGMET_MOVING.equals(l.getIdentity())||
                LexemeIdentity.SIGMET_INTENSITY.equals(l.getIdentity())) {
                    afterLMC = true;
                    break;
                }
                l=l.getPrevious();
            }
            if (afterLMC) {
                token.identify(LexemeIdentity.SIGMET_FCST_AT);
                if ((match.group(1)!=null)&&(match.group(1).length()>0)&&
                        (match.group(2)!=null)&&(match.group(1).length()>0)) {
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
                final Optional<Integer> forecastIndex = ctx.getParameter("forecastIndex", Integer.class);
                SIGMET sigmet = (SIGMET)msg;
                if (forecastIndex.isPresent()) {
                    String tim="";
                    System.err.println(">>>"+sigmet.getForecastGeometries().get().size());
                    System.err.println(">>>>"+sigmet.getForecastGeometries().get().get(forecastIndex.get()));
                    if (sigmet.getForecastGeometries().get().get(forecastIndex.get()).getTime().isPresent()) {
                        PartialOrCompleteTimeInstant t = sigmet.getForecastGeometries().get().get(0).getTime().get();
                        tim=String.format(" AT %02d%02dZ", t.getHour().getAsInt(), t.getMinute().getAsInt());
                    }
                    return Optional.of(this.createLexeme("FCST"+tim, LexemeIdentity.OBS_OR_FORECAST));
                }
            }

            return Optional.empty();
        }
    }
}
