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
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

public class Longitude extends RegexMatchingLexemeVisitor {
    public Longitude(final OccurrenceFrequency prio) {
        super("^(?<longitude>[WE]\\d+)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        System.err.println("vim:"+match.group(0));
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
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(analysisIndex.get());
                    if (analysis.getRegions() != null && analysis.getRegions().size() > 0) {
                        SpaceWeatherRegion region = analysis.getRegions().get(0);
                        if (!region.getLocationIndicator().isPresent() && region.getAirSpaceVolume().isPresent()) {
                            AirspaceVolume volume = region.getAirSpaceVolume().get();
                            if (volume.getHorizontalProjection().isPresent()) {
                                Geometry geom = volume.getHorizontalProjection().get();
                                if (geom instanceof PolygonGeometry) {
                                    //Add check for WGS84 lat, lon CRS, EPSG:4326 or variants of the ID?
                                    int latOffset = -1;
                                    int lonOffset = -1;
                                    if (geom.getAxisLabels().isPresent()) {
                                        latOffset = geom.getAxisLabels().get().indexOf("lat");
                                        if (latOffset == -1) {
                                            latOffset = geom.getAxisLabels().get().indexOf("latitude");
                                        }
                                        lonOffset = geom.getAxisLabels().get().indexOf("lon");
                                        if (lonOffset == -1) {
                                            lonOffset = geom.getAxisLabels().get().indexOf("longitude");
                                        }
                                        if (lonOffset == -1) {
                                            lonOffset = geom.getAxisLabels().get().indexOf("long");
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
                                            latBuilder.append(String.format("%03d", lat.abs().intValue()));
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
