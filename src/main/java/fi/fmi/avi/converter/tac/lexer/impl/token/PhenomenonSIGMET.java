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

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.SIGMET_PHENOMENON;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.PHENOMENON_SIGMET;


/**
 * Created by rinne on 10/02/17.
 */
public class PhenomenonSIGMET extends RegexMatchingLexemeVisitor {
    static String regex= "^(OBSC\\sTS|OBSC\\sTSGR|EMBD\\sTS|EMBD\\sTSGR|FRQ\\sTS|FRQ\\sTSGR|SQL\\sTS||SQL\\sTSGR"+
                "|SEV\\sTURB|SEV\\sICE|SEV\\sICE\\s\\(FZRA\\)|SEV\\sMTW|HVY\\sDS|HVY\\sSS|RDOACT\\sCLD|VA\\sCLD)";
    public PhenomenonSIGMET(final OccurrenceFrequency prio) {
            super(regex, prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(PHENOMENON_SIGMET);
        String m=match.group(1);
        if (m.equals("OBSC TS")) {
            token.setParsedValue(SIGMET_PHENOMENON, "OBSC_TS");
        } else if (m.equals("EMBD TS")) {
            token.setParsedValue(SIGMET_PHENOMENON, "EMBD_TS");
        } else if (m.equals("FRQ TS")) {
            token.setParsedValue(SIGMET_PHENOMENON, "FRQ_TS");
        } else if (m.equals("SQL TS")) {
            token.setParsedValue(SIGMET_PHENOMENON, "SQL_TS");
        } else if (m.equals("OBSC TSGR")) {
            token.setParsedValue(SIGMET_PHENOMENON, "OBSC_TSGR");
        } else if (m.equals("EMBD TSGR")) {
            token.setParsedValue(SIGMET_PHENOMENON, "EMBD_TSGR");
        } else if (m.equals("FRQ TSGR")) {
            token.setParsedValue(SIGMET_PHENOMENON, "FRQ_TSGR");
        } else if (m.equals("SQL TSGR")) {
            token.setParsedValue(SIGMET_PHENOMENON, "SQL_TSGR");

        } else if (m.equals("SEV TURB")) {
            token.setParsedValue(SIGMET_PHENOMENON, "SEV_TURB");
        } else if (m.equals("SEV ICE")) {
            token.setParsedValue(SIGMET_PHENOMENON, "SEV_ICE");
        } else if (m.equals("SEV ICE (FZRA)")) {
            token.setParsedValue(SIGMET_PHENOMENON, "SEV_ICE_FZRA");
        } else if (m.equals("SEV MTW")) {
            token.setParsedValue(SIGMET_PHENOMENON, "SEV_MTW");
        } else if (m.equals("HVY DS")) {
            token.setParsedValue(SIGMET_PHENOMENON, "HVY_DS");
        } else if (m.equals("HVY SS")) {
            token.setParsedValue(SIGMET_PHENOMENON, "HVY_SS");
        } else if (m.equals("RDOACT CLD")) {
            token.setParsedValue(SIGMET_PHENOMENON, "RDOACT_CLD");
        } else if (m.equals("VA CLD")) {
            token.setParsedValue(SIGMET_PHENOMENON, "VA_CLD");
        }
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

                }
            }
            return Optional.empty();
        }
    }
}
