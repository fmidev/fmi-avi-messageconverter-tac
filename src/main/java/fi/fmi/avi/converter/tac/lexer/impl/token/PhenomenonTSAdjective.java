package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TS_ADJECTIVE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.PHENOMENON_TS;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.PHENOMENON_TS_ADJECTIVE;

/**
 * Created by rinne on 10/02/17.
 */
public class PhenomenonTSAdjective extends PrioritizedLexemeVisitor {

    public PhenomenonTSAdjective(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {

        String tok=token.getTACToken();
        if ("OBSC".equals(tok)) {
            token.identify(PHENOMENON_TS_ADJECTIVE);
            token.setParsedValue(TS_ADJECTIVE, "obscured");
        } else if ("EMBD".equals(tok)) {
            token.identify(PHENOMENON_TS_ADJECTIVE);
            token.setParsedValue(TS_ADJECTIVE, "embedded");
        } else if ("FRQ".equals(tok)) {
            token.identify(PHENOMENON_TS_ADJECTIVE);
            token.setParsedValue(TS_ADJECTIVE, "frequent");
        } else if ("SQL".equals(tok)) {
            token.identify(PHENOMENON_TS_ADJECTIVE);
            token.setParsedValue(TS_ADJECTIVE, "squall");
        }
        return;
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            ArrayList<AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon> tsPhenomena= new ArrayList<>(Arrays.asList(
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.EMBD_TS,
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.EMBD_TSGR,
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.OBSC_TS,
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.OBSC_TSGR,
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.SQL_TS,
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.SQL_TSGR,
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.FRQ_TS,
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.FRQ_TSGR
            ));
           if (SIGMET.class.isAssignableFrom(clz)) {
                if (tsPhenomena.contains(((SIGMET) msg).getSigmetPhenomenon())) {
                    SIGMET sigmet = (SIGMET) msg;
                    String phen=sigmet.getSigmetPhenomenon().getText();
                    String token=phen.replace("_TSGR", "").replace("_TS", "");
                    return Optional.of(this.createLexeme(token, PHENOMENON_TS));
                }
            }
            return Optional.empty();
        }
    }
}
