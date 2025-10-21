package fi.fmi.avi.converter.tac.swx.amd79;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.token.*;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.immutable.SpaceWeatherAdvisoryAmd79Impl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd79ReconstructorTest {

    @Autowired
    private LexingFactory lexingFactory;

    private SpaceWeatherAdvisoryAmd79 msg;
    private ReconstructorContext<SpaceWeatherAdvisoryAmd79> ctx;

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

        final String input = getInput("spacewx-A2-3.json");
        msg = objectMapper.readValue(input, SpaceWeatherAdvisoryAmd79Impl.class);
        ctx = new ReconstructorContext<>(msg, new ConversionHints());

    }

    private String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd79ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Test
    public void advisoryNumberReconstructorTest() throws Exception {
        final AdvisoryNumber.Reconstructor reconstructor = new AdvisoryNumber.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd79.class, ctx);
        assertEquals("2016/2", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherCenterReconstructorTest() throws Exception {
        final SWXCenter.Reconstructor reconstructor = new SWXCenter.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd79.class, ctx);
        assertEquals("DONLON", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherEffectReconstructorTest() throws Exception {
        final SWXEffectAndIntensity.Reconstructor reconstructor = new SWXEffectAndIntensity.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexeme = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisoryAmd79.class, ctx);
        assertEquals("HF COM MOD", lexeme.get(0).getTACToken());
        assertEquals("AND", lexeme.get(2).getTACToken());
        assertEquals("GNSS MOD", lexeme.get(4).getTACToken());
    }

    @Test
    public void spaceWeatherPresetLocationReconstructorTest() {
        final SWXPresetLocation.Reconstructor reconstructor = new SWXPresetLocation.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        //ctx.setHint(ConversionHints.KEY_SWX_ANALYSIS_INDEX, 0);
        ctx.setParameter("analysisIndex", 0);
        final List<Lexeme> lexeme = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisoryAmd79.class, ctx);
        assertEquals("HNH", lexeme.get(0).getTACToken());
        assertEquals("HSH", lexeme.get(2).getTACToken());

    }

    @Test
    public void advisoryPhenomenaReconstructorTest() throws Exception {
        final List<Lexeme> lexList = new ArrayList<>();

        final SWXPhenomena.Reconstructor reconstructor = new SWXPhenomena.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        for (int i = 0; i < msg.getAnalyses().size(); i++) {
            ctx.setParameter("analysisIndex", i);
            final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd79.class, ctx);

            assertEquals(LexemeIdentity.ADVISORY_PHENOMENA_LABEL, lexeme.get().getIdentity());
            lexList.add(lexeme.get());
        }

        assertEquals("OBS SWX:", lexList.get(0).getTACToken());
        assertEquals("FCST SWX +6 HR:", lexList.get(1).getTACToken());
        assertEquals("FCST SWX +12 HR:", lexList.get(2).getTACToken());
        assertEquals("FCST SWX +18 HR:", lexList.get(3).getTACToken());
        assertEquals("FCST SWX +24 HR:", lexList.get(4).getTACToken());
    }

    @Test
    public void issueTimeReconstructorTest() throws Exception {
        final AdvisoryPhenomenaTimeGroup.Reconstructor reconstructor = new AdvisoryPhenomenaTimeGroup.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final List<Lexeme> lexList = new ArrayList<>();

        for (int i = 0; i < msg.getAnalyses().size(); i++) {
            ctx.setParameter("analysisIndex", i);
            final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd79.class, ctx);
            assertEquals(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, lexeme.get().getIdentity());
            lexList.add(lexeme.get());
        }

        assertEquals("08/0100Z", lexList.get(0).getTACToken());
        assertEquals("08/0700Z", lexList.get(1).getTACToken());
        assertEquals("08/1300Z", lexList.get(2).getTACToken());
        assertEquals("08/1900Z", lexList.get(3).getTACToken());
        assertEquals("09/0100Z", lexList.get(4).getTACToken());
    }
}
