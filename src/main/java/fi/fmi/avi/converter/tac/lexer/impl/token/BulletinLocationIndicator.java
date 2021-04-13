package fi.fmi.avi.converter.tac.lexer.impl.token;

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
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletin;

public class BulletinLocationIndicator extends RegexMatchingLexemeVisitor {

    public BulletinLocationIndicator(final OccurrenceFrequency prio) {
        super("^(?<code>[A-Z]{4})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.hasPrevious() && token.getPrevious().getIdentityIfAcceptable() != null && token.getPrevious()
                .getIdentityIfAcceptable()
                .equals(LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS)) {
            token.identify(LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, match.group("code"));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            final Optional<Lexeme> retval = Optional.empty();
            if (MeteorologicalBulletin.class.isAssignableFrom(clz)) {
                final BulletinHeading heading = ((MeteorologicalBulletin) msg).getHeading();
                if (heading != null) {
                    if (heading.getLocationIndicator() == null || heading.getLocationIndicator().length() != 4) {
                        throw new SerializingException("Invalid location indicator '" + heading.getLocationIndicator() + "' in TAF bulletin");
                    }
                    return Optional.of(createLexeme(heading.getLocationIndicator(), LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR));
                } else {
                    throw new SerializingException("TAF bulletin heading is null");
                }
            }
            return retval;
        }
    }
}
