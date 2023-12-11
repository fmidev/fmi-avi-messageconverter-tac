package fi.fmi.avi.converter.tac.sigmet.reconstructors;

import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.token.PolygonCoordinatePair;
import fi.fmi.avi.model.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
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

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class PolygonCoordinatePairTest {

    @Autowired
    private LexingFactory lexingFactory;
    private ReconstructorContext<SIGMET> ctx;
    private PolygonCoordinatePair.Reconstructor reconstructor;

    private SIGMET initPoint(final double lat, final double lon) {
        SIGMETImpl.Builder bldr = SIGMETImpl.builder();
        PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();
        TacOrGeoGeometryImpl.Builder geometryBuilder = TacOrGeoGeometryImpl.builder();
        PointGeometryImpl.Builder pointBuilder = PointGeometryImpl.builder();
        pointBuilder.addCoordinates(lat, lon);
        geometryBuilder.setGeoGeometry(pointBuilder.build());
        phenBuilder.setGeometry(geometryBuilder.build());
        bldr.setAnalysisGeometries(Collections.singletonList(phenBuilder.buildPartial()));
        return bldr.buildPartial();
    }

    @Before
    public void setUp() {
        ctx = new ReconstructorContext<>(null);
        ctx.setParameter("analysisIndex", 0);
        reconstructor = new PolygonCoordinatePair.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
    }

    @Test
    public void point1() throws SerializingException {
        SIGMET sigmet = initPoint(52, 5.2);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(sigmet, SIGMET.class, ctx);
        assertEquals("N52 E00512", lexemes.get(0).getTACToken());
    }

    @Test
    public void point2() throws SerializingException {
        SIGMET sigmet = initPoint(52.5, 5.8);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(sigmet, SIGMET.class, ctx);
        assertEquals("N5230 E00548", lexemes.get(0).getTACToken());
    }

    @Test
    public void point3() throws SerializingException {
        final SIGMET sigmet = initPoint(52.56863523779511, 5.694449728936808);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(sigmet, SIGMET.class, ctx);
        assertEquals("N5234 E00541", lexemes.get(0).getTACToken());
    }

}
