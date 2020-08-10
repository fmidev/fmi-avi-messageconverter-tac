package fi.fmi.avi.converter.tac.swx;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryNumber;

import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomenaTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXCenter;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXEffect;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXPhenomena;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXPresetLocation;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;

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
        ctx = new ReconstructorContext<>(msg, new ConversionHints());

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
        Assert.assertEquals("2016/2", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherCenterReconstructorTest() throws Exception {
        SWXCenter.Reconstructor reconstructor = new SWXCenter.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisory.class, ctx);
        Assert.assertEquals("DONLON Weather Forecast Center", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherEffectReconstructorTest() throws Exception {
        SWXEffect.Reconstructor reconstructor = new SWXEffect.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        List<Lexeme> lexeme = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisory.class, ctx);
        Assert.assertEquals("HF COM MOD", lexeme.get(0).getTACToken());
        Assert.assertEquals("AND", lexeme.get(2).getTACToken());
        Assert.assertEquals("GNSS MOD", lexeme.get(4).getTACToken());
    }

    @Test
    public void spaceWeatherPresetLocationReconstructorTest() throws Exception {
        SWXPresetLocation.Reconstructor reconstructor = new SWXPresetLocation.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        //ctx.setHint(ConversionHints.KEY_SWX_ANALYSIS_INDEX, 0);
        ctx.setParameter("analysisIndex", 0);
        List<Lexeme> lexeme = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisory.class, ctx);
        Assert.assertEquals("HNH",lexeme.get(0).getTACToken());
        Assert.assertEquals("HSH",lexeme.get(2).getTACToken());

    }

    @Test
    public void AdvisoryPhenomenaReconstructorTest() throws Exception {
        List<Lexeme> lexList = new ArrayList<>();

        SWXPhenomena.Reconstructor reconstructor = new SWXPhenomena.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        for(int i = 0; i < msg.getAnalyses().size(); i++) {
            ctx.setParameter("analysisIndex", i);
            Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisory.class, ctx);

            Assert.assertEquals(lexeme.get().getIdentity(), LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
            lexList.add(lexeme.get());
        }

        Assert.assertEquals("OBS SWX:", lexList.get(0).getTACToken());
        Assert.assertEquals("FCST SWX +6 HR:", lexList.get(1).getTACToken());
        Assert.assertEquals("FCST SWX +12 HR:", lexList.get(2).getTACToken());
        Assert.assertEquals("FCST SWX +18 HR:", lexList.get(3).getTACToken());
        Assert.assertEquals("FCST SWX +24 HR:", lexList.get(4).getTACToken());
    }

    @Test
    public void issueTimeReconstructorTest() throws Exception {
        AdvisoryPhenomenaTimeGroup.Reconstructor reconstructor = new AdvisoryPhenomenaTimeGroup.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        List<Lexeme> lexList = new ArrayList<>();

        for(int i = 0; i < msg.getAnalyses().size(); i++) {
            ctx.setParameter("analysisIndex", i);
            Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisory.class, ctx);
            Assert.assertEquals(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, lexeme.get().getIdentity());
            lexList.add(lexeme.get());
        }

        Assert.assertEquals("08/0100Z", lexList.get(0).getTACToken());
        Assert.assertEquals("08/0700Z", lexList.get(1).getTACToken());
        Assert.assertEquals("08/1300Z", lexList.get(2).getTACToken());
        Assert.assertEquals("08/1900Z", lexList.get(3).getTACToken());
        Assert.assertEquals("09/0100Z", lexList.get(4).getTACToken());
    }
}
