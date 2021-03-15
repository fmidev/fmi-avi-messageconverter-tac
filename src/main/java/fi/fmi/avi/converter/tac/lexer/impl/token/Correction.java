package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.CORRECTION;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;
import fi.fmi.avi.model.taf.TAF;

/**
 * Created by rinne on 10/02/17.
 */
public class Correction extends PrioritizedLexemeVisitor {

    public Correction(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getPrevious() == token.getFirst() && "COR".equalsIgnoreCase(token.getTACToken())) {
            token.identify(CORRECTION);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {

            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                if (AviationCodeListUser.MetarStatus.CORRECTION == ((MeteorologicalTerminalAirReport) msg).getStatus()) {
                    return Optional.of(this.createLexeme("COR", CORRECTION));
                }
            } else if (TAF.class.isAssignableFrom(clz)) {
                if (AviationWeatherMessage.ReportStatus.CORRECTION == ((TAF) msg).getReportStatus().orElse(null)) {
                    return Optional.of(this.createLexeme("COR", CORRECTION));
                }
            }
            return Optional.empty();
        }
    }
}
