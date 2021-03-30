package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.WITHHAIL;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.PHENOMENON_TS;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.PHENOMENON_SIGMET;


/**
 * Created by rinne on 10/02/17.
 */
public class PhenomenonSIGMET extends RegexMatchingLexemeVisitor {

    public PhenomenonSIGMET(final OccurrenceFrequency prio) {
        super("^(SEV\\sTURB|SEV\\sICE|SEV\\sMTW|HVY\\sDS|HVY\\sSS|RDOACT\\sCLD)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(PHENOMENON_SIGMET);
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
                    if (phen.endsWith("TSGR")) {
                        return Optional.of(this.createLexeme("TSGR", PHENOMENON_TS));
                    }
                    if (phen.endsWith("TS")) {
                        return Optional.of(this.createLexeme("TS", PHENOMENON_TS));
                    }
                }
            }
            return Optional.empty();
        }
    }
}
