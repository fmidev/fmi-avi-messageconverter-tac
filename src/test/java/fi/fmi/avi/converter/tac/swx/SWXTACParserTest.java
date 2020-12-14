package fi.fmi.avi.converter.tac.swx;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;
import fi.fmi.avi.model.swx.immutable.IssuingCenterImpl;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXTACParserTest {

    @Autowired
    private AviMessageConverter converter;

    /*
    Dummy SWX Lexer produces a message like below:

        + "SWX ADVISORY\n" //
                       + "STATUS: TEST\n"//
                       + "DTG: 20190128/1200Z\n" //
                       + "SWXC: PECASUS\n" //
                       + "ADVISORY NR: 2019/1\n"//
                       + "SWX EFFECT: SATCOM MOD AND RADIATION SEV\n" //
                       + "OBS SWX: 08/1200Z HNH HSH E16000 - W2000 ABV FL340\n"//
                       + "FCST SWX +6 HR: 08/1800Z N80 W180 - N70 W75 - N60 E15 - N70 E75 - N80 W180 ABV FL370\n"//
                       + "FCST SWX +12 HR: 09/0000Z NO SWX EXP\n"//
                       + "FCST SWX +18 HR: 09/0600Z DAYLIGHT SIDE\n"//
                       + "FCST SWX +24 HR: 09/1200Z NO SWX EXP\n"//
                       + "RMK: TEST TEST TEST TEST\n"
                       + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" + "SEE WWW.PECASUS.ORG \n"
                       + "NXT ADVISORY: WILL BE ISSUED BY 20161108/0700Z\n \n",
        */

    @Test
    public void testParser1() throws Exception {
        final String input = getInput("spacewx-A2-3.tac");

        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);

        assertEquals(0, result.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals(swx.getPermissibleUsageReason().get(), AviationCodeListUser.PermissibleUsageReason.EXERCISE);
        assertEquals(swx.getIssuingCenter().getName().get(), "DONLON");
        assertEquals(swx.getAdvisoryNumber().getSerialNumber(), 2);
        assertEquals(swx.getAdvisoryNumber().getYear(), 2016);
        assertEquals(2, swx.getPhenomena().size());
        assertEquals(swx.getPhenomena().get(0), SpaceWeatherPhenomenon.fromCombinedCode("HF COM MOD"));
        assertEquals(swx.getPhenomena().get(1), SpaceWeatherPhenomenon.fromCombinedCode("GNSS MOD"));

        final String[] expectedRemarks = { "LOW", "LVL", "GEOMAGNETIC", "STORMING", "CAUSING", "INCREASED", "AURORAL", "ACT", "AND", "SUBSEQUENT", "MOD",
                "DEGRADATION", "OF", "GNSS", "AND", "HF", "COM", "AVBL", "IN", "THE", "AURORAL", "ZONE.", "THIS", "STORMING", "EXP", "TO", "SUBSIDE", "IN",
                "THE", "FCST", "PERIOD.", "SEE", "WWW.SPACEWEATHERPROVIDER.WEB" };
        assertTrue(Arrays.deepEquals(swx.getRemarks().get().toArray(new String[33]), expectedRemarks));
        assertEquals(swx.getNextAdvisory().getTimeSpecifier(), NextAdvisory.Type.NO_FURTHER_ADVISORIES);

        final List<SpaceWeatherAdvisoryAnalysis> analyses = swx.getAnalyses();
        assertEquals(5, analyses.size());
        SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertEquals(2, analysis.getRegions().size());

        assertTrue(analysis.getRegions().get(0).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegions().get(0).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        final PolygonGeometry poly = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        final Double[] expected = { 90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d };
        final Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));

        analysis = analyses.get(4);
        Assert.assertTrue(analysis.getNilPhenomenonReason().isPresent());
        Assert.assertEquals(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED, analysis.getNilPhenomenonReason().get());
        Assert.assertEquals(NextAdvisory.Type.NO_FURTHER_ADVISORIES, swx.getNextAdvisory().getTimeSpecifier());
        Assert.assertTrue(swx.getReplaceAdvisoryNumber().isPresent());
        Assert.assertEquals(1, swx.getReplaceAdvisoryNumber().get().getSerialNumber());
        Assert.assertEquals(2016, swx.getReplaceAdvisoryNumber().get().getYear());
    }

    @Test
    public void testParser2() throws Exception {
        final String input = getInput("spacewx-A2-4.tac");

        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);

        assertEquals(0, result.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertTrue(swx.getPermissibleUsage().isPresent());
        assertEquals(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL, swx.getPermissibleUsage().get());
        assertEquals(AviationCodeListUser.PermissibleUsageReason.TEST, swx.getPermissibleUsageReason().get());
        assertEquals(swx.getIssuingCenter().getName().get(), "DONLON");
        assertEquals(swx.getAdvisoryNumber().getSerialNumber(), 2);
        assertEquals(swx.getAdvisoryNumber().getYear(), 2016);
        assertEquals(1, swx.getPhenomena().size());
        assertEquals(swx.getPhenomena().get(0), SpaceWeatherPhenomenon.fromCombinedCode("RADIATION MOD"));
        final String[] expectedRemarks = { "RADIATION", "LVL", "EXCEEDED", "100", "PCT", "OF", "BACKGROUND", "LVL", "AT", "FL340", "AND", "ABV.", "THE",
                "CURRENT", "EVENT", "HAS", "PEAKED", "AND", "LVL", "SLW", "RTN", "TO", "BACKGROUND", "LVL.", "SEE", "WWW.SPACEWEATHERPROVIDER.WEB" };
        assertTrue(Arrays.deepEquals(swx.getRemarks().get().toArray(new String[26]), expectedRemarks));
        assertEquals(swx.getNextAdvisory().getTimeSpecifier(), NextAdvisory.Type.NO_FURTHER_ADVISORIES);

        final List<SpaceWeatherAdvisoryAnalysis> analyses = swx.getAnalyses();
        assertEquals(5, analyses.size());
        final SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertEquals(2, analysis.getRegions().size());

        //1st region:
        SpaceWeatherRegion r = analysis.getRegions().get(0);

        assertTrue(r.getLocationIndicator().isPresent());
        assertEquals(r.getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertTrue(r.getAirSpaceVolume().isPresent());
        assertTrue(r.getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(r.getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        PolygonGeometry poly = (PolygonGeometry) r.getAirSpaceVolume().get().getHorizontalProjection().get();

        Double[] expected = { 90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d };
        Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));

        assertEquals(r.getAirSpaceVolume().get().getLowerLimit().get().getValue(), 340.0);
        assertEquals(r.getAirSpaceVolume().get().getLowerLimit().get().getUom(), "FL");
        assertEquals(r.getAirSpaceVolume().get().getLowerLimitReference().get(), "STD");

        //2nd region:
        r = analysis.getRegions().get(1);
        assertTrue(r.getLocationIndicator().isPresent());
        assertEquals(r.getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE);
        assertTrue(r.getAirSpaceVolume().isPresent());
        assertTrue(r.getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(r.getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        poly = (PolygonGeometry) r.getAirSpaceVolume().get().getHorizontalProjection().get();

        expected = new Double[] { -60d, -180d, -90d, -180d, -90d, 180d, -60d, 180d, -60d, -180d };
        actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));

        assertEquals(r.getAirSpaceVolume().get().getLowerLimit().get().getValue(), 340.0);
        assertEquals(r.getAirSpaceVolume().get().getLowerLimit().get().getUom(), "FL");
        assertEquals(r.getAirSpaceVolume().get().getLowerLimitReference().get(), "STD");

    }

    @Test
    public void testAdvancedMessage() throws IOException {
        final String input = getInput("spacewx-advanced.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertTrue(swx.getIssuingCenter().getName().isPresent());
        assertEquals(swx.getIssuingCenter().getName().get(), "PECASUS");
        assertEquals(swx.getAdvisoryNumber().getSerialNumber(), 1);
        assertEquals(swx.getAdvisoryNumber().getYear(), 2019);
        assertEquals(2, swx.getPhenomena().size());
        assertEquals(swx.getPhenomena().get(0), SpaceWeatherPhenomenon.fromCombinedCode("SATCOM MOD"));
        assertEquals(swx.getPhenomena().get(1), SpaceWeatherPhenomenon.fromCombinedCode("RADIATION SEV"));
        assertTrue(swx.getRemarks().isPresent());
        final String[] expectedRemarks = { "TEST", "TEST", "TEST", "TEST", "THIS", "IS", "A", "TEST", "MESSAGE", "FOR", "TECHNICAL", "TEST.", "SEE",//
                "WWW.PECASUS.ORG" };
        assertTrue(Arrays.deepEquals(swx.getRemarks().get().toArray(new String[14]), expectedRemarks));
        assertEquals(swx.getNextAdvisory().getTimeSpecifier(), NextAdvisory.Type.NEXT_ADVISORY_BY);

        final List<SpaceWeatherAdvisoryAnalysis> analyses = swx.getAnalyses();
        assertEquals(5, analyses.size());

        SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertEquals(2, analysis.getRegions().size());

        assertTrue(analysis.getRegions().get(0).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegions().get(0).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        PolygonGeometry geometry = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        List<Double> expectedExteriorRingPositions = Arrays.asList(90d, 160d, 60d, 160d, 60d, -20d, 90d, -20d, 90d, 160d);
        //Double[] expected = { 90d, 160d, 60d, 160d, 60d, -20d, 90d, -20d, 90d, 160d };
        List<Double> actualExteriorRingPositions = geometry.getExteriorRingPositions();
        assertEquals(expectedExteriorRingPositions, actualExteriorRingPositions);

        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimitReference().isPresent());
        assertFalse(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit().isPresent());
        assertFalse(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimitReference().isPresent());
        assertEquals(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build(),
                analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().get());

        assertTrue(analysis.getRegions().get(1).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegions().get(1).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE);
        assertTrue(analysis.getRegions().get(1).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegions().get(1).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(analysis.getRegions().get(1).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        geometry = (PolygonGeometry) analysis.getRegions().get(1).getAirSpaceVolume().get().getHorizontalProjection().get();
        expectedExteriorRingPositions = Arrays.asList(-60d, 160d, -90d, 160d, -90d, -20d, -60d, -20d, -60d, 160d);
        actualExteriorRingPositions = geometry.getExteriorRingPositions();
        assertEquals(expectedExteriorRingPositions, actualExteriorRingPositions);

        analysis = analyses.get(1);
        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertEquals(1, analysis.getRegions().size());
        assertFalse(analysis.getRegions().get(0).getLocationIndicator().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        final PolygonGeometry poly = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        final Double[] expected = new Double[] { 80d, -180d, 70d, -75d, 60d, 15d, 70d, 75d, 80d, -180d };
        final Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimitReference().isPresent());
        assertFalse(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit().isPresent());
        assertFalse(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimitReference().isPresent());
        assertEquals(NumericMeasureImpl.builder().setValue(370d).setUom("FL").build(),
                analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().get());

        analysis = analyses.get(2);

        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertTrue(analysis.getNilPhenomenonReason().isPresent());
        assertEquals(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED, analysis.getNilPhenomenonReason().get());

        analysis = analyses.get(3);

        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertEquals(1, analysis.getRegions().size());
        assertTrue(analysis.getRegions().get(0).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegions().get(0).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.DAYLIGHT_SIDE);

        analysis = analyses.get(4);

        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertEquals(1, analysis.getRegions().size());
        assertEquals(analysis.getRegions().get(0).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        geometry = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        expectedExteriorRingPositions = Arrays.asList(90d, 160d, 60d, 160d, 60d, -20d, 90d, -20d, 90d, 160d);
        actualExteriorRingPositions = geometry.getExteriorRingPositions();
        assertEquals(expectedExteriorRingPositions, actualExteriorRingPositions);
        assertTrue(Arrays.deepEquals(expected, actual));
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimitReference().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimitReference().isPresent());

        assertEquals(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build(),
                analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().get());
        assertEquals(NumericMeasureImpl.builder().setValue(370d).setUom("FL").build(),
                analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit().get());
    }

    @Test
    public void testBadMessage() throws IOException {
        final String input = getInput("spacewx-pecasus-notavbl.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(ConversionResult.Status.FAIL, result.getStatus());
        assertEquals(16, result.getConversionIssues().size());
        assertFalse(result.getConvertedMessage().isPresent());
    }

    @Test
    public void testInvalidMinimalMessage() throws IOException {
        final String input = getInput("spacewx-invalid-minimal.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().size() > 0);
    }

    @Test
    public void testInvalidMissingEndToken() throws IOException {
        final String input = getInput("spacewx-invalid-missing-end-token.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(1, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertEquals("Message does not end in end token", result.getConversionIssues().get(0).getMessage());
    }

    @Test
    public void testEndTokenAtEndAndRandomPlace() throws IOException {
        final String input = getInput("spacewx-invalid-end-token-at-end-and-elsewhere.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConvertedMessage().isPresent());
        assertEquals(1, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertTrue(result.getConversionIssues().get(0).getMessage().startsWith("Message has an extra end token"));
    }

    @Test
    public void testInvalidStatusLabel() throws IOException {
        final String input = getInput("spacewx-invalid-status-label.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(3, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertTrue(result.getConversionIssues().get(0).getMessage().contains("Input message lexing was not fully successful"));
    }

    @Test
    public void testInvalidEmptyStatus() throws IOException {
        final String input = getInput("spacewx-invalid-status-empty.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(12, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.MISSING_DATA, result.getConversionIssues().get(3).getType());
        assertTrue(result.getConversionIssues()
                .stream()
                .map(ConversionIssue::getMessage)
                .anyMatch(s -> s.contains("Advisory status label was found, but the status could not be parsed in message")));
    }

    @Test
    public void testInvalidRemarkLabel() throws IOException {
        final String input = getInput("spacewx-invalid-remark-label.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(36, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertTrue(result.getConversionIssues().get(0).getMessage().contains("Input message lexing was not fully successful"));
    }

    @Test
    public void testInvalidRmkLabel() throws IOException {
        final String input = getInput("spacewx-invalid-rmk-label.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(6, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(1).getType());
        assertEquals("Lexing problem with 'RMK'", result.getConversionIssues().get(1).getMessage());

        assertEquals(ConversionIssue.Type.MISSING_DATA, result.getConversionIssues().get(5).getType());
        assertTrue(result.getConversionIssues().get(5).getMessage().contains("One of REMARKS_START required in message"));
    }

    @Test
    public void testInvalidReplaceNumberLabel() throws IOException {
        final String input = getInput("spacewx-invalid-replace-number-label.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(4, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertTrue(result.getConversionIssues().get(0).getMessage().contains("Input message lexing was not fully successful"));
    }

    @Test
    public void testInvalidNextAdvisoryLabelWithoutRemarks() throws IOException {
        final String input = getInput("spacewx-invalid-next-advisory-label.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(8, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertTrue(result.getConversionIssues().get(0).getMessage().contains("Input message lexing was not fully successful"));
    }

    @Test
    public void testInvalidAdvisoryNumber() throws IOException {
        final String input = getInput("spacewx-invalid-advisory-number.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(4, result.getConversionIssues().size());
        final ConversionIssue issue = result.getConversionIssues().get(2);
        assertEquals(ConversionIssue.Type.MISSING_DATA, issue.getType());
        assertEquals(ConversionIssue.Severity.ERROR, issue.getSeverity());
        Assert.assertTrue(issue.getMessage().contains("One of ADVISORY_NUMBER required in message"));
    }

    @Test
    public void testInvalidReplaceNumber() throws IOException {
        final String input = getInput("spacewx-invalid-replace-number.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(3, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.MISSING_DATA, result.getConversionIssues().get(2).getType());
        assertTrue(result.getConversionIssues().get(2).getMessage().contains("Replace advisory number is missing"));
    }

    @Test
    public void testInvalidEndTokenMisplaced() throws IOException {
        final String input = getInput("spacewx-invalid-misplaced-end-token.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(1, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Severity.ERROR, result.getConversionIssues().get(0).getSeverity());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertEquals("Message does not end in end token", result.getConversionIssues().get(0).getMessage());
    }

    @Test
    public void testInvalidDuplicateSwxc() throws IOException {
        final String input = getInput("spacewx-invalid-duplicate-swxc.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(1, result.getConversionIssues().size());
        final ConversionIssue issue = result.getConversionIssues().get(0);
        assertEquals(ConversionIssue.Severity.ERROR, issue.getSeverity());
        assertEquals(ConversionIssue.Type.SYNTAX, issue.getType());
        assertTrue(issue.getMessage().contains("More than one of SWX_CENTRE"));
    }

    @Test
    public void testInvalidMissingLatitudeBands() throws IOException {
        final String input = getInput("spacewx-invalid-missing-latitude-bands.tac");
        final List<Double> worldPolygonCoords = Arrays.asList(-90d, -180d, 90d, -180d, 90d, 180d, -90d, 180d, -90d, -180d);

        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(4, result.getConversionIssues().size());
        assertTrue(result.getConversionIssues().stream()//
                .allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.WARNING && issue.getType() == ConversionIssue.Type.MISSING_DATA));
        assertTrue(result.getConvertedMessage().isPresent());

        final List<AirspaceVolume> airspaceVolumes = result.getConvertedMessage().get().getAnalyses().stream()//
                .flatMap(analysis -> analysis.getRegions().stream())//
                .map(SpaceWeatherRegion::getAirSpaceVolume).map(Optional::get)//
                .collect(Collectors.toList());

        final AirspaceVolume obsVolume = airspaceVolumes.get(0);
        assertEquals("STD", obsVolume.getUpperLimitReference().get());
        assertEquals("STD", obsVolume.getLowerLimitReference().get());
        assertEquals(NumericMeasureImpl.builder().setValue(250d).setUom("FL").build(), obsVolume.getLowerLimit().get());
        assertEquals(NumericMeasureImpl.builder().setValue(350d).setUom("FL").build(), obsVolume.getUpperLimit().get());
        assertEquals(Arrays.asList(-90d, -150d, 90d, -150d, 90d, -30d, -90d, -30d, -90d, -150d),
                ((PolygonGeometry) obsVolume.getHorizontalProjection().get()).getExteriorRingPositions());

        for (int i = 1; i < 4; i++) {
            final AirspaceVolume forecastVolume = airspaceVolumes.get(i);
            assertFalse(forecastVolume.getUpperLimitReference().isPresent());
            assertFalse(forecastVolume.getUpperLimit().isPresent());
            assertEquals("STD", forecastVolume.getLowerLimitReference().get());
            assertEquals(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build(), forecastVolume.getLowerLimit().get());
            assertEquals(worldPolygonCoords, ((PolygonGeometry) forecastVolume.getHorizontalProjection().get()).getExteriorRingPositions());
        }
    }

    @Test
    public void testInvalidMissingLatitudeBandsAndLongitudes() throws IOException {
        final String input = getInput("spacewx-invalid-missing-latitude-bands-longitudes.tac");
        final List<Double> worldPolygonCoords = Arrays.asList(-90d, -180d, 90d, -180d, 90d, 180d, -90d, 180d, -90d, -180d);

        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(4, result.getConversionIssues().size());
        assertTrue(result.getConversionIssues().stream()//
                .allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.WARNING && issue.getType() == ConversionIssue.Type.MISSING_DATA));
        assertTrue(result.getConvertedMessage().isPresent());

        final List<AirspaceVolume> airspaceVolumes = result.getConvertedMessage().get().getAnalyses().stream()//
                .flatMap(analysis -> analysis.getRegions().stream())//
                .map(SpaceWeatherRegion::getAirSpaceVolume).map(Optional::get)//
                .collect(Collectors.toList());

        final AirspaceVolume obsVolume = airspaceVolumes.get(0);
        assertEquals("STD", obsVolume.getUpperLimitReference().get());
        assertEquals("STD", obsVolume.getLowerLimitReference().get());
        assertEquals(NumericMeasureImpl.builder().setValue(250d).setUom("FL").build(), obsVolume.getLowerLimit().get());
        assertEquals(NumericMeasureImpl.builder().setValue(350d).setUom("FL").build(), obsVolume.getUpperLimit().get());
        assertEquals(worldPolygonCoords, ((PolygonGeometry) obsVolume.getHorizontalProjection().get()).getExteriorRingPositions());

        for (int i = 1; i < 4; i++) {
            final AirspaceVolume forecastVolume = airspaceVolumes.get(i);
            assertFalse(forecastVolume.getUpperLimitReference().isPresent());
            assertFalse(forecastVolume.getUpperLimit().isPresent());
            assertEquals("STD", forecastVolume.getLowerLimitReference().get());
            assertEquals(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build(), forecastVolume.getLowerLimit().get());
            assertEquals(worldPolygonCoords, ((PolygonGeometry) forecastVolume.getHorizontalProjection().get()).getExteriorRingPositions());
        }
    }

    @Test
    public void testInvalidTokenOrder() throws IOException {
        final String tac = getInput("spacewx-token-order.tac");
        final List<String> originalList = Arrays.asList(tac.split("\n"));
        final String comparisonTac = tac + "=";

        for (int x = 0; x < originalList.size(); x++) {
            final LinkedList<String> modifiedList = new LinkedList<>(originalList);
            modifiedList.addFirst(modifiedList.get(x));
            modifiedList.remove(x + 1);

            for (int i = 0; i < modifiedList.size() - 1; i++) {
                Collections.rotate(modifiedList.subList(i, i + 2), -1);

                if (modifiedList.get(i).startsWith("RMK:") //
                        && (modifiedList.get(i + 1).startsWith("STATUS:") || modifiedList.get(i + 1).startsWith("NR RPLC:"))) {
                    // Skip cases where optional labels are interpreted as a continuation of a remark
                    continue;
                }

                final String modifiedTac = String.join("\n", modifiedList) + "=";
                if (!comparisonTac.equals(modifiedTac)) {
                    final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(modifiedTac, TACConverter.TAC_TO_SWX_POJO);
                    if (result.getConversionIssues().isEmpty()) {
                        fail("Invalid token order not detected for TAC:\n" + modifiedTac);
                    }
                }
            }
        }
    }

    @Test
    public void testInvalidTokenOrder2() throws IOException {
        final String input = getInput("spacewx-invalid-token-order.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(10, result.getConversionIssues().size());
        assertTrue(result.getConversionIssues().stream()//
                .allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.ERROR && issue.getType() == ConversionIssue.Type.SYNTAX && issue.getMessage()
                        .contains("Invalid token order")));
    }

    @Test
    public void testInvalidTokenOrder3() throws IOException {
        final String input = getInput("spacewx-invalid-token-order-fcst.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(4, result.getConversionIssues().size());
        assertTrue(result.getConversionIssues().stream()//
                .allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.ERROR && issue.getType() == ConversionIssue.Type.SYNTAX && issue.getMessage()
                        .contains("Invalid token order")));
    }

    @Test
    public void testInvalidPhenomenonTokenOrder() throws IOException {
        final String input = getInput("spacewx-invalid-phenomenon-token-order.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(2, result.getConversionIssues().size());

        final ConversionIssue obsIssue = result.getConversionIssues().get(0);
        assertEquals(obsIssue.getSeverity(), ConversionIssue.Severity.ERROR);
        assertEquals(obsIssue.getType(), ConversionIssue.Type.SYNTAX);
        assertEquals("Invalid token order: ''HSH'(SWX_PHENOMENON_PRESET_LOCATION,OK)' was found after one of type SWX_PHENOMENON_LONGITUDE_LIMIT",
                obsIssue.getMessage());

        final ConversionIssue forecastIssue = result.getConversionIssues().get(1);
        assertEquals(forecastIssue.getSeverity(), ConversionIssue.Severity.ERROR);
        assertEquals(forecastIssue.getType(), ConversionIssue.Type.SYNTAX);
        assertEquals("Invalid token order: ''ABV FL340'(SWX_PHENOMENON_VERTICAL_LIMIT,OK)' was found after one of type POLYGON_COORDINATE_PAIR",
                forecastIssue.getMessage());
    }

    @Test
    public void testInvalidForecastHours() throws IOException {
        final String input = getInput("spacewx-invalid-forecast-hours.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(4, result.getConversionIssues().size());
        assertTrue(result.getConversionIssues().stream()//
                .allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.ERROR && issue.getType() == ConversionIssue.Type.SYNTAX));
        assertEquals(result.getConversionIssues().get(0).getMessage(), "Invalid forecast hour: +0 HR");
        assertEquals(result.getConversionIssues().get(1).getMessage(), "Invalid forecast hour: +15 HR");
        assertEquals(result.getConversionIssues().get(2).getMessage(), "Invalid token order: forecast +12 HR after forecast +15 HR");
        assertEquals(result.getConversionIssues().get(3).getMessage(), "Invalid forecast hour: +27 HR");
    }

    @Test
    public void testLatitudeBands() throws IOException {
        final String input = getInput("spacewx-latitude-bands.tac");
        final List<Double> expected = Arrays.asList(30.0, -150.0, 0.0, -150.0, 0.0, -30.0, 30.0, -30.0, 30.0, -150.0);
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertEquals(expected, geom.getExteriorRingPositions());
    }

    @Test
    public void testIssuingCenter() throws IOException {
        final String input = getInput("spacewx-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);

        final IssuingCenterImpl expected = IssuingCenterImpl.builder()//
                .setName("DONLON")//
                .setType("OTHER:SWXC")//
                .build();
        assertTrue(result.getConvertedMessage().isPresent());
        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals(expected, swx.getIssuingCenter());
    }

    @Test
    public void testNilRemark() throws IOException {
        final String input = getInput("spacewx-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConvertedMessage().isPresent());
        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertFalse(swx.getRemarks().isPresent());
    }

    @Test
    public void testDoubleNilRemark() throws IOException {
        final String input = getInput("spacewx-double-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConvertedMessage().isPresent());
        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertTrue(swx.getRemarks().isPresent());
    }

    @Test
    public void testOperationalPermissibleUsage() throws IOException {
        final String input = getInput("spacewx-pecasus-noswx.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConvertedMessage().isPresent());
        final SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertFalse(swx.getPermissibleUsageReason().isPresent());
        assertTrue(swx.getPermissibleUsage().isPresent());
        assertEquals(AviationCodeListUser.PermissibleUsage.OPERATIONAL, swx.getPermissibleUsage().get());
    }

    @Test
    public void testCoordinatePair() throws Exception {
        final String input = getInput("spacewx-coordinate-list.tac");
        final List<Double> expected = Arrays.asList(20.0, -105.0, 20.0, 30.0, -40.0, 30.0, -40.0, -105.0, 20.0, -105.0);
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertEquals(1, analysis.getRegions().size());
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertEquals(expected, geom.getExteriorRingPositions());
    }

    @Test
    public void testLongitudesWithoutSpacesAroundDashes() throws Exception {
        final String spacedInput = getInput("spacewx-latitude-bands.tac");
        final String spacelessInput = getInput("spacewx-latitude-bands-longitudes-spaceless.tac");
        final ConversionResult<SpaceWeatherAdvisory> spacedResult = this.converter.convertMessage(spacedInput, TACConverter.TAC_TO_SWX_POJO);
        final ConversionResult<SpaceWeatherAdvisory> spacelessResult = this.converter.convertMessage(spacelessInput, TACConverter.TAC_TO_SWX_POJO);

        assertTrue(spacedResult.getConversionIssues().isEmpty());
        assertTrue(spacelessResult.getConversionIssues().isEmpty());
        assertTrue(spacedResult.getConvertedMessage().isPresent());
        assertTrue(spacelessResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory spaced = SpaceWeatherAdvisoryImpl.Builder.from(spacedResult.getConvertedMessage().get()).setTranslatedTAC("").build();
        final SpaceWeatherAdvisory spaceless = SpaceWeatherAdvisoryImpl.Builder.from(spacelessResult.getConvertedMessage().get()).setTranslatedTAC("").build();
        assertEquals(spaced, spaceless);
    }

    @Test
    public void testDaylightSide() throws Exception {
        final String input = getInput("spacewx-daylight-side.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertEquals(1, analysis.getRegions().size());
        final SpaceWeatherRegion region = analysis.getRegions().get(0);
        assertEquals(SpaceWeatherRegion.SpaceWeatherLocation.DAYLIGHT_SIDE, region.getLocationIndicator().get());
        assertFalse(region.getAirSpaceVolume().isPresent());
        assertFalse(region.getLongitudeLimitMinimum().isPresent());
        assertFalse(region.getLongitudeLimitMaximum().isPresent());
    }

    @Test
    public void testPrecisePolygonCoordinates() throws Exception {
        final String input = getInput("spacewx-precise-polygon-coordinates.tac");
        final List<Double> expected = Arrays.asList(-20.0, -170.0, -20.0, -130.0, -10.0, -130.0, -10.0, -170.0, -20.0, -170.0);
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertEquals(1, analysis.getRegions().size());
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertEquals(expected, geom.getExteriorRingPositions());
    }

    @Test
    public void testPrecisePolygonCoordinates2() throws Exception {
        final String input = getInput("spacewx-precise-polygon-coordinates-2.tac");
        final List<Double> expected = Arrays.asList(-20.0, -170.22, -20.01, -130.51, -10.99, -130.02, -11.03, -170.99, -20.0, -170.0);
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertEquals(1, analysis.getRegions().size());
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertEquals(expected, geom.getExteriorRingPositions());
    }

    @Test
    public void testPolygonCoordinateLeniency() throws Exception {
        final String input = getInput("spacewx-invalid-polygon-coordinates.tac");
        final List<Double> expected = Arrays.asList(-20.0, -170.0, -20.0, -130.0, -10.0, -130.0, -10.0, -170.0, -20.0, -170.0);
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertEquals(1, analysis.getRegions().size());
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertEquals(expected, geom.getExteriorRingPositions());
    }

    @Test
    public void testInvalidIssueTimeDay() throws IOException {
        final String input = getInput("spacewx-invalid-issue-time-day.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX //
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Lexing problem with '20200100/1310Z'")));
    }

    @Test
    public void testInvalidIssueTimeHour() throws IOException {
        final String input = getInput("spacewx-invalid-issue-time-hour.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX //
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Lexing problem with '20200101/2400Z'")));
    }

    @Test
    public void testInvalidNextAdvisoryMonth() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-month.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components")));
    }

    @Test
    public void testInvalidNextAdvisoryDayZero() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-day-zero.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components")));
    }

    @Test
    public void testInvalidNextAdvisoryDay33() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-day-33.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components")));
    }

    @Test
    public void testInvalidNextAdvisoryHour() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-hour.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components")));
    }

    @Test
    public void testInvalidObservationDay() throws IOException {
        final String input = getInput("spacewx-invalid-obs-day.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Invalid analysis time in OBS SWX:")));
    }

    @Test
    public void testInvalidObservationTime() throws IOException {
        final String input = getInput("spacewx-invalid-obs-hour.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertFalse(result.getConversionIssues().isEmpty());
        assertTrue(result.getConversionIssues().stream()//
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Invalid analysis time in OBS SWX:")));
    }

    private String getInput(final String fileName) throws IOException {
        try (InputStream is = SWXReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }
}
