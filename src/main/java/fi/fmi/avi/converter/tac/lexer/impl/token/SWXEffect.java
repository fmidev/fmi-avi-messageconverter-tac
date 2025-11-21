package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd82.Effect;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.Optional;
import java.util.regex.Matcher;

public class SWXEffect extends RegexMatchingLexemeVisitor {
    public SWXEffect(final OccurrenceFrequency prio) {
        super("^(?<effect>SATCOM|HF\\s+COM|GNSS|RADIATION)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_EFFECT);
        final String effect = Optional.ofNullable(match.group("effect"))
                .map(value -> value.replaceAll("\\s+", "_"))
                .orElse(null);
        token.setParsedValue(Lexeme.ParsedValueName.PHENOMENON, effect);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(
                final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) throws SerializingException {
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                return getAsLexeme((SpaceWeatherAdvisoryAmd82) msg);
            }
            return Optional.empty();
        }

        private Optional<Lexeme> getAsLexeme(final SpaceWeatherAdvisoryAmd82 msg) {
            final Effect effect = msg.getEffect();
            final Lexeme lexeme = createLexeme(effect.getCode().replaceAll("_", " "), LexemeIdentity.SWX_EFFECT);
            return Optional.of(lexeme);
        }
    }
}
