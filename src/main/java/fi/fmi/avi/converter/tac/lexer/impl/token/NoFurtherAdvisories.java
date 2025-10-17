package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.NextAdvisory;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.NEXT_ADVISORY;

public class NoFurtherAdvisories extends RegexMatchingLexemeVisitor {
    public NoFurtherAdvisories(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^NO\\sFURTHER\\sADVISORIES$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {

        if (token != null && token.hasPrevious()) {
            if (token.getPrevious().getIdentity() != null && token.getPrevious().getIdentity().equals(LexemeIdentity.NEXT_ADVISORY_LABEL)) {
                token.identify(NEXT_ADVISORY);
                token.setParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.NO_FURTHER_ADVISORIES);
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                retval = Optional.of(this.createLexeme("NO FURTHER ADVISORIES", NEXT_ADVISORY));
            }
            return retval;
        }
    }
}
