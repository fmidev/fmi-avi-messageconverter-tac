package fi.fmi.avi.converter.tac.swx.amd82;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.TACTokenReconstructor;
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
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

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

    private List<Lexeme> getAsLexemes(final TACTokenReconstructor reconstructor) {
        try {
            return reconstructor.getAsLexemes(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
        } catch (final SerializingException exception) {
            throw new RuntimeException(exception);
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
        assertThat(lexeme.map(Lexeme::getTACToken)).hasValue("2016/2");
    }

    @Test
    public void spaceWeatherCenterReconstructorTest() throws Exception {
        final SWXCenter.Reconstructor reconstructor = new SWXCenter.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final Optional<Lexeme> lexeme = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
        assertThat(lexeme.map(Lexeme::getTACToken)).hasValue("DONLON");
    }

    @Test
    public void spaceWeatherEffectReconstructorTest() throws Exception {
        final SWXEffect.Reconstructor reconstructor = new SWXEffect.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
        assertThat(lexemes)
                .extracting(Lexeme::getTACToken)
                .containsExactly("HF COM");
    }

    @Test
    public void spaceWeatherPresetLocationReconstructorTest() throws SerializingException {
        final SWXPresetLocation.Reconstructor reconstructor = new SWXPresetLocation.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        //ctx.setHint(ConversionHints.KEY_SWX_ANALYSIS_INDEX, 0);
        ctx.setParameter("analysisIndex", 0);
        ctx.setParameter("intensityAndRegionIndex", 0);
        final List<Lexeme> lexemes = reconstructor.getAsLexemes(msg, SpaceWeatherAdvisoryAmd82.class, ctx);
        assertThat(lexemes)
                .extracting(Lexeme::getTACToken)
                .containsExactly("HNH", " ", "HSH");
    }

    @Test
    public void advisoryPhenomenaReconstructorTest() {
        final SWXPhenomena.Reconstructor reconstructor = new SWXPhenomena.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final List<List<Lexeme>> lexemeLists = IntStream.range(0, msg.getAnalyses().size())
                .mapToObj(i -> {
                    ctx.setParameter("analysisIndex", i);
                    return getAsLexemes(reconstructor);
                })
                .collect(Collectors.toList());

        assertThat(lexemeLists)
                .allSatisfy(lexemes -> assertThat(lexemes).hasSize(1));
        assertThat(lexemeLists)
                .extracting(lexemes -> lexemes.get(0))
                .allSatisfy(lexeme -> assertThat(lexeme.getIdentity())
                        .isEqualTo(LexemeIdentity.ADVISORY_PHENOMENA_LABEL));
        assertThat(lexemeLists)
                .extracting(lexemes -> lexemes.get(0).getTACToken())
                .containsExactly(
                        "OBS SWX:",
                        "FCST SWX +6 HR:",
                        "FCST SWX +12 HR:",
                        "FCST SWX +18 HR:",
                        "FCST SWX +24 HR:"
                );
    }

    @Test
    public void issueTimeReconstructorTest() {
        final AdvisoryPhenomenaTimeGroup.Reconstructor reconstructor = new AdvisoryPhenomenaTimeGroup.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final List<List<Lexeme>> lexemeLists = IntStream.range(0, msg.getAnalyses().size())
                .mapToObj(i -> {
                    ctx.setParameter("analysisIndex", i);
                    return getAsLexemes(reconstructor);
                })
                .collect(Collectors.toList());

        assertThat(lexemeLists)
                .allSatisfy(lexemes -> assertThat(lexemes).hasSize(1));
        assertThat(lexemeLists)
                .extracting(lexemes -> lexemes.get(0))
                .allSatisfy(lexeme -> assertThat(lexeme.getIdentity())
                        .isEqualTo(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP));
        assertThat(lexemeLists)
                .extracting(lexemes -> lexemes.get(0).getTACToken())
                .containsExactly(
                        "08/0100Z",
                        "08/0700Z",
                        "08/1300Z",
                        "08/1900Z",
                        "09/0100Z"
                );
    }

    @Test
    public void intensityReconstructorTest() {
        final SWXIntensity.Reconstructor reconstructor = new SWXIntensity.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final List<Lexeme> allRegionLexemes = IntStream.range(0, msg.getAnalyses().size())
                .mapToObj(analysisIndex -> {
                    ctx.setParameter("analysisIndex", analysisIndex);
                    return IntStream.range(0, msg.getAnalyses().get(analysisIndex).getIntensityAndRegions().size())
                            .mapToObj(intensityAndRegionIndex -> {
                                ctx.setParameter("intensityAndRegionIndex", intensityAndRegionIndex);
                                return getAsLexemes(reconstructor).stream();
                            });
                })
                .flatMap(Function.identity())
                .flatMap(Function.identity())
                .collect(Collectors.toList());

        assertThat(allRegionLexemes)
                .hasSize(4)
                .allSatisfy(lexeme -> {
                    assertThat(lexeme.getIdentity()).isEqualTo(LexemeIdentity.SWX_INTENSITY);
                    assertThat(lexeme.getTACToken()).isEqualTo("MOD");
                });
    }

    @Test
    public void replaceNumberLabelReconstructorTest() {
        final ReplaceAdvisoryNumberLabel.Reconstructor reconstructor = new ReplaceAdvisoryNumberLabel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        final Lexeme label = reconstructor.getAsLexeme(msg, SpaceWeatherAdvisoryAmd82.class, ctx).orElse(null);

        assertThat(label).isNotNull();
        assertThat(label.getIdentity()).isEqualTo(LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
        assertThat(label.getTACToken()).isEqualTo("NR RPLC:");
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

        assertThat(replaceNumbers).hasSize(3);
        assertThat(replaceNumbers)
                .extracting(Lexeme::getIdentity)
                .containsExactly(
                        LexemeIdentity.REPLACE_ADVISORY_NUMBER,
                        LexemeIdentity.WHITE_SPACE,
                        LexemeIdentity.REPLACE_ADVISORY_NUMBER
                );
        assertThat(replaceNumbers)
                .extracting(Lexeme::getTACToken)
                .containsExactly(
                        "2020/13",
                        " ",
                        "2020/14"
                );
    }

    @Test
    public void noReplaceNumberReconstructorTest() {
        final SpaceWeatherAdvisoryAmd82 noReplaceNumbers = SpaceWeatherAdvisoryAmd82Impl.Builder.from(msg)
                .clearReplaceAdvisoryNumbers()
                .build();
        final ReconstructorContext<SpaceWeatherAdvisoryAmd82> context = new ReconstructorContext<>(noReplaceNumbers, new ConversionHints());

        final ReplaceAdvisoryNumberLabel.Reconstructor reconstructor = new ReplaceAdvisoryNumberLabel.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);

        assertThat(reconstructor.getAsLexeme(noReplaceNumbers, SpaceWeatherAdvisoryAmd82.class, context)).isEmpty();
    }

}
