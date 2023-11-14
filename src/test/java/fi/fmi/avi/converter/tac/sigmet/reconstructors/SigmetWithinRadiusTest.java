package fi.fmi.avi.converter.tac.sigmet.reconstructors;

import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.token.SigmetWithinRadius;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.immutable.TacOrGeoGeometryImpl;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class SigmetWithinRadiusTest {

    @Autowired
    private LexingFactory lexingFactory;
    private ReconstructorContext<SIGMET> ctx;
    private SigmetWithinRadius.Reconstructor reconstructor;

    private static SIGMET initCircle(final double lat, final double lon, final double radius) {
        final SIGMETImpl.Builder builder = SIGMETImpl.builder();
        final PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();
        final TacOrGeoGeometryImpl.Builder geometryBuilder = TacOrGeoGeometryImpl.builder();
        final CircleByCenterPoint circle = CircleByCenterPointImpl.builder()
                .setRadius(NumericMeasureImpl.of(radius, "KM"))
                .setCenterPointCoordinates(Arrays.asList(lat, lon))
                .build();
        geometryBuilder.setGeoGeometry(circle);
        phenBuilder.setGeometry(geometryBuilder.build());
        builder.setAnalysisGeometries(Collections.singletonList(phenBuilder.buildPartial()));
        return builder.buildPartial();
    }

    @Before
    public void setUp() {
        ctx = new ReconstructorContext<>(null);
        ctx.setParameter("analysisIndex", 0);
        reconstructor = new SigmetWithinRadius.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
    }

    @Test
    public void circle1() {
        final SIGMET sigmet = initCircle(52, 5.2, 10);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(sigmet, SIGMET.class, ctx);
        assertEquals("WI 10KM OF N52 E00512", lexemes.get(0).getTACToken());
    }

    @Test
    public void circle2() {
        final SIGMET sigmet = initCircle(52.5, 5.8, 30);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(sigmet, SIGMET.class, ctx);
        assertEquals("WI 30KM OF N5230 E00548", lexemes.get(0).getTACToken());
    }
}
