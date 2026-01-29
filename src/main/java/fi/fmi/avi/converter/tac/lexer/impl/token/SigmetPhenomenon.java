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
    private static final String REGEX = "(OBSC\\s+TS|OBSC\\s+TSGR|EMBD\\s+TS|EMBD\\s+TSGR|FRQ\\s+TS|FRQ\\s+TSGR|SQL\\s+TS||SQL\\s+TSGR" +
            "|SEV\\s+TURB|SEV\\s+ICE|SEV\\s+ICE\\s+\\(FZRA\\)|SEV\\s+MTW|HVY\\s+DS|HVY\\s+SS|RDOACT\\s+CLD|VA\\s+CLD)";

    public SigmetPhenomenon(final OccurrenceFrequency prio) {
        super(REGEX, prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_PHENOMENON);
        final String m = match.group(1);
        switch (m) {
            case "OBSC TS":
                token.setParsedValue(PHENOMENON, "OBSC_TS");
                break;
            case "EMBD TS":
                token.setParsedValue(PHENOMENON, "EMBD_TS");
                break;
            case "FRQ TS":
                token.setParsedValue(PHENOMENON, "FRQ_TS");
                break;
            case "SQL TS":
                token.setParsedValue(PHENOMENON, "SQL_TS");
                break;
            case "OBSC TSGR":
                token.setParsedValue(PHENOMENON, "OBSC_TSGR");
                break;
            case "EMBD TSGR":
                token.setParsedValue(PHENOMENON, "EMBD_TSGR");
                break;
            case "FRQ TSGR":
                token.setParsedValue(PHENOMENON, "FRQ_TSGR");
                break;
            case "SQL TSGR":
                token.setParsedValue(PHENOMENON, "SQL_TSGR");
                break;
            case "SEV TURB":
                token.setParsedValue(PHENOMENON, "SEV_TURB");
                break;
            case "SEV ICE":
                token.setParsedValue(PHENOMENON, "SEV_ICE");
                break;
            case "SEV ICE (FZRA)":
                token.setParsedValue(PHENOMENON, "SEV_ICE_FZRA");
                break;
            case "SEV MTW":
                token.setParsedValue(PHENOMENON, "SEV_MTW");
                break;
            case "HVY DS":
                token.setParsedValue(PHENOMENON, "HVY_DS");
                break;
            case "HVY SS":
                token.setParsedValue(PHENOMENON, "HVY_SS");
                break;
            case "RDOACT CLD":
                token.setParsedValue(PHENOMENON, "RDOACT_CLD");
                break;
            case "VA CLD":
                token.setParsedValue(PHENOMENON, "VA_CLD");
                break;
            case "TC":
                token.setParsedValue(PHENOMENON, "TC");
                break;
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {

            if (SIGMET.class.isAssignableFrom(clz)) {
                final SIGMET sigmet = (SIGMET) msg;
                if (sigmet.getPhenomenon().isPresent()) {
                    final AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon phen = sigmet.getPhenomenon().get();
                    final String text;
                    if (AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.SEV_ICE_FZRA.equals(phen)) {
                        text = "SEV ICE (FZRA)";
                    } else if (AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA.equals(phen)) {
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



