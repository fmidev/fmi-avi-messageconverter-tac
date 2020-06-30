package fi.fmi.avi.converter.tac.swx;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.text.html.Option;

import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.impl.token.*;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.immutable.AdvisoryNumberImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXReconstructorTest {

    @Autowired
    private LexingFactory lexingFactory;

    @Autowired
    private AviMessageConverter converter;

    private ObjectMapper OBJECT_MAPPER;
    private SpaceWeatherAdvisory msg;
    private ReconstructorContext<SpaceWeatherAdvisory> ctx;

    @Before
    public void setup() throws Exception {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

        String input = getInput("spacewx-A2-3.json");
        msg = OBJECT_MAPPER.readValue(input, SpaceWeatherAdvisoryImpl.class);

        ctx = new ReconstructorContext<>(msg);
    }

    private String getInput(String fileName) throws IOException {
        InputStream is = null;
        try {
            is = SWXReconstructorTest.class.getResourceAsStream(fileName);
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Test
    public void advisoryNumberReconstructorTest() throws Exception {
        AdvisoryNumber.Reconstructor reconstructor = new AdvisoryNumber.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisory.class, ctx);
        Assert.assertEquals("ADVISORY NR: 2/2016", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherCenterReconstructorTest() throws Exception {
        SpaceWeatherCenter.Reconstructor reconstructor = new SpaceWeatherCenter.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisory.class, ctx);
        Assert.assertEquals("SWXC: DONLON", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherEffectReconstructorTest() throws Exception {
        SpaceWeatherEffect.Reconstructor reconstructor = new SpaceWeatherEffect.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        List<Lexeme> lexeme = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisory.class, ctx);
        Assert.assertEquals("HF COM MOD", lexeme.get(0).getTACToken());
        Assert.assertEquals("AND", lexeme.get(1).getTACToken());
        Assert.assertEquals("GNSS MOD", lexeme.get(2).getTACToken());
    }

    @Test
    public void spaceWeatherPresetLocationReconstructorTest() throws Exception {
        SpaceWeatherPresetLocation.Reconstructor reconstructor = new SpaceWeatherPresetLocation.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        SpaceWeatherAdvisoryAnalysis analysis = msg.getAnalyses().get(0);
        ReconstructorContext regionCtx = new ReconstructorContext<>(analysis);
        List<Lexeme> lexeme = reconstructor.getAsLexemes(analysis, SpaceWeatherAdvisoryAnalysis.class, regionCtx);
        Assert.assertEquals("HNH",lexeme.get(0).getTACToken());
        Assert.assertEquals("HSH",lexeme.get(1).getTACToken());

    }

    @Test
    public void temp() throws Exception {
        SWXTACSerializer s = new SWXTACSerializer();
        s.setLexingFactory(this.lexingFactory);
        s.tokenizeMessage(msg);
        NextAdvisory.Reconstructor reconstructor = new NextAdvisory.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisory.class, ctx);
    }
}
