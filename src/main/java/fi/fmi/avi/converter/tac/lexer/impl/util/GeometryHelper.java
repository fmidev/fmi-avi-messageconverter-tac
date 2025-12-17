package fi.fmi.avi.converter.tac.lexer.impl.util;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.*;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.model.immutable.PointGeometryImpl;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.BiFunction;


public class GeometryHelper {
    private static final Set<String> LATITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lat", "latitude")));
    private static final Set<String> LONGITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lon", "long", "longitude")));

    /**
     * <p>
     * Creates lexemes for a coordinate pair in TAC format (e.g., "N5230 E01015").
     * Optionally includes a trailing separator for use in coordinate lists.
     * </p>
     * <p>
     * If coordinates are out of valid range (latitude: -90 to 90, longitude: -180 to 180), an empty list is returned.
     * </p>
     *
     * @param lat                    latitude in decimal degrees
     * @param lon                    longitude in decimal degrees
     * @param includeSeparator       if true, appends whitespace and separator lexemes after the coordinate pair
     * @param createLexeme           function to create lexeme instances
     * @param includeMinutesWhenZero if true, always includes minutes even when zero (e.g., N5200 instead of N52)
     * @return list of lexemes representing the coordinate pair (and optionally separator), or an empty list if
     * coordinates are invalid
     */
    public static List<Lexeme> createCoordinatePairLexemes(final BigDecimal lat, final BigDecimal lon,
                                                           final boolean includeSeparator,
                                                           final BiFunction<String, LexemeIdentity, Lexeme> createLexeme,
                                                           final boolean includeMinutesWhenZero) {
        if (lat.doubleValue() < -90.0 || lat.doubleValue() > 90.0 || lon.doubleValue() < -180.0 || lon.doubleValue() > 180.0) {
            return Collections.emptyList();
        }

        final List<Lexeme> lexemes = new ArrayList<>();
        final String coordinatePairString = formatCoordinatePair(lat, lon, includeMinutesWhenZero);
        lexemes.add(createLexeme.apply(coordinatePairString, LexemeIdentity.POLYGON_COORDINATE_PAIR));

        if (includeSeparator) {
            addCoordinateSeparatorLexemes(lexemes, createLexeme);
        }

        return lexemes;
    }

    /**
     * Formats a coordinate pair as a TAC string.
     *
     * @param lat                    latitude in decimal degrees
     * @param lon                    longitude in decimal degrees
     * @param includeMinutesWhenZero if true, always includes minutes even when zero
     * @return formatted coordinate string
     */
    private static String formatCoordinatePair(final BigDecimal lat, final BigDecimal lon, final boolean includeMinutesWhenZero) {
        final StringBuilder latBuilder = new StringBuilder();
        final StringBuilder lonBuilder = new StringBuilder();
        final MathContext mc = new MathContext(2, RoundingMode.HALF_UP);

        latBuilder.append(lat.doubleValue() < 0 ? 'S' : 'N');
        lonBuilder.append(lon.doubleValue() < 0 ? 'W' : 'E');

        latBuilder.append(String.format(Locale.US, "%02d", lat.abs().intValue()));
        lonBuilder.append(String.format(Locale.US, "%03d", lon.abs().intValue()));

        final BigDecimal latDecimalPart = lat.subtract(BigDecimal.valueOf(lat.intValue()));
        final BigDecimal lonDecimalPart = lon.subtract(BigDecimal.valueOf(lon.intValue()));

        if (includeMinutesWhenZero || latDecimalPart.compareTo(BigDecimal.ZERO) != 0) {
            final BigDecimal latMinutes = latDecimalPart.abs().multiply(BigDecimal.valueOf(60d)).round(mc);
            latBuilder.append(String.format(Locale.US, "%02.0f", latMinutes));
        }

        if (includeMinutesWhenZero || lonDecimalPart.compareTo(BigDecimal.ZERO) != 0) {
            final BigDecimal lonMinutes = lonDecimalPart.abs().multiply(BigDecimal.valueOf(60d)).round(mc);
            lonBuilder.append(String.format(Locale.US, "%02.0f", lonMinutes));
        }

        return latBuilder + MeteorologicalBulletinSpecialCharacter.SPACE.getContent() + lonBuilder;
    }

    private static void addCoordinateSeparatorLexemes(final List<Lexeme> lexemes, final BiFunction<String, LexemeIdentity, Lexeme> createLexeme) {
        lexemes.add(createLexeme.apply(MeteorologicalBulletinSpecialCharacter.SPACE.getContent(), LexemeIdentity.WHITE_SPACE));
        lexemes.add(createLexeme.apply("-", LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR));
        lexemes.add(createLexeme.apply(MeteorologicalBulletinSpecialCharacter.SPACE.getContent(), LexemeIdentity.WHITE_SPACE));
    }

    /**
     * Creates lexemes for a geometry with a specific winding order enforced.
     *
     * @param geom          the geometry
     * @param createLexeme  function to create lexeme instances
     * @param specifyZeros  if true, always includes minutes even when zero
     * @param decimalPlaces number of decimal places for coordinate rounding
     * @param winding       the winding order to enforce
     * @return list of lexemes representing the geometry
     */
    public static List<Lexeme> getGeoLexemes(final Geometry geom,
                                             final BiFunction<String, LexemeIdentity, Lexeme> createLexeme,
                                             final boolean specifyZeros,
                                             final int decimalPlaces,
                                             final Winding winding) {
        return getGeoLexemesInternal(geom, createLexeme, specifyZeros, decimalPlaces, winding);
    }

    /**
     * Creates lexemes for a geometry, preserving the original winding order.
     * <p>
     * Use this method when the polygon coordinates should not be reordered,
     * for example when the input data is already in the correct order and
     * winding detection may be unreliable (e.g., SWX polygons crossing the antimeridian).
     * </p>
     *
     * @param geom          the geometry
     * @param createLexeme  function to create lexeme instances
     * @param specifyZeros  if true, always includes minutes even when zero
     * @param decimalPlaces number of decimal places for coordinate rounding
     * @return list of lexemes representing the geometry
     */
    public static List<Lexeme> getGeoLexemes(final Geometry geom,
                                             final BiFunction<String, LexemeIdentity, Lexeme> createLexeme,
                                             final boolean specifyZeros,
                                             final int decimalPlaces) {
        return getGeoLexemesInternal(geom, createLexeme, specifyZeros, decimalPlaces, null);
    }

    private static List<Lexeme> getGeoLexemesInternal(final Geometry geom,
                                                      final BiFunction<String, LexemeIdentity, Lexeme> createLexeme,
                                                      final boolean specifyZeros,
                                                      final int decimalPlaces,
                                                      @Nullable final Winding winding) {
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
            // TODO Add check for WGS84 lat, lon CRS, EPSG:4326 or variants of the ID?
            final PolygonGeometry polygon = (PolygonGeometry) geom;
            final List<Double> coords = winding != null
                    ? polygon.getExteriorRingPositions(winding) : polygon.getExteriorRingPositions();
            for (int coordPairIndex = 0; coordPairIndex < coords.size() - 1; coordPairIndex += 2) {
                final int latIndex = coordPairIndex + latOffset;
                final int lonIndex = coordPairIndex + lonOffset;
                final BigDecimal lat = round(coords.get(latIndex), decimalPlaces);
                final BigDecimal lon = round(coords.get(lonIndex), decimalPlaces);
                final boolean isLastPair = coordPairIndex >= coords.size() - 2;
                lexemes.addAll(createCoordinatePairLexemes(lat, lon, !isLastPair, createLexeme, specifyZeros));
            }
        } else if (geom instanceof PointGeometry) {
            final List<Double> coords = ((PointGeometry) geom).getCoordinates();
            final BigDecimal lat = round(coords.get(latOffset), decimalPlaces);
            final BigDecimal lon = round(coords.get(lonOffset), decimalPlaces);
            lexemes.addAll(createCoordinatePairLexemes(lat, lon, false, createLexeme, specifyZeros));
        } else if (geom instanceof CircleByCenterPoint) {
            final CircleByCenterPoint circle = (CircleByCenterPoint) geom;
            final double radius = circle.getRadius().getValue();
            final String unit = circle.getRadius().getUom();
            final List<Double> coords = circle.getCenterPointCoordinates();
            final BigDecimal lat = round(coords.get(latOffset), decimalPlaces);
            final BigDecimal lon = round(coords.get(lonOffset), decimalPlaces);
            final List<Lexeme> coordinateLexemes = createCoordinatePairLexemes(lat, lon, false, createLexeme, specifyZeros);
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
