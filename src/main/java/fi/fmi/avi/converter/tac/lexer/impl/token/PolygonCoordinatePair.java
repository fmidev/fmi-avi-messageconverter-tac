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
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CoordinateReferenceSystem;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.PolygonGeometry;
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
            latitude = Double.parseDouble(latStr.substring(1, 3) + "." + latStr.substring(3));
        } else {
            latitude = Double.parseDouble(latStr.substring(1));
        }
        if (latStr.charAt(0) == 'S') {
            latitude *= -1;
        }
        if (lonStr.length() > 4) {
            longitude = Double.parseDouble(lonStr.substring(1, 4) + "." + lonStr.substring(4));
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
                                if (geom instanceof PolygonGeometry) {
                                    //Add check for WGS84 lat, lon CRS, EPSG:4326 or variants of the ID?
                                    int latOffset = -1;
                                    int lonOffset = -1;
                                    final List<String> axisLabels = geom.getCrs().map(CoordinateReferenceSystem::getAxisLabels).orElse(Collections.emptyList());
                                    for (int i = axisLabels.size() - 1; i >= 0; i--) {
                                        final String axisLabel = axisLabels.get(i).toLowerCase(Locale.US);
                                        if (LATITUDE_AXIS_LABELS.contains(axisLabel)) {
                                            latOffset = i;
                                        } else if (LONGITUDE_AXIS_LABELS.contains(axisLabel)) {
                                            lonOffset = i;
                                        }
                                    }
                                    //defaults to EPSG:4326 (lat,lon) order:
                                    if (latOffset == -1) {
                                        latOffset = 0;
                                    }
                                    if (lonOffset == -1) {
                                        lonOffset = 1;
                                    }
                                    final List<Double> coords = ((PolygonGeometry) geom).getExteriorRingPositions();
                                    int latIndex;
                                    int lonIndex;
                                    for (int coordPairIndex = 0; coordPairIndex < coords.size() - 1; coordPairIndex = coordPairIndex + 2) {
                                        final StringBuilder latBuilder = new StringBuilder();
                                        final StringBuilder lonBuilder = new StringBuilder();
                                        latIndex = coordPairIndex + latOffset;
                                        lonIndex = coordPairIndex + lonOffset;
                                        final BigDecimal lat = BigDecimal.valueOf(coords.get(latIndex));
                                        final BigDecimal lon = BigDecimal.valueOf(coords.get(lonIndex));
                                        if (lat.doubleValue() >= -90.0 && lat.doubleValue() <= 90.0 && lon.doubleValue() >= -180.0
                                                && lon.doubleValue() <= 180.0) {
                                            if (lat.doubleValue() < 0) {
                                                latBuilder.append('S');
                                            } else {
                                                latBuilder.append('N');
                                            }
                                            if (lon.doubleValue() < 0) {
                                                lonBuilder.append('W');
                                            } else {
                                                lonBuilder.append('E');
                                            }
                                            final BigDecimal latDecimalPart = lat.subtract(BigDecimal.valueOf(lat.intValue()));
                                            final BigDecimal lonDecimalPart = lon.subtract(BigDecimal.valueOf(lon.intValue()));
                                            latBuilder.append(String.format("%02d", lat.abs().intValue()));
                                            lonBuilder.append(String.format("%03d", lon.abs().intValue()));
                                            if (latDecimalPart.compareTo(BigDecimal.ZERO) != 0) {
                                                latBuilder.append(String.format("%02d", latDecimalPart.abs().multiply(BigDecimal.valueOf(100d)).intValue()));
                                            }
                                            if (lonDecimalPart.compareTo(BigDecimal.ZERO) != 0) {
                                                lonBuilder.append(String.format("%02d", lonDecimalPart.abs().multiply(BigDecimal.valueOf(100d)).intValue()));
                                            }
                                            retval.add(this.createLexeme(
                                                    latBuilder.toString() + Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent()
                                                            + lonBuilder.toString(), LexemeIdentity.POLYGON_COORDINATE_PAIR));
                                            if (coordPairIndex < coords.size() - 2) {
                                                retval.add(this.createLexeme(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                                                        LexemeIdentity.WHITE_SPACE));
                                                retval.add(this.createLexeme("-", LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR));
                                                retval.add(this.createLexeme(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                                                        LexemeIdentity.WHITE_SPACE));
                                            }
                                        } else {
                                            throw new SerializingException(
                                                    "Coordinate values out of latitude longitude bounds at coordinate index " + coordPairIndex + "lat:" + lat
                                                            + ", lon:" + lon);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return retval;
        }
    }
}
