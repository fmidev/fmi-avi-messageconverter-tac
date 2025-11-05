package fi.fmi.avi.converter.tac.lexer.impl.util;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.model.immutable.PointGeometryImpl;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;


public class GeometryHelper {
    private static final Set<String> LATITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lat", "latitude")));
    private static final Set<String> LONGITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lon", "long", "longitude")));

    public static List<Lexeme> getCoordinateString(final BigDecimal lat, final BigDecimal lon, final boolean lastPair, final BiFunction<String, LexemeIdentity, Lexeme> createLexeme, final boolean specifyZeros) {
        final List<Lexeme> lexemes = new ArrayList<>();
        final StringBuilder latBuilder = new StringBuilder();
        final StringBuilder lonBuilder = new StringBuilder();

        final MathContext mc = new MathContext(2, RoundingMode.HALF_UP);
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
            latBuilder.append(String.format(Locale.US, "%02d", lat.abs().intValue()));
            lonBuilder.append(String.format(Locale.US, "%03d", lon.abs().intValue()));
            if (specifyZeros || (latDecimalPart.compareTo(BigDecimal.ZERO) != 0)) {
                latBuilder.append(String.format(Locale.US, "%02.0f", latDecimalPart.abs().multiply(BigDecimal.valueOf(60d)).round(mc)));
            }
            if ((specifyZeros || lonDecimalPart.compareTo(BigDecimal.ZERO) != 0)) {
                lonBuilder.append(String.format(Locale.US, "%02.0f", lonDecimalPart.abs().multiply(BigDecimal.valueOf(60d)).round(mc)));
            }
            lexemes.add(createLexeme.apply(latBuilder + MeteorologicalBulletinSpecialCharacter.SPACE.getContent()
                    + lonBuilder, LexemeIdentity.POLYGON_COORDINATE_PAIR));
            if (!lastPair) {
                lexemes.add(createLexeme.apply(MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                        LexemeIdentity.WHITE_SPACE));
                lexemes.add(createLexeme.apply("-", LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR));
                lexemes.add(createLexeme.apply(MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                        LexemeIdentity.WHITE_SPACE));
            }
        } else {
            // TODO
        }
        return lexemes;
    }

    public static List<Lexeme> getGeoLexemes(final Geometry geom, final BiFunction<String, LexemeIdentity, Lexeme> createLexeme) {
        return getGeoLexemes(geom, createLexeme, false, 2);
    }

    public static List<Lexeme> getGeoLexemes(final Geometry geom, final BiFunction<String, LexemeIdentity, Lexeme> createLexeme, final boolean specifyZeros) {
        return getGeoLexemes(geom, createLexeme, specifyZeros, 2);
    }

    public static List<Lexeme> getGeoLexemes(final Geometry geom, final BiFunction<String, LexemeIdentity, Lexeme> createLexeme, final boolean specifyZeros, final int decimalPlaces) {
        final List<Lexeme> lexemes = new ArrayList<>();

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
        if (latOffset == -1) {
            latOffset = 0;
        }
        if (lonOffset == -1) {
            lonOffset = 1;
        }

        if (geom instanceof PolygonGeometry) {
            //Add check for WGS84 lat, lon CRS, EPSG:4326 or variants of the ID?
            final List<Double> coords = ((PolygonGeometry) geom).getExteriorRingPositions(Winding.CLOCKWISE);
            int latIndex;
            int lonIndex;
            for (int coordPairIndex = 0; coordPairIndex < coords.size() - 1; coordPairIndex = coordPairIndex + 2) {
                latIndex = coordPairIndex + latOffset;
                lonIndex = coordPairIndex + lonOffset;
                final BigDecimal lat = round(coords.get(latIndex), decimalPlaces);
                final BigDecimal lon = round(coords.get(lonIndex), decimalPlaces);
                lexemes.addAll(getCoordinateString(lat, lon, (coordPairIndex >= coords.size() - 2), createLexeme, specifyZeros));
            }
        } else if (geom instanceof PointGeometry) {
            final List<Double> coords = ((PointGeometry) geom).getCoordinates();
            final BigDecimal lat = round(coords.get(latOffset), decimalPlaces);
            final BigDecimal lon = round(coords.get(lonOffset), decimalPlaces);
            lexemes.addAll(getCoordinateString(lat, lon, true, createLexeme, specifyZeros));
        } else if (geom instanceof CircleByCenterPoint) {
            final CircleByCenterPoint circle = (CircleByCenterPoint) geom;
            final double radius = circle.getRadius().getValue();
            final String unit = circle.getRadius().getUom();
            final List<Double> coords = circle.getCenterPointCoordinates();
            final BigDecimal lat = round(coords.get(latOffset), decimalPlaces);
            final BigDecimal lon = round(coords.get(lonOffset), decimalPlaces);
            final List<Lexeme> coordinateLexemes = getCoordinateString(lat, lon, true, createLexeme, specifyZeros);
            final Lexeme circleLexeme = createLexeme.apply(String.format(Locale.US, "WI %02.0f%s OF ", radius, unit) +
                    coordinateLexemes.get(0).getTACToken(), LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT);
            lexemes.add(circleLexeme);
        }

        return lexemes;
    }

    public static PointGeometry parsePoint(final String latStr, final String lonStr) {
        double latitude;
        double longitude;
        if (latStr.length() > 3) {
            final double latitudeMinutes = Double.parseDouble(latStr.substring(3)) / 60.;
            latitude = Double.parseDouble(latStr.substring(1, 3) + ".") + latitudeMinutes;
        } else {
            latitude = Double.parseDouble(latStr.substring(1));
        }
        if (latStr.charAt(0) == 'S') {
            latitude *= -1;
        }
        if (lonStr.length() > 4) {
            final double longitudeMinutes = Double.parseDouble(lonStr.substring(4)) / 60.;
            longitude = Double.parseDouble(lonStr.substring(1, 4) + ".") + longitudeMinutes;
        } else {
            longitude = Double.parseDouble(lonStr.substring(1));
        }
        if (lonStr.charAt(0) == 'W') {
            longitude *= -1;
        }
        final PointGeometryImpl.Builder pointBuilder = PointGeometryImpl.builder();
        pointBuilder.addCoordinates(latitude, longitude);
        return pointBuilder.build();
    }

    private static BigDecimal round(final double coordinate, final int decimalPlaces) {
        return BigDecimal.valueOf(coordinate).setScale(decimalPlaces, RoundingMode.HALF_UP);
    }

}
