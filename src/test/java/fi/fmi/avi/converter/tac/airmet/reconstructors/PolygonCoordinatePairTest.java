package fi.fmi.avi.converter.tac.airmet.reconstructors;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.token.PolygonCoordinatePair;
import fi.fmi.avi.model.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.immutable.PointGeometryImpl;
import fi.fmi.avi.model.immutable.TacOrGeoGeometryImpl;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.immutable.AIRMETImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class PolygonCoordinatePairTest {

    @Autowired
    private LexingFactory lexingFactory;

    private AIRMET initPoint(double lat, double lon){
        AIRMETImpl.Builder bldr = AIRMETImpl.builder();
        PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();
        TacOrGeoGeometryImpl.Builder geometryBuilder = new TacOrGeoGeometryImpl.Builder();
        PointGeometryImpl.Builder pointBuilder = PointGeometryImpl.builder();
        pointBuilder.addCoordinates(lat, lon);
        geometryBuilder.setGeoGeometry(pointBuilder.build());
        phenBuilder.setGeometry(geometryBuilder.build());
        bldr.setAnalysisGeometries(Arrays.asList(phenBuilder.buildPartial()));
        return bldr.buildPartial();
    }

    private AIRMET msg;
    private ReconstructorContext<AIRMET> ctx;

    @Test
    public void shouldBeCase1() throws Exception {
        AIRMET airmet = initPoint(52, 5.2);
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", Integer.valueOf(0));

        final PolygonCoordinatePair.Reconstructor reconstructor = new PolygonCoordinatePair.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(airmet, AIRMET.class, ctx);
        assert(lexemes.size()>0);
        assertEquals("N52 E00512", lexemes.get(0).getTACToken());
    }

    @Test
    public void shouldBeCase2() throws Exception {
        AIRMET airmet = initPoint(52.5, 5.8);
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", Integer.valueOf(0));

        final PolygonCoordinatePair.Reconstructor reconstructor = new PolygonCoordinatePair.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(airmet, AIRMET.class, ctx);
        assertEquals("N5230 E00548", lexemes.get(0).getTACToken());
    }

    @Test
    public void shouldBeCase3() throws Exception {
        AIRMET airmet = initPoint(52.98, 5.995);
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", Integer.valueOf(0));

        final PolygonCoordinatePair.Reconstructor reconstructor = new PolygonCoordinatePair.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(airmet, AIRMET.class, ctx);
        assertEquals("N5259 E006", lexemes.get(0).getTACToken());
    }

    @Test
    public void shouldBeCase4() throws Exception {
        AIRMET airmet = initPoint(-52.98, -5.995);
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", Integer.valueOf(0));

        final PolygonCoordinatePair.Reconstructor reconstructor = new PolygonCoordinatePair.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(airmet, AIRMET.class, ctx);
        assertEquals("S5259 W006", lexemes.get(0).getTACToken());
    }

    @Test
    public void shouldBeCase5() throws Exception {
        AIRMET airmet = initPoint(-52.00, -5.00);

        ConversionHints hints = new ConversionHints();

        // No hint about specifying zeros for minutes in coordinates (default behaviour) S52 W005
        ctx = new ReconstructorContext<>(msg, hints);
        ctx.setParameter("analysisIndex", Integer.valueOf(0));

        final PolygonCoordinatePair.Reconstructor reconstructor = new PolygonCoordinatePair.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(airmet, AIRMET.class, ctx);
        assertEquals("S52 W005", lexemes.get(0).getTACToken());

        // Hint to specify zeros for minutes in coordinates: S5200 W00500
        hints.put(ConversionHints.KEY_COORDINATE_MINUTES, ConversionHints.VALUE_COORDINATE_MINUTES_INCLUDE_ZERO);
        ctx = new ReconstructorContext<>(msg, hints);
        ctx.setParameter("analysisIndex", Integer.valueOf(0));

        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexemes2 = reconstructor.getAsLexemes(airmet, AIRMET.class, ctx);
        assertEquals("S5200 W00500", lexemes2.get(0).getTACToken());

        // Hint to drop zeros for minutes in coordinates: S52 W005
        hints.put(ConversionHints.KEY_COORDINATE_MINUTES, ConversionHints.VALUE_COORDINATE_MINUTES_OMIT_ZERO);
        ctx = new ReconstructorContext<>(msg, hints);
        ctx.setParameter("analysisIndex", Integer.valueOf(0));

        final List<Lexeme> lexemes3 = reconstructor.getAsLexemes(airmet, AIRMET.class, ctx);
        assertEquals("S52 W005", lexemes3.get(0).getTACToken());


    }
}
