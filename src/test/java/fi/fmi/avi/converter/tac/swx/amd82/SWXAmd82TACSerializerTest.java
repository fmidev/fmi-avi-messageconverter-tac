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
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAmd82Impl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd82TACSerializerTest {
    private static final String CR_LF = CARRIAGE_RETURN.getContent() + LINE_FEED.getContent();

    @Autowired
    @Qualifier("tacTokenizer")
    private AviMessageTACTokenizer tokenizer;

    @Autowired
    private AviMessageConverter converter;

    private SpaceWeatherAdvisoryAmd82 msg;

    private static String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd82ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    private static void assertRegionPolygonEquals(final List<Double> expectedExteriorRingPositions, final SpaceWeatherAdvisoryAnalysis analysis,
                                                  final int regionIndex) {
        assertFalse(analysis.getRegions().isEmpty());
        final Geometry geom = analysis.getRegions().get(regionIndex).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertTrue(geom instanceof PolygonGeometry);
        assertEquals(expectedExteriorRingPositions, ((PolygonGeometry) geom).getExteriorRingPositions());
    }

    @Before
    public void setup() throws Exception {
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

        final String input = getInput("spacewx-A2-3.json");
        msg = om.readValue(input, SpaceWeatherAdvisoryAmd82Impl.class);
    }

    @Test
    public void swxSerializerTest() {
        final ConversionResult<String> result = this.converter.convertMessage(msg, TACConverter.SWX_AMD82_POJO_TO_TAC, new ConversionHints());
        assertTrue(result.getConvertedMessage().isPresent());
        System.out.println(result.getConvertedMessage().get());
    }

    @Test
    public void testNilRemark() {
        final String expected = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        assertFalse(pojoResult.getConvertedMessage().get().getRemarks().isPresent());

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(expected, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testDecimalHandling() {
        final String original = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z HNH HSH E020 - W17200 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E150 - W40 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E5 - W160 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E17930 - W02054 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z N80 W15006 - N1 W75 - N60 E15 - N70 E75 - N80 W16024" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final String expected = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z HNH HSH E020 - W172 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E150 - W040 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E005 - W160 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E17930 - W02054 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z N80 W15006 - N01 W075 - N60 E015 - N70 E075 - N80 W16024" + CARRIAGE_RETURN.getContent()
                + LINE_FEED.getContent()//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(original, TACConverter.TAC_TO_SWX_AMD82_POJO);
        for (final ConversionIssue issue : pojoResult.getConversionIssues()) {
            System.err.println("iss:" + issue);
        }
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        assertRegionPolygonEquals(Arrays.asList(90d, 20d, 60d, 20d, 60d, -172d, 90d, -172d, 90d, 20d), analyses.get(0), 0);//
        assertRegionPolygonEquals(Arrays.asList(90d, 150d, 60d, 150d, 60d, -40d, 90d, -40d, 90d, 150d), analyses.get(1), 0);//
        assertRegionPolygonEquals(Arrays.asList(90d, 5d, 60d, 5d, 60d, -160d, 90d, -160d, 90d, 5d), analyses.get(2), 0);//
        assertRegionPolygonEquals(Arrays.asList(80d, -150.1d, 1d, -75d, 60d, 15d, 70d, 75d, 80d, -160.4d), analyses.get(4), 0);

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(expected, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testHemisphereExceeding180() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z HNH W180 - E120" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH W020 - W100 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z EQS E180 - E090 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z EQN W060 - E060 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MNH W100 - W030" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 120d, 90d, 120d, 90d, -180d), analyses.get(0), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, -20d, 60d, -20d, 60d, -100d, 90d, -100d, 90d, -20d), analyses.get(1), 0);
        assertRegionPolygonEquals(Arrays.asList(0d, -180d, -30d, -180d, -30d, 90d, 0d, 90d, 0d, -180d), analyses.get(2), 0);
        assertRegionPolygonEquals(Arrays.asList(30d, -60d, 0d, -60d, 0d, 60d, 30d, 60d, 30d, -60d), analyses.get(3), 0);
        assertRegionPolygonEquals(Arrays.asList(60d, -100d, 30d, -100d, 30d, -30d, 60d, -30d, 60d, -100d), analyses.get(4), 0);

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(tacMessage, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testHemisphereExceeding180SpecialCase() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z HNH W180 - E180" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH E180 - W100 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z EQS E000 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z EQN W180 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z MNH W000 - W180" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d), analyses.get(0), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, -100d, 90d, -100d, 90d, -180d), analyses.get(1), 0);
        assertRegionPolygonEquals(Arrays.asList(0d, 0d, -30d, 0d, -30d, 180d, 0d, 180d, 0d, 0d), analyses.get(2), 0);
        assertRegionPolygonEquals(Arrays.asList(30d, -180d, 0d, -180d, 0d, 0d, 30d, 0d, 30d, -180d), analyses.get(3), 0);
        assertRegionPolygonEquals(Arrays.asList(60d, 0d, 30d, 0d, 30d, 180d, 60d, 180d, 60d, 0d), analyses.get(4), 0);
    }

    @Test
    public void testHemisphereExceeding180SpecialCase2() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z HNH W180 - E180" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z HNH E000 - W000 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z HNH W000 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z HNH E010 - E010" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d), analyses.get(0), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d), analyses.get(1), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 0d, 90d, 0d, 90d, 0d), analyses.get(2), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 0d, 90d, 0d, 90d, 0d), analyses.get(3), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, 10d, 60d, 10d, 60d, 10d, 90d, 10d, 90d, 10d), analyses.get(4), 0);
    }

    @Test
    public void test180ToZero() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z NOT AVBL" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH E180 - W000 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z HNH E180 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z HNH W180 - E000 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z HNH W180 - W000" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d), analyses.get(1), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d), analyses.get(2), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d), analyses.get(3), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d), analyses.get(4), 0);
    }

    @Test
    public void testZeroTo180() {
        final String tacMessage = "SWX ADVISORY" + CR_LF//
                + "STATUS:             TEST" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z NOT AVBL" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH E000 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z HNH E000 - E180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z HNH W000 - E180 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z HNH W000 - W180" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        final List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        assertRegionPolygonEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d), analyses.get(1), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d), analyses.get(2), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d), analyses.get(3), 0);
        assertRegionPolygonEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d), analyses.get(4), 0);
    }

    @Test
    public void testAdvisoryStatusExercise() {
        final String expected = "SWX ADVISORY" + CR_LF//
                + "STATUS:             EXER" + CR_LF//
                + "DTG:                20161108/0000Z" + CR_LF//
                + "SWXC:               DONLON" + CR_LF//
                + "ADVISORY NR:        2016/2" + CR_LF//
                + "NR RPLC:            2016/1" + CR_LF//
                + "SWX EFFECT:         RADIATION MOD" + CR_LF//
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CR_LF//
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF//
                + "RMK:                NIL" + CR_LF//
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());
        assertFalse(pojoResult.getConvertedMessage().get().getRemarks().isPresent());

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(expected, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testTwoAdvisoryReplaceNumbers() {
        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2020/11 2020/12" + CR_LF
                + "SWX EFFECT:         RADIATION MOD" + CR_LF
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                NIL" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(expected, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testFourAdvisoryReplaceNumbers() {
        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2020/11 2020/12 2020/13 2020/14" + CR_LF
                + "SWX EFFECT:         RADIATION MOD" + CR_LF
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                NIL" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(expected, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testFiveAdvisoryReplaceNumbers() {
        final String inputTac = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2020/11 2020/12 2020/13 2020/14 2020/15" + CR_LF
                + "SWX EFFECT:         RADIATION MOD" + CR_LF
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                NIL" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final String expected = "SWX ADVISORY" + CR_LF
                + "STATUS:             TEST" + CR_LF
                + "DTG:                20161108/0000Z" + CR_LF
                + "SWXC:               DONLON" + CR_LF
                + "ADVISORY NR:        2016/2" + CR_LF
                + "NR RPLC:            2020/11 2020/12 2020/13 2020/14" + CR_LF
                + "SWX EFFECT:         RADIATION MOD" + CR_LF
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CR_LF
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CR_LF
                + "RMK:                NIL" + CR_LF
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult = this.converter.convertMessage(inputTac, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(ConversionResult.Status.WITH_ERRORS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());

        final ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_AMD82_POJO_TO_TAC,
                new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(expected, stringResult.getConvertedMessage().get());
    }


}
