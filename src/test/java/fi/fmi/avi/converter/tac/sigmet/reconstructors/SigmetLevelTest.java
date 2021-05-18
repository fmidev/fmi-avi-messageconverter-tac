package fi.fmi.avi.converter.tac.sigmet.reconstructors;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Optional;

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
import fi.fmi.avi.converter.tac.lexer.impl.token.SigmetLevel;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.PhenomenonGeometryWithHeightImpl;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class SigmetLevelTest {

    @Autowired
    private LexingFactory lexingFactory;

    private SIGMET initLevels(double lower, String lowerUnit, double upper, String upperUnit){
        SIGMETImpl.Builder bldr = SIGMETImpl.builder();
        PhenomenonGeometryWithHeightImpl.Builder phenBuilder = new PhenomenonGeometryWithHeightImpl.Builder();
        phenBuilder.setLowerLimit(NumericMeasureImpl.of(lower, lowerUnit));
        phenBuilder.setUpperLimit(NumericMeasureImpl.of(upper, upperUnit));
        bldr.setAnalysisGeometries(Arrays.asList(phenBuilder.buildPartial()));
        return bldr.buildPartial();
    }

    private SIGMET msg;
    private ReconstructorContext<SIGMET> ctx;

    @Test
    public void shouldBeFL_FL() throws Exception {
        SIGMET sigmet = initLevels(50, "FL", 100, "FL");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("FL050/100", lexeme.get().getTACToken());
    }
    @Test
    public void shouldBeSFC_FL() throws Exception {
        SIGMET sigmet = initLevels(0.0, "FT", 120, "FL");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("SFC/FL120", lexeme.get().getTACToken());
    }
    @Test
    public void shouldBeSFC_FT() throws Exception {
        SIGMET sigmet = initLevels(0.0, "FT", 10000, "FT");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("SFC/10000FT", lexeme.get().getTACToken());
    }
    @Test
    public void shouldBeSFC_M() throws Exception {
        SIGMET sigmet = initLevels(0.0, "FT", 1000, "M");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("SFC/1000M", lexeme.get().getTACToken());
    }

    @Test
    public void shouldBeM_M() throws Exception {
        SIGMET sigmet = initLevels(1000, "M", 2000, "M");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("1000/2000M", lexeme.get().getTACToken());
    }
    @Test
    public void shouldBeFT_FT() throws Exception {
        SIGMET sigmet = initLevels(1000.0, "FT", 10000, "FT");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("1000/10000FT", lexeme.get().getTACToken());
    }

    @Test
    public void shouldBeM_FL() throws Exception {
        SIGMET sigmet = initLevels(500, "M", 100, "FL");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("0500M/FL100", lexeme.get().getTACToken());
    }

    @Test
    public void shouldBeFT_FL() throws Exception {
        SIGMET sigmet = initLevels(500, "FT", 80, "FL");
        ctx = new ReconstructorContext<>(msg, new ConversionHints());
        ctx.setParameter("analysisIndex", new Integer(0));

        final SigmetLevel.Reconstructor reconstructor = new SigmetLevel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(sigmet, SIGMET.class, ctx);
        assertEquals("0500FT/FL080", lexeme.get().getTACToken());
    }

}
