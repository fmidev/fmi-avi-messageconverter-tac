package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.PHENOMENON;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_PHENOMENON;


/**
 * Created by rinne on 10/02/17.
 */
public class SigmetPhenomenon extends RegexMatchingLexemeVisitor {
    static String regex= "^(OBSC\\sTS|OBSC\\sTSGR|EMBD\\sTS|EMBD\\sTSGR|FRQ\\sTS|FRQ\\sTSGR|SQL\\sTS||SQL\\sTSGR"+
                "|SEV\\sTURB|SEV\\sICE|SEV\\sICE\\s\\(FZRA\\)|SEV\\sMTW|HVY\\sDS|HVY\\sSS|RDOACT\\sCLD|VA\\sCLD)";
    public SigmetPhenomenon(final OccurrenceFrequency prio) {
            super(regex, prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_PHENOMENON);
        String m=match.group(1);
        if (m.equals("OBSC TS")) {
            token.setParsedValue(PHENOMENON, "OBSC_TS");
        } else if (m.equals("EMBD TS")) {
            token.setParsedValue(PHENOMENON, "EMBD_TS");
        } else if (m.equals("FRQ TS")) {
            token.setParsedValue(PHENOMENON, "FRQ_TS");
        } else if (m.equals("SQL TS")) {
            token.setParsedValue(PHENOMENON, "SQL_TS");
        } else if (m.equals("OBSC TSGR")) {
            token.setParsedValue(PHENOMENON, "OBSC_TSGR");
        } else if (m.equals("EMBD TSGR")) {
            token.setParsedValue(PHENOMENON, "EMBD_TSGR");
        } else if (m.equals("FRQ TSGR")) {
            token.setParsedValue(PHENOMENON, "FRQ_TSGR");
        } else if (m.equals("SQL TSGR")) {
            token.setParsedValue(PHENOMENON, "SQL_TSGR");
        } else if (m.equals("SEV TURB")) {
            token.setParsedValue(PHENOMENON, "SEV_TURB");
        } else if (m.equals("SEV ICE")) {
            token.setParsedValue(PHENOMENON, "SEV_ICE");
        } else if (m.equals("SEV ICE (FZRA)")) {
            token.setParsedValue(PHENOMENON, "SEV_ICE_FZRA");
        } else if (m.equals("SEV MTW")) {
            token.setParsedValue(PHENOMENON, "SEV_MTW");
        } else if (m.equals("HVY DS")) {
            token.setParsedValue(PHENOMENON, "HVY_DS");
        } else if (m.equals("HVY SS")) {
            token.setParsedValue(PHENOMENON, "HVY_SS");
        } else if (m.equals("RDOACT CLD")) {
            token.setParsedValue(PHENOMENON, "RDOACT_CLD");
        } else if (m.equals("VA CLD")) {
            token.setParsedValue(PHENOMENON, "VA_CLD");
        } else if (m.equals("TC")) {
            token.setParsedValue(PHENOMENON, "TC");
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {

            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET)msg;
                if (sigmet.getSigmetPhenomenon().isPresent()) {
                    AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon phen=sigmet.getSigmetPhenomenon().get();
                    String text;
                    if (AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.SEV_ICE_FZRA.equals(phen)){
                        text = "SEV ICE (FZRA)";
                    } else if (AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA.equals(phen)){
                        text = "VA CLD";
                    } else {
                        text = phen.getText().replaceAll("_", " ");
                    }

                    return Optional.of(this.createLexeme(text, LexemeIdentity.SIGMET_PHENOMENON));
                }
            }
            return Optional.empty();
        }
    }
}



