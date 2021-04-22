package fi.fmi.avi.converter.tac.lexer.impl.util;

import java.math.BigDecimal;
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
import fi.fmi.avi.model.PolygonGeometry;

public class GeometryHelper {
    private static final Set<String> LATITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lat", "latitude")));
    private static final Set<String> LONGITUDE_AXIS_LABELS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("lon", "long", "longitude")));

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
                    lexemes.add(createLexeme.apply(
                            latBuilder.toString() + Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent()
                                    + lonBuilder.toString(), LexemeIdentity.POLYGON_COORDINATE_PAIR));
                    if (coordPairIndex < coords.size() - 2) {
                        lexemes.add(createLexeme.apply(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                                LexemeIdentity.WHITE_SPACE));
                        lexemes.add(createLexeme.apply("-", LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR));
                        lexemes.add(createLexeme.apply(Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE.getContent(),
                                LexemeIdentity.WHITE_SPACE));
                    }
                } else {
                    System.err.println("ERROR with coordinate bounds");
//                    throw new SerializingException(
//                            "Coordinate values out of latitude longitude bounds at coordinate index " + coordPairIndex + "lat:" + lat
//                                    + ", lon:" + lon);
                }
            }
        }

        return lexemes;
    }

}
