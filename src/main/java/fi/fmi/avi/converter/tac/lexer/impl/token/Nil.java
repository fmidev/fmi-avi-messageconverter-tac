package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.NIL;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
public class Nil extends PrioritizedLexemeVisitor {

    public Nil(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getPrevious() != null && token.getPrevious().getIdentity() == ISSUE_TIME && "NIL".equalsIgnoreCase(token.getTACToken())) {
            token.identify(NIL);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                if (AviationCodeListUser.MetarStatus.MISSING == ((MeteorologicalTerminalAirReport) msg).getStatus()) {
                    return Optional.of(this.createLexeme("NIL", NIL));
                }
            } else if (TAF.class.isAssignableFrom(clz)) {
                if (AviationCodeListUser.TAFStatus.MISSING == ((TAF) msg).getStatus()) {
                    return Optional.of(this.createLexeme("NIL", NIL));
                }
            }
            return Optional.empty();
        }
    }
}
