package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.TAFBulletinHeading;

public class BulletinHeadingDataDesignators extends RegexMatchingLexemeVisitor {

    public BulletinHeadingDataDesignators(final Priority prio) {
        super("^(?<TT>[A-Z]{2})(?<AA>[A-Z]{2})(?<ii>[0-9]{2})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if (token.getFirst().equals(token)) {
            String type = match.group("TT");
            String geographicalDesignator = match.group("AA");
            OptionalInt sequenceNumber = OptionalInt.empty();
            boolean issuesWithSequenceNumber = false;
            if (match.group("ii") != null) {
                sequenceNumber = OptionalInt.of(Integer.parseInt(match.group("ii")));
                if (sequenceNumber.isPresent()) {
                    if (sequenceNumber.getAsInt() <= 0) {
                        token.identify(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, Lexeme.Status.SYNTAX_ERROR,
                                "Bulletin heading sequence number (ii) is non-positive:" + sequenceNumber.getAsInt());
                        issuesWithSequenceNumber = true;
                    }
                } else {
                    token.identify(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, Lexeme.Status.SYNTAX_ERROR,
                            "Bulletin heading sequence number (ii) is non-integer:" + match.group("ii"));
                    issuesWithSequenceNumber = true;
                }
            }
            if (!issuesWithSequenceNumber) {
                token.identify(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS);
                token.setParsedValue(Lexeme.ParsedValueName.TYPE, type);
                token.setParsedValue(Lexeme.ParsedValueName.COUNTRY, geographicalDesignator);
                if (sequenceNumber.isPresent()) {
                    token.setParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, sequenceNumber.getAsInt());
                }
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(T msg, Class<T> clz, ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (TAFBulletin.class.isAssignableFrom(clz)) {
                TAFBulletinHeading heading = ((TAFBulletin) msg).getHeading();
                if (heading != null) {
                    StringBuilder sb = new StringBuilder("F");
                    if (heading.isValidLessThan12Hours()) {
                        sb.append('C');
                    } else {
                        sb.append('T');
                    }
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
