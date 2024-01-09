package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.geoinfo.GeoUtilsTac;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT;

public class SigmetWithinRadius extends RegexMatchingLexemeVisitor {
    public SigmetWithinRadius(final OccurrenceFrequency prio) {
        super("^WI (\\d{2})(KM|NM) OF ((N|S)(\\d{2,4})) ((W|E)(\\d{3,5}))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_WITHIN_RADIUS_OF_POINT);
        token.setParsedValue(RDOACT_RADIUS, Integer.parseInt(match.group(1)));
        token.setParsedValue(RDOACT_RADIUS_UNIT, match.group(2));
        token.setParsedValue(RDOACT_LON, GeoUtilsTac.getLatLon(match.group(3)));
        token.setParsedValue(RDOACT_LAT, GeoUtilsTac.getLatLon(match.group(6)));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            final ConversionHints hints = ctx.getHints();
            final boolean specifyZeros = hints != null && hints.containsKey(ConversionHints.KEY_COORDINATE_MINUTES) &&
                    ConversionHints.VALUE_COORDINATE_MINUTES_INCLUDE_ZERO.equals(hints.get(ConversionHints.KEY_COORDINATE_MINUTES));
            if (SIGMET.class.isAssignableFrom(clz)) {
                final SIGMET sigmet = (SIGMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent() && sigmet.getAnalysisGeometries().isPresent() && analysisIndex.get() < sigmet.getAnalysisGeometries().get().size()
                        && sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().isPresent()) {
                    final TacOrGeoGeometry geom = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().get();
                    if (geom.getGeoGeometry().isPresent() && !geom.getTacGeometry().isPresent() && !geom.getEntireArea()) {
                        final Geometry geoGeometry = geom.getGeoGeometry().get();
                        if (geoGeometry instanceof CircleByCenterPoint) {
                            return GeometryHelper.getGeoLexemes(geoGeometry, this::createLexeme, specifyZeros);
                        }
                    }
                }
            }
            return Collections.emptyList();
        }
    }
}
