package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

public class Longitude extends RegexMatchingLexemeVisitor {
    public Longitude(final OccurrenceFrequency prio) {
        super("^(?<longitude>[WE]\\d+)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        Double longitude;
        final String lonStr = match.group("longitude");
        if (match.group("longitude").length() > 4) {
            longitude = Double.parseDouble(lonStr.substring(1, 4) + "." + lonStr.substring(4));
        } else {
            longitude = Double.parseDouble(lonStr.substring(1));
        }
        if (lonStr.charAt(0) == 'W') {
            longitude *= -1;
        }
        if (longitude >= -180.0 && longitude <= 180.0) {
            token.identify(LexemeIdentity.LONGITUDE);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, longitude);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            List<Lexeme> retval = new ArrayList<>();
            if (SIGMET.class.isAssignableFrom(clz)) {
                System.err.println("TODO");
            }
            return retval;
        }
    }
}
