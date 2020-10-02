package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.NEXT_ADVISORY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.NEXT_ADVISORY_LABEL;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

public class NoFurtherAdvisories extends RegexMatchingLexemeVisitor {
    public NoFurtherAdvisories(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^NO\\sFURTHER\\sADVISORIES$", prio);
    }
    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        Lexeme previous = token.getPrevious();
        while (previous != null && previous.getIdentity().equals(LexemeIdentity.WHITE_SPACE)) {
            previous = previous.getPrevious();
        }
        if (previous.getIdentity().equals(NEXT_ADVISORY_LABEL)) {
            token.identify(NEXT_ADVISORY);
            token.setParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                StringBuilder builder = new StringBuilder("NO FURTHER ADVISORIES");
                retval = Optional.of(this.createLexeme(builder.toString(), NEXT_ADVISORY));
            }
            return retval;
        }
    }
}
