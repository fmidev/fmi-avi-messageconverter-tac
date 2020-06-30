package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;

public class SpaceWeatherEffect extends RegexMatchingLexemeVisitor {
    public SpaceWeatherEffect(final Priority prio) {
        super("^(?<phenomenon>(SATCOM|HF\\sCOM|GNSS|RADIATION){1}\\s(MOD|SEV){1})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_EFFECT);
        SpaceWeatherPhenomenon phenomenon = SpaceWeatherPhenomenon.fromCombinedCode(match.group("phenomenon"));
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, phenomenon);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ReconstructorContext<T> ctx)
                throws SerializingException {
            List<Lexeme> retval = new ArrayList<>();

            if(SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                List<SpaceWeatherPhenomenon> phenomena = ((SpaceWeatherAdvisory)msg).getPhenomena();

                if(phenomena.size() < 1) {
                    throw new SerializingException("There are no space weather phenomena");
                }

                int index = 0;
                for(SpaceWeatherPhenomenon phenomenon : phenomena) {
                    if(index > 0) {
                        retval.add(this.createLexeme("AND", null, Lexeme.Status.OK));
                    }
                    Lexeme lexeme = this.createLexeme(phenomenon.asCombinedCode() , LexemeIdentity.SWX_EFFECT);
                    retval.add(lexeme);
                    index++;
                }
            }
            return retval;
        }
    }
}
