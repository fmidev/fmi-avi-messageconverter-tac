package fi.fmi.avi.converter.tac.swx;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.MultiPolygonGeometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXTACSerializerTest {

    @Autowired
    @Qualifier("tacTokenizer")
    private AviMessageTACTokenizer tokenizer;

    @Autowired
    private AviMessageConverter converter;

    private SpaceWeatherAdvisory msg;

    @Before
    public void setup() throws Exception {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

        String input = getInput("spacewx-A2-3.json");
        msg = om.readValue(input, SpaceWeatherAdvisoryImpl.class);
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
    public void swxSerializerTest() {
        ConversionResult<String> result = this.converter.convertMessage(msg, TACConverter.SWX_POJO_TO_TAC, new ConversionHints());
        Assert.assertTrue(result.getConvertedMessage().isPresent());
        System.out.println(result.getConvertedMessage().get());
    }

    @Test
    public void testNilRemark() throws Exception {
        String expected = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        assertFalse(pojoResult.getConvertedMessage().get().getRemarks().isPresent());

        ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_POJO_TO_TAC,
                new ConversionHints());
        Assert.assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        Assert.assertTrue(stringResult.getConvertedMessage().isPresent());
        Assert.assertEquals(expected, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testDecimalHandling() {
        String original = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH HSH E020 - W17200 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E150 - W40 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E5 - W160 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E17952 - W02050 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z N80 W15010 - N1 W75 - N60 E15 - N70 E75 - N80 W16025" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        String expected = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH HSH E020 - W172 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E150 - W040 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E005 - W160 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E17952 - W02050 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z N80 W15010 - N01 W075 - N60 E015 - N70 E075 - N80 W16025" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(original, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();
        List<List<Double>> geom1 = Arrays.asList(
                Arrays.asList(90d, 20d, 60d, 20d, 60d, 180d, 90d, 180d, 90d, 20d),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, -172d, 90d, -172d, 90d, -180d));
        List<List<Double>> geom2 = Arrays.asList(
                Arrays.asList(90d, 150d, 60d, 150d, 60d, 180d, 90d, 180d, 90d, 150d),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, -40d, 90d, -40d, 90d, -180d));
        List<List<Double>> geom3 = Arrays.asList(
                Arrays.asList(90d, 5d, 60d, 5d, 60d, 180d, 90d, 180d, 90d, 5d),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, -160d, 90d, -160d, 90d, -180d));
        List<List<Double>> geom4 = Arrays.asList(
                Arrays.asList(90d, 179.52, 60d, 179.52, 60d, 180d, 90d, 180d, 90d, 179.52),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, -20.5, 90d, -20.5, 90d, -180d));
        List<Double> geom5 = Arrays.asList(80d, -150.1, 1d, -75d, 60d, 15d, 70d, 75d, 80d, -160.25);


        checkLatitudes(analyses.get(0), geom1);//
        checkLatitudes(analyses.get(1), geom2);//
        checkLatitudes(analyses.get(2), geom3);//
        checkLatitudes(analyses.get(3), geom4);//
        checkLatitudes(analyses.get(4), geom5);



        ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_POJO_TO_TAC,
                new ConversionHints());
        Assert.assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        Assert.assertTrue(stringResult.getConvertedMessage().isPresent());
        Assert.assertEquals(expected, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testHemisphereExceeding180() throws Exception {
        String tacMessage = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH W180 - E120" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH W020 - W100 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z EQS E180 - E090 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z EQN W060 - E060 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z MNH W100 - W030" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        //Analysis 1
        checkGeometryType(analyses.get(0), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 120d, 90d, 120d, 90d, -180d),
                ((PolygonGeometry) analyses.get(0).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        //Analysis 2
        checkGeometryType(analyses.get(1), MultiPolygonGeometry.class);
        List<List<Double>> expected = Arrays.asList(
                Arrays.asList(90d, -20d, 60d, -20d, 60d, 180d, 90d, 180d, 90d, -20d),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, -100d, 90d, -100d, 90d, -180d));

        assertEquals(expected,
                ((MultiPolygonGeometry) analyses.get(1).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        //Analysis 3
        checkGeometryType(analyses.get(2), PolygonGeometry.class);
        assertEquals(Arrays.asList(0d, -180d, -30d, -180d, -30d, 90d, 0d, 90d, 0d, -180d),
                ((PolygonGeometry) analyses.get(2).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        //Analysis 4
        checkGeometryType(analyses.get(3), PolygonGeometry.class);

        assertEquals(Arrays.asList(30d, -60d, 0d, -60d, 0d, 60d, 30d, 60d, 30d, -60d),
                ((PolygonGeometry) analyses.get(3).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        //Analysis 5
        checkGeometryType(analyses.get(4), PolygonGeometry.class);
        assertEquals(Arrays.asList(60d, -100d, 30d, -100d, 30d, -30d, 60d, -30d, 60d, -100d),
                ((PolygonGeometry) analyses.get(4).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_POJO_TO_TAC,
                new ConversionHints());
        Assert.assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        Assert.assertTrue(stringResult.getConvertedMessage().isPresent());
        Assert.assertEquals(tacMessage, stringResult.getConvertedMessage().get());
    }

    @Test
    public void testHemisphereExceeding180SpecialCase() throws Exception {
        String tacMessage = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent() + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent() + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH W180 - E180" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH E180 - W100 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z EQS E000 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z EQN W180 - E000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z MNH W000 - W180" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        //Analysis 1
        checkGeometryType(analyses.get(0), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d),
                ((PolygonGeometry) analyses.get(0).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        //Analysis 2
        checkGeometryType(analyses.get(1), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, -100d, 90d, -100d, 90d, -180d),
                ((PolygonGeometry) analyses.get(1).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        //Analysis 3
        checkGeometryType(analyses.get(2), PolygonGeometry.class);
        assertEquals(Arrays.asList(0d, 0d, -30d, 0d, -30d, 180d, 0d, 180d, 0d, 0d),
                ((PolygonGeometry) analyses.get(2).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        checkGeometryType(analyses.get(3), PolygonGeometry.class);
        assertEquals(Arrays.asList(30d, -180d, 0d, -180d, 0d, 0d, 30d, 0d, 30d, -180d),
                ((PolygonGeometry) analyses.get(3).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        //Analysis 5
        checkGeometryType(analyses.get(4), PolygonGeometry.class);
        assertEquals(Arrays.asList(60d, 0d, 30d, 0d, 30d, 180d, 60d, 180d, 60d, 0d),
                ((PolygonGeometry) analyses.get(4).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());
    }

    @Test
    public void testHemisphereExceeding180SpecialCase2() throws Exception {
        String tacMessage = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH W180 - E180" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH E000 - W000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH W000 - E000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z HNH E010 - E010" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        int index = 0;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index++;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index++;
        checkGeometryType(analyses.get(index), MultiPolygonGeometry.class);
        assertEquals(Arrays.asList(
                Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d)),
                ((MultiPolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index++;
        checkGeometryType(analyses.get(index), MultiPolygonGeometry.class);
        assertEquals(Arrays.asList(
                Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d)),
                ((MultiPolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index++;
        checkGeometryType(analyses.get(index), MultiPolygonGeometry.class);
        assertEquals(Arrays.asList(
                Arrays.asList(90d, 10d, 60d, 10d, 60d, 180d, 90d, 180d, 90d, 10d),
                Arrays.asList(90d, -180d, 60d, -180d, 60d, 10d, 90d, 10d, 90d, -180d)),
                ((MultiPolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());


    }

        private void checkGeometryType(SpaceWeatherAdvisoryAnalysis analysis, Class<?> clazz) {
        assertFalse(analysis.getRegions().isEmpty());
        Geometry geom = analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertTrue(clazz.isInstance(geom));
    }

    @Test
    public void test180ToZero() {
        String tacMessage = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z NOT AVBL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH E180 - W000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH E180 - E000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH W180 - E000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z HNH W180 - W000" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        int index = 1;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index ++;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index ++;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index ++;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, -180d, 60d, -180d, 60d, 0d, 90d, 0d, 90d, -180d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

    }

    @Test
    public void testZeroTo180() {
        String tacMessage = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z NOT AVBL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH E000 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH E000 - E180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH W000 - E180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z HNH W000 - W180" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(tacMessage, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        List<SpaceWeatherAdvisoryAnalysis> analyses = pojoResult.getConvertedMessage().get().getAnalyses();

        int index = 1;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index ++;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index ++;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());

        index ++;
        checkGeometryType(analyses.get(index), PolygonGeometry.class);
        assertEquals(Arrays.asList(90d, 0d, 60d, 0d, 60d, 180d, 90d, 180d, 90d, 0d),
                ((PolygonGeometry) analyses.get(index).getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get()).getExteriorRingPositions());
    }

    @Test
    public void testAdvisoryStatusExercise() throws Exception {
        String expected = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             EXER" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E180 - W180 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        assertFalse(pojoResult.getConvertedMessage().get().getRemarks().isPresent());

        ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_POJO_TO_TAC,
                new ConversionHints());
        Assert.assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        Assert.assertTrue(stringResult.getConvertedMessage().isPresent());
        Assert.assertEquals(expected, stringResult.getConvertedMessage().get());
    }



    private void checkLatitudes(SpaceWeatherAdvisoryAnalysis analysis, List<?> expected) {
        assertFalse(analysis.getRegions().isEmpty());
        Geometry geom = analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        if(geom instanceof PolygonGeometry) {
            List<Double> positions = ((PolygonGeometry) geom).getExteriorRingPositions();
            assertEquals(expected, positions);
        } else if (geom instanceof MultiPolygonGeometry) {
            List<List<Double>> positions = ((MultiPolygonGeometry) geom).getExteriorRingPositions();
            assertEquals(expected, positions);
        }
    }
}
