package fi.fmi.avi.converter.tac.swx.amd82;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.amd82.*;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAmd82Impl;
import org.assertj.core.api.ListAssert;
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
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd82TACSerializerTest {
    private static final String CR_LF = CARRIAGE_RETURN.getContent() + LINE_FEED.getContent();

    @Autowired
    private AviMessageConverter converter;

    private ObjectMapper objectMapper;

    private static String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd82ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    private static ListAssert<Double> assertThatAnalysisRegionPositions(
            final List<SpaceWeatherAdvisoryAnalysis> analyses, final int analysisIndex,
            final int intensityAndRegionIndex, final int regionIndex) {
        assertThat(analyses).hasSizeGreaterThan(analysisIndex);
        final List<SpaceWeatherIntensityAndRegion> intensityAndRegions = analyses.get(analysisIndex).getIntensityAndRegions();
        assertThat(intensityAndRegions).hasSizeGreaterThan(intensityAndRegionIndex);
        final List<SpaceWeatherRegion> regions = intensityAndRegions.get(intensityAndRegionIndex).getRegions();
        assertThat(regions).hasSizeGreaterThan(regionIndex);
        final Geometry geometry = regions.get(regionIndex)
                .getAirSpaceVolume()
                .flatMap(AirspaceVolume::getHorizontalProjection)
                .orElse(null);
        assertThat(geometry)
                .isNotNull()
                .isInstanceOf(PolygonGeometry.class);
        return assertThat(((PolygonGeometry) geometry).getExteriorRingPositions());
    }

    private SpaceWeatherAdvisoryAmd82 loadAdvisory(final String fileName) throws IOException {
        final String input = getInput(fileName);
        return objectMapper.readValue(input, SpaceWeatherAdvisoryAmd82Impl.class);
    }

    @Before
    public void setup() throws Exception {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());
    }

    @Test
    public void swxSerializerTest() throws IOException {
        final SpaceWeatherAdvisoryAmd82 advisory = loadAdvisory("spacewx-A2-3.json");
        final ConversionResult<String> result = this.converter.convertMessage(advisory, TACConverter.SWX_AMD82_POJO_TO_TAC, new ConversionHints());
        assertThat(result.getConvertedMessage()).isNotEmpty();
    }

    @Test
    public void testNilRemark() {
        final String expected = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(pojoResult.getConvertedMessage().flatMap(AviationWeatherMessage::getRemarks)).isEmpty();

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(stringResult.getConvertedMessage()).hasValue(expected);
    }

    @Test
    public void testDecimalHandling() {
        final String original = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z MOD HNH HSH E020 - W17200 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E150 - W40 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD HNH HSH E5 - W160 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD HNH HSH E17930 - W02054 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MOD N80 W15006 - N1 W75 - N60 E15 - N70 E75 - N80 W15006" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final String expected = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z MOD HNH HSH E020 - W172 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E150 - W040 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD HNH HSH E005 - W160 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD HNH HSH E179 - W021 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MOD N80 W150 - N01 W075 - N60 E015 - N70 E075 - N80 W150" + CARRIAGE_RETURN.getContent()
                + LINE_FEED.getContent()//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(original, TACConverter.TAC_TO_SWX_AMD82_POJO);
        for (final ConversionIssue issue : pojoResult.getConversionIssues()) {
            System.err.println("iss:" + issue);
        }
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.WITH_WARNINGS);
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage()
                .map(SpaceWeatherAdvisoryAmd82::getAnalyses)
                .orElse(Collections.emptyList());

        assertThatAnalysisRegionPositions(analyses, 0, 0, 0)
                .containsExactly(90d, 20d, 60d, 20d, 60d, -172d, 90d, -172d, 90d, 20d);
        assertThatAnalysisRegionPositions(analyses, 1, 0, 0)
                .containsExactly(90d, 150d, 60d, 150d, 60d, -40d, 90d, -40d, 90d, 150d);
        assertThatAnalysisRegionPositions(analyses, 2, 0, 0)
                .containsExactly(90d, 5d, 60d, 5d, 60d, -160d, 90d, -160d, 90d, 5d);
        assertThatAnalysisRegionPositions(analyses, 4, 0, 0)
                .containsExactly(80d, -150.0d, 1d, -75d, 60d, 15d, 70d, 75d, 80d, -150d);

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(stringResult.getConvertedMessage()).hasValue(expected);
    }

    @Test
    public void testHemisphereExceeding180() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z MOD HNH W180 - E120" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH W020 - W100 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD EQS E180 - E090 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD EQN W060 - E060 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MOD MNH W100 - W030" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage()
                .map(SpaceWeatherAdvisoryAmd82::getAnalyses)
                .orElse(Collections.emptyList());

        assertThatAnalysisRegionPositions(analyses, 0, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 120d, 90d, 120d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 1, 0, 0)
                .containsExactly(90d, -20d, 60d, -20d, 60d, -100d, 90d, -100d, 90d, -20d);
        assertThatAnalysisRegionPositions(analyses, 2, 0, 0)
                .containsExactly(0d, -180d, -30d, -180d, -30d, 90d, 0d, 90d, 0d, -180d);
        assertThatAnalysisRegionPositions(analyses, 3, 0, 0)
                .containsExactly(30d, -60d, 0d, -60d, 0d, 60d, 30d, 60d, 30d, -60d);
        assertThatAnalysisRegionPositions(analyses, 4, 0, 0)
                .containsExactly(60d, -100d, 30d, -100d, 30d, -30d, 60d, -30d, 60d, -100d);

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(stringResult.getConvertedMessage()).hasValue(tacMessage);
    }

    @Test
    public void testHemisphereExceeding180SpecialCase() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z MOD HNH W180 - E180" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH E180 - W100 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD EQS E000 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD EQN W180 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MOD MNH W000 - W180" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage()
                .map(SpaceWeatherAdvisoryAmd82::getAnalyses)
                .orElse(Collections.emptyList());

        assertThatAnalysisRegionPositions(analyses, 0, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 1, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, -100d, 90d, -100d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 2, 0, 0)
                .containsExactly(0d, 0d, -30d, 0d, -30d, 180d, 0d, 180d, 0d, 0d);
        assertThatAnalysisRegionPositions(analyses, 3, 0, 0)
                .containsExactly(30d, -180d, 0d, -180d, 0d, 0d, 30d, 0d, 30d, -180d);
        assertThatAnalysisRegionPositions(analyses, 4, 0, 0)
                .containsExactly(60d, 0d, 30d, 0d, 30d, 180d, 60d, 180d, 60d, 0d);
    }

    @Test
    public void testHemisphereExceeding180SpecialCase2() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z MOD HNH W180 - E180" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD HNH E000 - W000 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD HNH W000 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MOD HNH E010 - E010" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage()
                .map(SpaceWeatherAdvisoryAmd82::getAnalyses)
                .orElse(Collections.emptyList());

        assertThatAnalysisRegionPositions(analyses, 0, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 1, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 2, 0, 0)
                .containsExactly(90d, 0d, 60d, 0d, 60d, 0d, 90d, 0d, 90d, 0d);
        assertThatAnalysisRegionPositions(analyses, 3, 0, 0)
                .containsExactly(90d, 0d, 60d, 0d, 60d, 0d, 90d, 0d, 90d, 0d);
        assertThatAnalysisRegionPositions(analyses, 4, 0, 0)
                .containsExactly(90d, 10d, 60d, 10d, 60d, 10d, 90d, 10d, 90d, 10d);
    }

    @Test
    public void test180ToZero() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z NOT AVBL" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH E180 - W000 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD HNH E180 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD HNH W180 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MOD HNH W180 - W000" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage()
                .map(SpaceWeatherAdvisoryAmd82::getAnalyses)
                .orElse(Collections.emptyList());

        assertThatAnalysisRegionPositions(analyses, 1, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 2, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 3, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d);
        assertThatAnalysisRegionPositions(analyses, 4, 0, 0)
                .containsExactly(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d);
    }

    @Test
    public void testZeroTo180() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z NOT AVBL" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH E000 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD HNH E000 - E180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD HNH W000 - E180 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MOD HNH W000 - W180" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage()
                .map(SpaceWeatherAdvisoryAmd82::getAnalyses)
                .orElse(Collections.emptyList());

        assertThatAnalysisRegionPositions(analyses, 1, 0, 0)
                .containsExactly(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d);
        assertThatAnalysisRegionPositions(analyses, 2, 0, 0)
                .containsExactly(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d);
        assertThatAnalysisRegionPositions(analyses, 3, 0, 0)
                .containsExactly(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d);
        assertThatAnalysisRegionPositions(analyses, 4, 0, 0)
                .containsExactly(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d);
    }

    @Test
    public void testAdvisoryStatusExercise() {
        final String expected = "SWX ADVISORY" + CR_LF//
                + "STATUS:             EXER" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "SWX EFFECT:         RADIATION" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "OBS SWX:            08/0100Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(pojoResult.getConvertedMessage().flatMap(AviationWeatherMessage::getRemarks)).isEmpty();

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(stringResult.getConvertedMessage()).hasValue(expected);
    }

    @Test
    public void testTwoAdvisoryReplaceNumbers() {
        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "SWX EFFECT:         RADIATION" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2020/11 2020/12" + CR_LF
                + "OBS SWX:            08/0100Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                NIL" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(pojoResult.getConvertedMessage()).isNotEmpty();

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(stringResult.getConvertedMessage()).hasValue(expected);
    }

    @Test
    public void testFourAdvisoryReplaceNumbers() {
        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "SWX EFFECT:         RADIATION" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2020/11 2020/12 2020/13 2020/14" + CR_LF
                + "OBS SWX:            08/0100Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                NIL" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(pojoResult.getConvertedMessage()).isNotEmpty();

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(stringResult.getConvertedMessage().orElse(null)).isEqualTo(expected);
    }

    @Test
    public void testFiveAdvisoryReplaceNumbers() {
        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "SWX EFFECT:         RADIATION" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2020/11 2020/12 2020/13 2020/14 2020/15" + CR_LF
                + "OBS SWX:            08/0100Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                NIL" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.WITH_WARNINGS);
        assertThat(pojoResult.getConvertedMessage()).isNotEmpty();

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.WITH_WARNINGS);
        assertThat(stringResult.getConvertedMessage()).hasValue(expected);
    }

    @Test
    public void testFractionalCoordinatesRoundedInSerialization() throws IOException {
        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "SWX EFFECT:         RADIATION" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2016/1" + CR_LF
                + "OBS SWX:            08/0100Z MOD N80 W160 - N01 W075 - N60 E016 - N71 E075 - N80 W160" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z NO SWX EXP" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z NO SWX EXP" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                TEST FRACTIONAL COORDINATES ROUNDING" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final SpaceWeatherAdvisoryAmd82 inputWithFractionals = loadAdvisory("spacewx-fractional-coords.json");

        final ConversionResult<String> result = this.converter.convertMessage(
                inputWithFractionals,
                TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage().orElse(null)).isEqualTo(expected);
    }

    @Test
    public void testUnclosedPolygonUnchangedSerialization() throws IOException {
        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "SWX EFFECT:         RADIATION" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2016/1" + CR_LF
                + "OBS SWX:            08/0100Z MOD N80 W150 - N70 E075 - N60 E015 - N01 W075 - N80 W160" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z MOD HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z NO SWX EXP" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z NO SWX EXP" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                TEST UNCLOSED POLYGON" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final SpaceWeatherAdvisoryAmd82 inputWithFractionals = loadAdvisory("spacewx-unclosed-polygon.json");

        final ConversionResult<String> result = this.converter.convertMessage(
                inputWithFractionals,
                TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).isPresent();
        assertThat(result.getConvertedMessage().orElse(null)).isEqualTo(expected);
    }

    @Test
    public void testMultipleIntensityAndRegions() {
        final String expected = "SWX ADVISORY" + CR_LF
                + "DTG:                20201108/0100Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "SWX EFFECT:         HF COM" + CR_LF
                + "ADVISORY NR:        2020/1" + CR_LF
                + "OBS SWX:            08/0100Z SEV MNH EQN EQS MSH DAYSIDE MOD NIGHTSIDE" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z NO SWX EXP" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z NO SWX EXP" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z NO SWX EXP" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                SWX EVENT IMPACTING LOWER HF COM FREQ BAND. SEE WWW.SPACEWEATHERPROVIDER.WEB" + CR_LF
                + "NXT ADVISORY:       WILL BE ISSUED BY 20201108/0700Z=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(pojoResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage()
                .map(SpaceWeatherAdvisoryAmd82::getAnalyses)
                .orElse(Collections.emptyList());
        assertThat(analyses).hasSize(5);

        final SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertThat(analysis.getTime().getCompleteTime().orElse(null))
                .isEqualTo(ZonedDateTime.parse("2020-11-08T01:00:00Z"));
        assertThat(analysis.getIntensityAndRegions())
                .extracting(SpaceWeatherIntensityAndRegion::getIntensity)
                .containsExactly(Intensity.SEVERE, Intensity.MODERATE);
        assertThat(analysis.getIntensityAndRegions().get(0).getRegions())
                .allSatisfy(region -> {
                    assertThat(region.getLongitudeLimitMinimum()).isEmpty();
                    assertThat(region.getLongitudeLimitMaximum()).isEmpty();
                })
                .extracting(region -> region.getLocationIndicator().orElse(null))
                .containsExactly(
                        SpaceWeatherRegion.SpaceWeatherLocation.MIDDLE_NORTHERN_HEMISPHERE,
                        SpaceWeatherRegion.SpaceWeatherLocation.EQUATORIAL_LATITUDES_NORTHERN_HEMISPHERE,
                        SpaceWeatherRegion.SpaceWeatherLocation.EQUATORIAL_LATITUDES_SOUTHERN_HEMISPHERE,
                        SpaceWeatherRegion.SpaceWeatherLocation.MIDDLE_LATITUDES_SOUTHERN_HEMISPHERE,
                        SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE
                );
        assertThat(analysis.getIntensityAndRegions().get(1).getRegions())
                .extracting(region -> region.getLocationIndicator().orElse(null))
                .containsExactly(SpaceWeatherRegion.SpaceWeatherLocation.NIGHTSIDE);

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertThat(stringResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(stringResult.getConvertedMessage()).hasValue(expected);
    }
}
