package fi.fmi.avi.converter.tac.lexer.impl.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.CoordinateReferenceSystem;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.Geometry.Winding;
import fi.fmi.avi.model.immutable.PointGeometryImpl;

public class GeometryHelper {
    private static final Set<String> LATITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lat", "latitude")));
    private static final Set<String> LONGITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lon", "long", "longitude")));

    public static List<Lexeme> getCoordinateString(BigDecimal lat, BigDecimal lon, boolean lastPair, BiFunction<String, LexemeIdentity, Lexeme> createLexeme) {
        List<Lexeme> lexemes = new ArrayList<>();
        final StringBuilder latBuilder = new StringBuilder();
        final StringBuilder lonBuilder = new StringBuilder();

        MathContext mc = new MathContext(2, RoundingMode.HALF_UP);
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
                latBuilder.append(String.format("%02.0f", latDecimalPart.abs().multiply(BigDecimal.valueOf(60d)).round(mc)));
            }
            if (lonDecimalPart.compareTo(BigDecimal.ZERO) != 0) {
                lonBuilder.append(String.format("%02.0f", lonDecimalPart.abs().multiply(BigDecimal.valueOf(60d)).round(mc)));
            }
            lexemes.add(createLexeme.apply(
                    latBuilder.toString() + Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent()
                            + lonBuilder.toString(), LexemeIdentity.POLYGON_COORDINATE_PAIR));
            if (!lastPair) {
                lexemes.add(createLexeme.apply(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                        LexemeIdentity.WHITE_SPACE));
                lexemes.add(createLexeme.apply("-", LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR));
                lexemes.add(createLexeme.apply(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                        LexemeIdentity.WHITE_SPACE));
            }
        } else {

        }
        return lexemes;
    }

    // public static List<Lexeme> getCoordinateLexemes(BigDecimal lat, BigDecimal lon, boolean lastPair, BiFunction<String, LexemeIdentity, Lexeme> createLexeme) {
    //     List<Lexeme> lexemes = new ArrayList<>();
    //      lexemes.add(createLexeme.apply(getCoordinateString(lat, lon), LexemeIdentity.POLYGON_COORDINATE_PAIR));
    //     if (!lastPair) {
    //         lexemes.add(createLexeme.apply(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
    //                 LexemeIdentity.WHITE_SPACE));
    //         lexemes.add(createLexeme.apply("-", LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR));
    //         lexemes.add(createLexeme.apply(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
    //                 LexemeIdentity.WHITE_SPACE));
    //     }
    //     return lexemes;

    // }

    public static List<Lexeme> getGeoLexemes(Geometry geom, BiFunction<String, LexemeIdentity, Lexeme> createLexeme) {
        List<Lexeme> lexemes = new ArrayList<>();
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
            final List<Double> coords = ((PolygonGeometry) geom).getExteriorRingPositions(Winding.CW);
            int latIndex;
            int lonIndex;
            for (int coordPairIndex = 0; coordPairIndex < coords.size() - 1; coordPairIndex = coordPairIndex + 2) {
                latIndex = coordPairIndex + latOffset;
                lonIndex = coordPairIndex + lonOffset;
                final BigDecimal lat = BigDecimal.valueOf(coords.get(latIndex));
                final BigDecimal lon = BigDecimal.valueOf(coords.get(lonIndex));
                lexemes.addAll(getCoordinateString(lat, lon, (coordPairIndex >= coords.size() - 2), createLexeme));
            }
        } else if (geom instanceof PointGeometry) {
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
            final List<Double> coords = ((PointGeometry) geom).getCoordinates();
            final BigDecimal lat = BigDecimal.valueOf(coords.get(latOffset));
            final BigDecimal lon = BigDecimal.valueOf(coords.get(lonOffset));
            lexemes.addAll(getCoordinateString(lat, lon, true, createLexeme));
        }

        return lexemes;
    }

    public static PointGeometry parsePoint(String latStr, String lonStr) {
        double latitude;
        double longitude;
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
        PointGeometryImpl.Builder pointBuilder = PointGeometryImpl.builder();
        pointBuilder.addCoordinates(latitude, longitude);
        return pointBuilder.build();

    }
    public class Point {
        final double latitude;
        final double longitude;
        public Point(String latStr, String lonStr) {
            double lat;
            double lon;
            if (latStr.length() > 3) {
                double latitudeMinutes = Double.parseDouble(latStr.substring(3))/60.;
                lat = Double.parseDouble(latStr.substring(1, 3) + ".") + latitudeMinutes;
            } else {
                lat = Double.parseDouble(latStr.substring(1));
            }
            if (latStr.charAt(0) == 'S') {
                latitude = lat * -1;
            } else {
                latitude=lat;
            }
            if (lonStr.length() > 4) {
                double longitudeMinutes = Double.parseDouble(lonStr.substring(4))/60.;
                lon = Double.parseDouble(lonStr.substring(1, 4) + ".") + longitudeMinutes;
            } else {
                lon = Double.parseDouble(lonStr.substring(1));
            }
            if (lonStr.charAt(0) == 'W') {
                longitude = lon * -1;
            } else {
                longitude = lon;
            }
        }
        public double getLongitude() {
            return longitude;
        }

        public double getLatitude() {
            return latitude;
        }
    }

}
