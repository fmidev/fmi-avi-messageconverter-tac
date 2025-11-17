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
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

public class SWXEffectAndIntensity extends RegexMatchingLexemeVisitor {
    public SWXEffectAndIntensity(final OccurrenceFrequency prio) {
        super("^(?<effect>(SATCOM|HF\\sCOM|GNSS|RADIATION){1})\\s(?<intensity>(MOD|SEV){1})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_EFFECT_AND_INTENSITY);
        token.setParsedValue(Lexeme.ParsedValueName.PHENOMENON, match.group("effect"));
        token.setParsedValue(Lexeme.ParsedValueName.INTENSITY, match.group("intensity"));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(
                final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                return getAsLexemes((SpaceWeatherAdvisoryAmd79) msg);
            }
            return Collections.emptyList();
        }

        private List<Lexeme> getAsLexemes(final SpaceWeatherAdvisoryAmd79 msg) throws SerializingException {
            final List<Lexeme> lexemes = new ArrayList<>();
            final List<SpaceWeatherPhenomenon> phenomena = msg.getPhenomena();

            if (phenomena.isEmpty()) {
                throw new SerializingException("There are no space weather phenomena");
            }

            int index = 0;
            for (final SpaceWeatherPhenomenon phenomenon : phenomena) {
                if (index > 0) {
                    lexemes.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
                    lexemes.add(this.createLexeme("AND", LexemeIdentity.SWX_EFFECT_CONJUCTION, Lexeme.Status.OK));
                    lexemes.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
                }
                final Lexeme lexeme = this.createLexeme(phenomenon.asCombinedCode(), LexemeIdentity.SWX_EFFECT_AND_INTENSITY);
                lexemes.add(lexeme);
                index++;
            }
            return lexemes;
        }
    }
}
