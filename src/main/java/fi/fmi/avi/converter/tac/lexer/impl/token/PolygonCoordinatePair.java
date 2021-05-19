package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CoordinateReferenceSystem;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

public class PolygonCoordinatePair extends RegexMatchingLexemeVisitor {
    private static final Set<String> LATITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lat", "latitude")));
    private static final Set<String> LONGITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lon", "long", "longitude")));

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
            double latitudeMinutes = Double.parseDouble(latStr.substring(3))/60.;
            latitude = Double.parseDouble(latStr.substring(1, 3) + ".") + latitudeMinutes;
        } else {
            latitude = Double.parseDouble(latStr.substring(1));
        }
        if (latStr.charAt(0) == 'S') {
            latitude *= -1;
        }
        if (lonStr.length() > 4) {
            double longitudeMinutes = Double.parseDouble(lonStr.substring(4))/60.;
            longitude = Double.parseDouble(lonStr.substring(1, 4) + ".") + longitudeMinutes;
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
            final List<Lexeme> retval = new ArrayList<>();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(analysisIndex.get());
                    if (analysis.getRegions() != null && analysis.getRegions().size() > 0) {
                        final SpaceWeatherRegion region = analysis.getRegions().get(0);
                        if (!region.getLocationIndicator().isPresent() && region.getAirSpaceVolume().isPresent()) {
                            final AirspaceVolume volume = region.getAirSpaceVolume().get();
                            if (volume.getHorizontalProjection().isPresent()) {
                                final Geometry geom = volume.getHorizontalProjection().get();
                                retval.addAll(GeometryHelper.getGeoLexemes(geom, (s,id) -> this.createLexeme(s, id)));
                            }
                        }
                    }
                }
            } else if (SIGMET.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final TacOrGeoGeometry tacOrGeoGeometry = ((SIGMET) msg).getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().get();
                    if (tacOrGeoGeometry.getGeoGeometry().isPresent()&&!tacOrGeoGeometry.getTacGeometry().isPresent()) {
                        final Geometry geoGeometry = tacOrGeoGeometry.getGeoGeometry().get();
                        if (PointGeometry.class.isAssignableFrom(geoGeometry.getClass())) {
                            PointGeometry pt = (PointGeometry)geoGeometry;
                            retval.addAll(GeometryHelper.getGeoLexemes(pt, (s,id) -> this.createLexeme(s, id)));
                        }
                    }
                }
                final Optional<Integer> forecastIndex = ctx.getParameter("forecastIndex", Integer.class);
                if (forecastIndex.isPresent()) {
                    final TacOrGeoGeometry tacOrGeoGeometry = ((SIGMET) msg).getForecastGeometries().get().get(forecastIndex.get()).getGeometry().get();
                    if (tacOrGeoGeometry.getGeoGeometry().isPresent()&&!tacOrGeoGeometry.getTacGeometry().isPresent()) {
                        final Geometry geoGeometry = tacOrGeoGeometry.getGeoGeometry().get();
                        if (PointGeometry.class.isAssignableFrom(geoGeometry.getClass())) {
                            PointGeometry pt = (PointGeometry)geoGeometry;
                            retval.addAll(GeometryHelper.getGeoLexemes(pt, (s,id) -> this.createLexeme(s, id)));
                        }
                    }
                }
            } else if (AIRMET.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final TacOrGeoGeometry tacOrGeoGeometry = ((AIRMET) msg).getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().get();
                    if (tacOrGeoGeometry.getGeoGeometry().isPresent()&&!tacOrGeoGeometry.getTacGeometry().isPresent()) {
                        final Geometry geoGeometry = tacOrGeoGeometry.getGeoGeometry().get();
                        if (PointGeometry.class.isAssignableFrom(geoGeometry.getClass())) {
                            PointGeometry pt = (PointGeometry)geoGeometry;
                            retval.addAll(GeometryHelper.getGeoLexemes(pt, (s,id) -> this.createLexeme(s, id)));
                        }
                    }
                }
            }
            return retval;
        }
    }
}
