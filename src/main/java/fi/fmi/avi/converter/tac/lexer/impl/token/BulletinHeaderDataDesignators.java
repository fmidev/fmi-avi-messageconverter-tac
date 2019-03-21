package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.MeteorologicalBulletin;

public class BulletinHeaderDataDesignators extends RegexMatchingLexemeVisitor {

    public BulletinHeaderDataDesignators(final Priority prio) {
        super("^(?<designators>[A-Z]{2}[A-Z]{2}[0-9]{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.getFirst().equals(token)) {
            token.identify(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, match.group("designators"));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (MeteorologicalBulletin.class.isAssignableFrom(clz)) {
                BulletinHeading heading = ((MeteorologicalBulletin) msg).getHeading();
                if (heading != null) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(heading.getDataTypeDesignatorsForTAC());

                    if (heading.getGeographicalDesignator() == null || heading.getGeographicalDesignator().length() != 2) {
                        throw new SerializingException("Invalid geographical location code '" + heading.getGeographicalDesignator() + "' in TAF bulletin");
                    }
                    sb.append(heading.getGeographicalDesignator());

                    if (heading.getBulletinNumber() < 0 || heading.getBulletinNumber() > 99) {
                        throw new SerializingException("Invalid bulletin number ('ii' part) '" + heading.getBulletinNumber() + "' in TAF bulletin");
                    }
                    sb.append(String.format("%02d", heading.getBulletinNumber()));
                    return Optional.of(createLexeme(sb.toString(), Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS));
                } else {
                    throw new SerializingException("TAF bulletin heading is null");
                }
            }
            return retval;
        }
    }
}
