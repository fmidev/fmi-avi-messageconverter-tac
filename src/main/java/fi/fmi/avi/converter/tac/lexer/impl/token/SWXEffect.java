package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherPhenomenon;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class SWXEffect extends RegexMatchingLexemeVisitor {
    public SWXEffect(final OccurrenceFrequency prio) {
        super("^(?<phenomenon>(SATCOM|HF\\sCOM|GNSS|RADIATION){1}\\s(MOD|SEV){1})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_EFFECT);
        final SpaceWeatherPhenomenon phenomenon = SpaceWeatherPhenomenon.fromCombinedCode(match.group("phenomenon"));
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, phenomenon);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            final List<Lexeme> retval = new ArrayList<>();

            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final List<SpaceWeatherPhenomenon> phenomena = ((SpaceWeatherAdvisoryAmd79) msg).getPhenomena();

                if (phenomena.size() < 1) {
                    throw new SerializingException("There are no space weather phenomena");
                }

                int index = 0;
                for (final SpaceWeatherPhenomenon phenomenon : phenomena) {
                    if (index > 0) {
                        retval.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

                        retval.add(this.createLexeme("AND", LexemeIdentity.SWX_EFFECT_CONJUCTION, Lexeme.Status.OK));
                        retval.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
                    }
                    final Lexeme lexeme = this.createLexeme(phenomenon.asCombinedCode(), LexemeIdentity.SWX_EFFECT);
                    retval.add(lexeme);
                    index++;
                }
            }
            return retval;
        }
    }
}
