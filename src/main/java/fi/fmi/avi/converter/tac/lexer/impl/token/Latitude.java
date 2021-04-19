package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

public class Latitude extends RegexMatchingLexemeVisitor {
    public Latitude(final OccurrenceFrequency prio) {
        super("^(?<latitude>[NS]\\d+)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        Double latitude;
        final String latStr = match.group("latitude");
        if (match.group("latitude").length() > 4) {
            latitude = Double.parseDouble(latStr.substring(1, 4) + "." + latStr.substring(4));
        } else {
            latitude = Double.parseDouble(latStr.substring(1));
        }
        if (latStr.charAt(0) == 'N') {
            latitude *= -1;
        }
        if (latitude >= -90.0 && latitude <= 90.0) {
            token.identify(LexemeIdentity.LATITUDE);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, latitude);
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
