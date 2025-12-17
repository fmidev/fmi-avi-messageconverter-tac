package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.PointGeometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.Winding;
import fi.fmi.avi.model.immutable.*;
import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

public class GeometryHelperTest {

    private LexingFactory lexingFactory;
    private BiFunction<String, LexemeIdentity, Lexeme> lexemeCreator;

    @Before
    public void setUp() {
        lexingFactory = new LexingFactoryImpl();
        lexemeCreator = (token, identity) -> lexingFactory.createLexeme(token, identity);
    }

    @Test
    public void testCreateCoordinatePairLexemesNorthEast() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(52.5), BigDecimal.valueOf(5.8),
                false, lexemeCreator, false);

        assertThat(lexemes).hasSize(1);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N5230 E00548");
        assertThat(lexemes.get(0).getIdentity()).isEqualTo(LexemeIdentity.POLYGON_COORDINATE_PAIR);
    }

    @Test
    public void testCreateCoordinatePairLexemesSouthWest() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(-52.98), BigDecimal.valueOf(-5.995),
                false, lexemeCreator, false);

        assertThat(lexemes).hasSize(1);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("S5259 W00560");
    }

    @Test
    public void testCreateCoordinatePairLexemesZeroMinutesIncluded() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(-52.00), BigDecimal.valueOf(-5.00),
                false, lexemeCreator, true);

        assertThat(lexemes).hasSize(1);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("S5200 W00500");
    }

    @Test
    public void testCreateCoordinatePairLexemesZeroMinutesExcluded() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(-52.00), BigDecimal.valueOf(-5.00),
                false, lexemeCreator, false);

        assertThat(lexemes).hasSize(1);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("S52 W005");
    }

    @Test
    public void testCreateCoordinatePairLexemesWithSeparator() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(52.5), BigDecimal.valueOf(5.8),
                true, lexemeCreator, false);

        assertThat(lexemes).hasSize(4);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N5230 E00548");
        assertThat(lexemes.get(0).getIdentity()).isEqualTo(LexemeIdentity.POLYGON_COORDINATE_PAIR);
        assertThat(lexemes.get(1).getIdentity()).isEqualTo(LexemeIdentity.WHITE_SPACE);
        assertThat(lexemes.get(2).getTACToken()).isEqualTo("-");
        assertThat(lexemes.get(2).getIdentity()).isEqualTo(LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR);
        assertThat(lexemes.get(3).getIdentity()).isEqualTo(LexemeIdentity.WHITE_SPACE);
    }

    @Test
    public void testCreateCoordinatePairLexemesOutOfRangeLatitude() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(91.0), BigDecimal.valueOf(5.0),
                false, lexemeCreator, false);
        assertThat(lexemes).isEmpty();
    }

    @Test
    public void testCreateCoordinatePairLexemesOutOfRangeLongitude() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(52.0), BigDecimal.valueOf(181.0),
                false, lexemeCreator, false);

        assertThat(lexemes).isEmpty();
    }

    @Test
    public void testCreateCoordinatePairLexemes_negativeOutOfRange() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(-91.0), BigDecimal.valueOf(-181.0),
                false, lexemeCreator, false);

        assertThat(lexemes).isEmpty();
    }

    @Test
    public void testCreateCoordinatePairLexemesNorthPole() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(90.0), BigDecimal.valueOf(0.0),
                false, lexemeCreator, false);

        assertThat(lexemes)
                .hasSize(1)
                .first()
                .extracting(Lexeme::getTACToken)
                .isEqualTo("N90 E000");
    }

    @Test
    public void testCreateCoordinatePairLexemesSouthPole() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(-90.0), BigDecimal.valueOf(0.0),
                false, lexemeCreator, false);

        assertThat(lexemes)
                .hasSize(1)
                .first()
                .extracting(Lexeme::getTACToken)
                .isEqualTo("S90 E000");
    }

    @Test
    public void testCreateCoordinatePairLexemesPositiveDateLine() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(0.0), BigDecimal.valueOf(180.0),
                false, lexemeCreator, false);

        assertThat(lexemes)
                .hasSize(1)
                .first()
                .extracting(Lexeme::getTACToken)
                .isEqualTo("N00 E180");
    }

    @Test
    public void testCreateCoordinatePairLexemesNegativeDateLine() {
        final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(
                BigDecimal.valueOf(0.0), BigDecimal.valueOf(-180.0),
                false, lexemeCreator, false);

        assertThat(lexemes)
                .hasSize(1)
                .first()
                .extracting(Lexeme::getTACToken)
                .isEqualTo("N00 W180");
    }

    @Test
    public void testParsePointWithMinutes() {
        final PointGeometry point = GeometryHelper.parsePoint("N5230", "E00548");

        assertThat(point.getCoordinates())
                .hasSize(2)
                .containsExactly(52.5, 5.8);
    }

    @Test
    public void testParsePointWithoutMinutes() {
        final PointGeometry point = GeometryHelper.parsePoint("N52", "E005");

        assertThat(point.getCoordinates())
                .hasSize(2)
                .containsExactly(52.0, 5.0);
    }

    @Test
    public void testGetGeoLexemesPolygon() {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .addExteriorRingPositions(52.0, 5.2)
                .addExteriorRingPositions(53.0, 6.3)
                .addExteriorRingPositions(51.0, 7.18)
                .addExteriorRingPositions(52.0, 5.2)
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(polygon, lexemeCreator, false, 2, Winding.CLOCKWISE);

        assertThat(lexemes).hasSize(13);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N52 E00512");
        assertThat(lexemes.get(4).getTACToken()).isEqualTo("N53 E00618");
        assertThat(lexemes.get(8).getTACToken()).isEqualTo("N51 E00711");
        assertThat(lexemes.get(12).getTACToken()).isEqualTo("N52 E00512");
    }

    @Test
    public void testGetGeoLexemes_polygonWithZeroMinutes() {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .addExteriorRingPositions(52.0, 5.0)
                .addExteriorRingPositions(53.0, 6.0)
                .addExteriorRingPositions(52.0, 5.0)
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(polygon, lexemeCreator, true, 2, Winding.CLOCKWISE);

        assertThat(lexemes).hasSize(9);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N5200 E00500");
        assertThat(lexemes.get(4).getTACToken()).isEqualTo("N5300 E00600");
        assertThat(lexemes.get(8).getTACToken()).isEqualTo("N5200 E00500");
    }

    @Test
    public void testGetGeoLexemes_point() {
        final PointGeometry point = PointGeometryImpl.builder()
                .addCoordinates(52.5, 5.8)
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(point, lexemeCreator, false, 2, Winding.CLOCKWISE);

        assertThat(lexemes).hasSize(1);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N5230 E00548");
    }

    @Test
    public void testGetGeoLexemesCircle() {
        final CircleByCenterPoint circle = CircleByCenterPointImpl.builder()
                .setCenterPointCoordinates(Arrays.asList(52.5, 5.8))
                .setRadius(NumericMeasureImpl.of(30.0, "KM"))
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(circle, lexemeCreator, false, 2, Winding.CLOCKWISE);

        assertThat(lexemes).hasSize(1);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("WI 30KM OF N5230 E00548");
        assertThat(lexemes.get(0).getIdentity()).isEqualTo(LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT);
    }

    @Test
    public void testGetGeoLexemesWindingCW() {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .addExteriorRingPositions(52.0, 5.0)
                .addExteriorRingPositions(53.0, 5.0)
                .addExteriorRingPositions(53.0, 6.0)
                .addExteriorRingPositions(52.0, 5.0)
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(polygon, lexemeCreator, false, 2, Winding.CLOCKWISE);
        assertThat(lexemes).hasSize(13);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N52 E005");
        assertThat(lexemes.get(4).getTACToken()).isEqualTo("N53 E005");
        assertThat(lexemes.get(8).getTACToken()).isEqualTo("N53 E006");
        assertThat(lexemes.get(12).getTACToken()).isEqualTo("N52 E005");
    }

    @Test
    public void testGetGeoLexemesWindingCWEnforcedCCW() {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .addExteriorRingPositions(52.0, 5.0)
                .addExteriorRingPositions(53.0, 5.0)
                .addExteriorRingPositions(53.0, 6.0)
                .addExteriorRingPositions(52.0, 5.0)
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(polygon, lexemeCreator, false, 2, Winding.COUNTERCLOCKWISE);
        assertThat(lexemes).hasSize(13);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N52 E005");
        assertThat(lexemes.get(4).getTACToken()).isEqualTo("N53 E006");
        assertThat(lexemes.get(8).getTACToken()).isEqualTo("N53 E005");
        assertThat(lexemes.get(12).getTACToken()).isEqualTo("N52 E005");
    }

    @Test
    public void testGetGeoLexemesWindingCCW() {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .addExteriorRingPositions(52.0, 5.0)
                .addExteriorRingPositions(52.0, 6.0)
                .addExteriorRingPositions(53.0, 6.0)
                .addExteriorRingPositions(53.0, 5.5)
                .addExteriorRingPositions(52.0, 5.0)
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(polygon, lexemeCreator, false, 2, Winding.COUNTERCLOCKWISE);
        assertThat(lexemes).hasSize(17);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N52 E005");
        assertThat(lexemes.get(4).getTACToken()).isEqualTo("N52 E006");
        assertThat(lexemes.get(8).getTACToken()).isEqualTo("N53 E006");
        assertThat(lexemes.get(12).getTACToken()).isEqualTo("N53 E00530");
        assertThat(lexemes.get(16).getTACToken()).isEqualTo("N52 E005");
    }

    @Test
    public void testGetGeoLexemesWindingCCWEnforcedCW() {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .addExteriorRingPositions(52.0, 5.0)
                .addExteriorRingPositions(52.0, 6.0)
                .addExteriorRingPositions(53.0, 6.0)
                .addExteriorRingPositions(53.0, 5.5)
                .addExteriorRingPositions(52.0, 5.0)
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(polygon, lexemeCreator, false, 2, Winding.CLOCKWISE);
        assertThat(lexemes).hasSize(17);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N52 E005");
        assertThat(lexemes.get(4).getTACToken()).isEqualTo("N53 E00530");
        assertThat(lexemes.get(8).getTACToken()).isEqualTo("N53 E006");
        assertThat(lexemes.get(12).getTACToken()).isEqualTo("N52 E006");
        assertThat(lexemes.get(16).getTACToken()).isEqualTo("N52 E005");
    }

    @Test
    public void testGetGeoLexemesUnclosedUnchanged() {
        final PolygonGeometry polygon = PolygonGeometryImpl.builder()
                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                .addExteriorRingPositions(52.0, 5.0)
                .addExteriorRingPositions(52.0, 6.0)
                .addExteriorRingPositions(53.0, 6.0)
                .addExteriorRingPositions(53.0, 5.5)
                .build();

        final List<Lexeme> lexemes = GeometryHelper.getGeoLexemes(polygon, lexemeCreator, false, 2, Winding.CLOCKWISE);
        assertThat(lexemes).hasSize(13);
        assertThat(lexemes.get(0).getTACToken()).isEqualTo("N52 E005");
        assertThat(lexemes.get(4).getTACToken()).isEqualTo("N52 E006");
        assertThat(lexemes.get(8).getTACToken()).isEqualTo("N53 E006");
        assertThat(lexemes.get(12).getTACToken()).isEqualTo("N53 E00530");
    }

}