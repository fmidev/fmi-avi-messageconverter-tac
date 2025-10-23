package fi.fmi.avi.converter.tac.swx.amd82;

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
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.swx.amd82.immutable.AdvisoryNumberImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAmd82Impl;
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
import static org.junit.Assert.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd82ReconstructorTest {

    @Autowired
    private LexingFactory lexingFactory;

    private SpaceWeatherAdvisoryAmd82 msg;
    private ReconstructorContext<SpaceWeatherAdvisoryAmd82> ctx;

    private static String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd82ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Before
    public void setUp() throws Exception {
        final ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

        final String input = getInput("spacewx-A2-3.json");
        msg = objectMapper.readValue(input, SpaceWeatherAdvisoryAmd82Impl.class);
        ctx = new ReconstructorContext<>(msg, new ConversionHints());

    }

    @Test
    public void advisoryNumberReconstructorTest() throws Exception {
        final AdvisoryNumber.Reconstructor reconstructor = new AdvisoryNumber.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
        assertEquals("2016/2", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherCenterReconstructorTest() throws Exception {
        final SWXCenter.Reconstructor reconstructor = new SWXCenter.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
        assertEquals("DONLON", lexeme.get().getTACToken());
    }

    @Test
    public void spaceWeatherEffectReconstructorTest() throws Exception {
        final SWXEffectAndIntensity.Reconstructor reconstructor = new SWXEffectAndIntensity.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexeme = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
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
        final List<Lexeme> lexeme = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
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
            final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd82.class, ctx);

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
            final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
            assertEquals(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, lexeme.get().getIdentity());
            lexList.add(lexeme.get());
        }

        assertEquals("08/0100Z", lexList.get(0).getTACToken());
        assertEquals("08/0700Z", lexList.get(1).getTACToken());
        assertEquals("08/1300Z", lexList.get(2).getTACToken());
        assertEquals("08/1900Z", lexList.get(3).getTACToken());
        assertEquals("09/0100Z", lexList.get(4).getTACToken());
    }

    @Test
    public void replaceNumberLabelReconstructorTest() {
        final ReplaceAdvisoryNumberLabel.Reconstructor reconstructor = new ReplaceAdvisoryNumberLabel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final Optional<Lexeme> label = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd82.class, ctx);

        assertEquals("NR RPLC:", label.get().getTACToken());
        assertEquals(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL, label.get().getIdentity());
    }

    @Test
    public void replaceNumberReconstructorTest() throws Exception {
        final SpaceWeatherAdvisoryAmd82 advisory = SpaceWeatherAdvisoryAmd82Impl.Builder.from(msg)
                .clearReplaceAdvisoryNumbers()
                .addReplaceAdvisoryNumbers(AdvisoryNumberImpl.builder().setYear(2020).setSerialNumber(13).build(),
                        AdvisoryNumberImpl.builder().setYear(2020).setSerialNumber(14).build())
                .build();
        final ReconstructorContext<SpaceWeatherAdvisoryAmd82> context = new ReconstructorContext<>(advisory, new ConversionHints());

        final ReplaceAdvisoryNumber.Reconstructor reconstructor = new ReplaceAdvisoryNumber.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final List<Lexeme> replaceNumbers = reconstructor.getAsLexemes(advisory, SpaceWeatherAdvisoryAmd82.class, context);

        assertFalse(replaceNumbers.isEmpty());
        assertEquals(LexemeIdentity.REPLACE_ADVISORY_NUMBER, replaceNumbers.get(0).getIdentity());
        assertEquals("2020/13", replaceNumbers.get(0).getTACToken());
        assertEquals(LexemeIdentity.WHITE_SPACE, replaceNumbers.get(1).getIdentity());
        assertEquals(" ", replaceNumbers.get(1).getTACToken());
        assertEquals(LexemeIdentity.REPLACE_ADVISORY_NUMBER, replaceNumbers.get(2).getIdentity());
        assertEquals("2020/14", replaceNumbers.get(2).getTACToken());
    }

    @Test
    public void noReplaceNumberReconstructorTest() {
        final SpaceWeatherAdvisoryAmd82 noReplaceNumbers = SpaceWeatherAdvisoryAmd82Impl.Builder.from(msg)
                .clearReplaceAdvisoryNumbers()
                .build();
        final ReconstructorContext<SpaceWeatherAdvisoryAmd82> context = new ReconstructorContext<>(noReplaceNumbers, new ConversionHints());

        final ReplaceAdvisoryNumberLabel.Reconstructor reconstructor = new ReplaceAdvisoryNumberLabel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final Optional<Lexeme> label = reconstructor.getAsLexeme(noReplaceNumbers, SpaceWeatherAdvisoryAmd82.class, context);
        assertFalse(label.isPresent());
    }

}
