package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AERODROME_DESIGNATOR;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
public class ICAOCode extends RegexMatchingLexemeVisitor {
    public ICAOCode(final OccurrenceFrequency prio) {
        super("^[A-Z]{4,}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        //Must be second or third token:
        if (token.getPrevious() == token.getFirst() || (token.hasPrevious() && token.getPrevious().getPrevious() == token.getFirst())) {
            token.identify(AERODROME_DESIGNATOR);
            token.setParsedValue(VALUE, token.getTACToken());
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                final MeteorologicalTerminalAirReport m = (MeteorologicalTerminalAirReport) msg;
                if (m.getAerodrome() != null) {
                    return Optional.of(this.createLexeme(m.getAerodrome().getDesignator(), AERODROME_DESIGNATOR));
                }
            } else if (TAF.class.isAssignableFrom(clz)) {
                final TAF t = (TAF) msg;
                if (t.getAerodrome() != null) {
                    return Optional.of(this.createLexeme(t.getAerodrome().getDesignator(), AERODROME_DESIGNATOR));
                }
            }
            return Optional.empty();
        }
    }
}
