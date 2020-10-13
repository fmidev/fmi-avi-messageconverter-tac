package fi.fmi.avi.converter.tac.swx;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

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
        final Double[] expected = { -90d, -180d, -60d, -180d, -60d, 180d, -90d, 180d, -90d, -180d };
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

        Double[] expected = { -90d, -180d, -60d, -180d, -60d, 180d, -90d, 180d, -90d, -180d };
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

        expected = new Double[] { 60d, -180d, 90d, -180d, 90d, 180d, 60d, 180d, 60d, -180d };
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
        PolygonGeometry poly = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        Double[] expected = { -90d, -160d, -60d, -160d, -60d, 20d, -90d, 20d, -90d, -160d };
        Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));

        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
        assertEquals(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build(),
                analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().get());

        assertTrue(analysis.getRegions().get(1).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegions().get(1).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE);
        assertTrue(analysis.getRegions().get(1).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegions().get(1).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(analysis.getRegions().get(1).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        poly = (PolygonGeometry) analysis.getRegions().get(1).getAirSpaceVolume().get().getHorizontalProjection().get();
        expected = new Double[] { 60d, -160d, 90d, -160d, 90d, 20d, 60d, 20d, 60d, -160d };
        actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));

        analysis = analyses.get(1);
        assertEquals(analysis.getAnalysisType(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertEquals(1, analysis.getRegions().size());
        assertFalse(analysis.getRegions().get(0).getLocationIndicator().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(PolygonGeometry.class.isAssignableFrom(analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        poly = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        expected = new Double[] { -80d, -180d, -70d, -75d, -60d, 15d, -70d, 75d, -80d, -180d };
        actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
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
        poly = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        expected = new Double[] { -90d, -160d, -60d, -160d, -60d, 20d, -90d, 20d, -90d, -160d };
        actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
        assertEquals(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build(),
                analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit().get());
        assertTrue(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit().isPresent());
        assertEquals(NumericMeasureImpl.builder().setValue(370d).setUom("FL").build(),
                analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit().get());
    }

    @Test
    public void testBadMessage() throws IOException {
        final String input = getInput("spacewx-pecasus-notavbl.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(ConversionResult.Status.FAIL, result.getStatus());
        assertEquals(15, result.getConversionIssues().size());
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
        assertEquals(11, result.getConversionIssues().size());
        assertEquals(ConversionIssue.Type.MISSING_DATA, result.getConversionIssues().get(3).getType());
        assertTrue(result.getConversionIssues().get(5).getMessage().contains("Advisory status label was found, but the status could not be parsed in message"));
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
        ConversionIssue issue = result.getConversionIssues().get(2);
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
        assertTrue(result.getConversionIssues().get(2).getMessage().contains("Replace Advisory number label was found, but the data could not be parsed in "
                + "message"));
    }

    @Test
    public void testInvalidEndTokenMisplaced() throws IOException {
        final String input = getInput("spacewx-invalid-misplaced-end-token.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(1, result.getConversionIssues().size() );
        assertEquals(ConversionIssue.Severity.ERROR, result.getConversionIssues().get(0).getSeverity());
        assertEquals(ConversionIssue.Type.SYNTAX, result.getConversionIssues().get(0).getType());
        assertEquals("Message does not end in end token", result.getConversionIssues().get(0).getMessage());
    }

    @Test
    public void testNilRemark() throws IOException {
        final String input = getInput("spacewx-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConvertedMessage().isPresent());
        SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertFalse(swx.getRemarks().isPresent());
    }

    @Test
    public void testDoubleNilRemark() throws IOException {
        final String input = getInput("spacewx-double-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConvertedMessage().isPresent());
        SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertTrue(swx.getRemarks().isPresent());
    }

    @Test
    public void testOperationalPermissibleUsage() throws IOException {
        final String input = getInput("spacewx-pecasus-noswx.tac");
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConvertedMessage().isPresent());
        SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertFalse(swx.getPermissibleUsageReason().isPresent());
        assertTrue(swx.getPermissibleUsage().isPresent());
        assertEquals(AviationCodeListUser.PermissibleUsage.OPERATIONAL, swx.getPermissibleUsage().get());
    }

    private String getInput(final String fileName) throws IOException {
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
}
