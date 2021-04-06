package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.SigmetAnalysisType;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.IS_FORECAST;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.PHENOMENON_SIGMET;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_START;
/**
 * Created by rinne on 10/02/17.
 */
public class ObsOrForecast extends RegexMatchingLexemeVisitor {

    public ObsOrForecast(final OccurrenceFrequency prio) {
        super("^(OBS|FCST)(\\sAT\\s([0-9]{4})Z)?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious()&&PHENOMENON_SIGMET.equals(token.getPrevious().getIdentity())&&
            SIGMET_START.equals(token.getFirst().getIdentity())) {
            token.identify(LexemeIdentity.OBS_OR_FORECAST);
            token.setParsedValue(IS_FORECAST, !("OBS".equals(match.group(1))));
            if (match.group(3)!=null) {}
                token.setParsedValue(VALUE, match.group(3));
            return;
        }

    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET m = (SIGMET) msg;
                if (SigmetAnalysisType.OBSERVATION.equals(m.getAnalysisType())) {
                    return Optional.of(this.createLexeme("OBS", LexemeIdentity.OBS_OR_FORECAST));
                } else if (SigmetAnalysisType.FORECAST.equals(m.getAnalysisType())) {
                    return Optional.of(this.createLexeme("FCST", LexemeIdentity.OBS_OR_FORECAST));
                }
            }
            return Optional.empty();
        }
    }
}
