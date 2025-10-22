package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.swx.amd79.AirspaceVolume;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

public class PolygonCoordinatePair extends RegexMatchingLexemeVisitor {

    public PolygonCoordinatePair(final OccurrenceFrequency prio) {
        super("^(?<latitude>[NS]\\d+)\\s+(?<longitude>[WE]\\d+)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        double latitude;
        double longitude;
        final String latStr = match.group("latitude");
        final String lonStr = match.group("longitude");
        if (latStr.length() > 3) {
            final double latitudeMinutesAsDecimal = Integer.parseInt(latStr.substring(3)) / 60.;
            latitude = Double.parseDouble(latStr.substring(1, 3) + ".") + latitudeMinutesAsDecimal;
        } else {
            latitude = Double.parseDouble(latStr.substring(1));
        }
        if (latStr.charAt(0) == 'S') {
            latitude *= -1;
        }
        if (lonStr.length() > 4) {
            final double longitudeMinutesAsDecimal = Integer.parseInt(lonStr.substring(4)) / 60.;
            longitude = Double.parseDouble(lonStr.substring(1, 4) + ".") + longitudeMinutesAsDecimal;
        } else {
            longitude = Double.parseDouble(lonStr.substring(1));
        }
        if (lonStr.charAt(0) == 'W') {
            longitude *= -1;
        }
        if (latitude >= -90.0 && latitude <= 90.0 && longitude >= -180.0 && longitude <= 180.0) {
            token.identify(LexemeIdentity.POLYGON_COORDINATE_PAIR);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE, latitude);
            token.setParsedValue(Lexeme.ParsedValueName.VALUE2, longitude);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            final ConversionHints hints = ctx.getHints();
            final boolean specifyZeros = (hints != null) && hints.containsKey(ConversionHints.KEY_COORDINATE_MINUTES) &&
                    ConversionHints.VALUE_COORDINATE_MINUTES_INCLUDE_ZERO.equals(hints.get(ConversionHints.KEY_COORDINATE_MINUTES));
            final List<Lexeme> retval = new ArrayList<>();
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisoryAmd82) msg).getAnalyses().get(analysisIndex.get());
                    if (analysis.getRegions() != null && !analysis.getRegions().isEmpty()) {
                        final fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion region = analysis.getRegions().get(0);
                        if (!region.getLocationIndicator().isPresent() && region.getAirSpaceVolume().isPresent()) {
                            final fi.fmi.avi.model.swx.amd82.AirspaceVolume volume = region.getAirSpaceVolume().get();
                            if (volume.getHorizontalProjection().isPresent()) {
                                final Geometry geom = volume.getHorizontalProjection().get();
                                retval.addAll(GeometryHelper.getGeoLexemes(geom, this::createLexeme));
                            }
                        }
                    }
                }
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisoryAmd79) msg).getAnalyses().get(analysisIndex.get());
                    if (analysis.getRegions() != null && !analysis.getRegions().isEmpty()) {
                        final SpaceWeatherRegion region = analysis.getRegions().get(0);
                        if (!region.getLocationIndicator().isPresent() && region.getAirSpaceVolume().isPresent()) {
                            final AirspaceVolume volume = region.getAirSpaceVolume().get();
                            if (volume.getHorizontalProjection().isPresent()) {
                                final Geometry geom = volume.getHorizontalProjection().get();
                                retval.addAll(GeometryHelper.getGeoLexemes(geom, this::createLexeme));
                            }
                        }
                    }
                }
            } else if (SIGMETAIRMET.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final TacOrGeoGeometry tacOrGeoGeometry = ((SIGMETAIRMET) msg).getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().get();
                    createGeometries(tacOrGeoGeometry, specifyZeros, retval);
                }
                if (SIGMET.class.isAssignableFrom(clz)) {
                    final Optional<Integer> forecastIndex = ctx.getParameter("forecastIndex", Integer.class);
                    if (forecastIndex.isPresent()) {
                        final TacOrGeoGeometry tacOrGeoGeometry = ((SIGMET) msg).getForecastGeometries().get().get(forecastIndex.get()).getGeometry().get();
                        createGeometries(tacOrGeoGeometry, specifyZeros, retval);
                    }
                }
            }
            return retval;
        }

        private void createGeometries(final TacOrGeoGeometry tacOrGeoGeometry, final boolean specifyZeros, final List<Lexeme> retval) {
            if (tacOrGeoGeometry.getGeoGeometry().isPresent() && !tacOrGeoGeometry.getTacGeometry().isPresent()) {
                final Geometry geoGeometry = tacOrGeoGeometry.getGeoGeometry().get();
                if (PointGeometry.class.isAssignableFrom(geoGeometry.getClass())) {
                    final PointGeometry pt = (PointGeometry) geoGeometry;
                    retval.addAll(GeometryHelper.getGeoLexemes(pt, this::createLexeme, specifyZeros));
                }
            }
        }
    }
}
