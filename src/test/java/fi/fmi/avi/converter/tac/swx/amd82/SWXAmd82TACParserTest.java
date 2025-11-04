package fi.fmi.avi.converter.tac.swx.amd82;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.CircleByCenterPointImpl;
import fi.fmi.avi.model.immutable.CoordinateReferenceSystemImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.swx.amd82.*;
import fi.fmi.avi.model.swx.amd82.immutable.AirspaceVolumeImpl;
import fi.fmi.avi.model.swx.amd82.immutable.IssuingCenterImpl;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAmd82Impl;
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
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd82TACParserTest {

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
                       + "FCST SWX +18 HR: 09/0600Z DAYSIDE\n"//
                       + "FCST SWX +24 HR: 09/1200Z NO SWX EXP\n"//
                       + "RMK: TEST TEST TEST TEST\n"
                       + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" //
                       + "SEE WWW.PECASUS.ORG \n"
                       + "NXT ADVISORY: WILL BE ISSUED BY 20161108/0700Z\n \n",
        */

    private static String withReplaceNumbers(final String tac, final String replacementNumbers) {
        return tac.replaceAll("(?m)^NR\\s+RPLC:\\s+.*$", "NR RPLC: " + replacementNumbers);
    }

    private static String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd82ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Test
    public void testParser1() throws Exception {
        final String input = getInput("spacewx-A2-3.tac");

        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(result.getConversionIssues()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).isPresent();

        final SpaceWeatherAdvisoryAmd82 swx = result.getConvertedMessage().get();
        assertThat(swx.getPermissibleUsageReason()).hasValue(AviationCodeListUser.PermissibleUsageReason.EXERCISE);
        assertThat(swx.getIssuingCenter().getName()).hasValue("DONLON");
        assertThat(swx.getAdvisoryNumber().getSerialNumber()).isEqualTo(2);
        assertThat(swx.getAdvisoryNumber().getYear()).isEqualTo(2016);
        assertThat(swx.getPhenomena())
                .containsExactly(
                        SpaceWeatherPhenomenon.fromCombinedCode("HF COM MOD"),
                        SpaceWeatherPhenomenon.fromCombinedCode("GNSS MOD"));

        final String[] expectedRemarks = {"LOW", "LVL", "GEOMAGNETIC", "STORMING", "CAUSING", "INCREASED", "AURORAL", "ACT", "AND", "SUBSEQUENT", "MOD",
                "DEGRADATION", "OF", "GNSS", "AND", "HF", "COM", "AVBL", "IN", "THE", "AURORAL", "ZONE.", "THIS", "STORMING", "EXP", "TO", "SUBSIDE", "IN",
                "THE", "FCST", "PERIOD.", "SEE", "WWW.SPACEWEATHERPROVIDER.WEB"};
        assertThat(swx.getRemarks().get()).containsExactly(expectedRemarks);
        assertThat(swx.getNextAdvisory().getTimeSpecifier()).isEqualTo(NextAdvisory.Type.NO_FURTHER_ADVISORIES);

        final List<SpaceWeatherAdvisoryAnalysis> analyses = swx.getAnalyses();
        assertThat(analyses).hasSize(5);

        final ZonedDateTime observationTime = ZonedDateTime.parse("2016-11-08T01:00Z");
        for (int i = 0; i < 5; i++) {
            assertThat(analyses.get(i).getTime().getCompleteTime()).hasValue(observationTime.plusHours(i * 6));
        }


        SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertThat(analysis.getRegions()).hasSize(2);

        assertThat(analysis.getRegions().get(0).getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume())
                .hasValueSatisfying(volume ->
                        assertThat(volume.getHorizontalProjection()).hasValueSatisfying(projection ->
                                assertThat(projection).isInstanceOf(PolygonGeometry.class)));
        final PolygonGeometry poly = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        final Double[] expected = {90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d};
        final Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertThat(actual).isEqualTo(expected);

        analysis = analyses.get(4);
        assertThat(analysis.getNilPhenomenonReason())
                .hasValue(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED);
        assertThat(swx.getNextAdvisory().getTimeSpecifier()).isEqualTo(NextAdvisory.Type.NO_FURTHER_ADVISORIES);
        assertThat(swx.getReplaceAdvisoryNumbers())
                .hasSize(1)
                .first()
                .satisfies(advisory -> {
                    assertThat(advisory.getSerialNumber()).isEqualTo(1);
                    assertThat(advisory.getYear()).isEqualTo(2016);
                });
    }

    @Test
    public void testParser2() throws Exception {
        final String input = getInput("spacewx-A2-4.tac");

        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(result.getConversionIssues()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).isPresent();

        final SpaceWeatherAdvisoryAmd82 swx = result.getConvertedMessage().get();
        assertThat(swx.getPermissibleUsage())
                .hasValue(AviationCodeListUser.PermissibleUsage.NON_OPERATIONAL);
        assertThat(swx.getPermissibleUsageReason()).hasValue(AviationCodeListUser.PermissibleUsageReason.TEST);
        assertThat(swx.getIssueTime().get().getCompleteTime()).hasValue(ZonedDateTime.parse("2016-11-08T00:00Z"));
        assertThat(swx.getIssuingCenter().getName()).hasValue("DONLON");
        assertThat(swx.getAdvisoryNumber().getSerialNumber()).isEqualTo(2);
        assertThat(swx.getAdvisoryNumber().getYear()).isEqualTo(2016);
        assertThat(swx.getPhenomena())
                .containsExactly(SpaceWeatherPhenomenon.fromCombinedCode("RADIATION MOD"));
        final String[] expectedRemarks = {"RADIATION", "LVL", "EXCEEDED", "100", "PCT", "OF", "BACKGROUND", "LVL", "AT", "FL340", "AND", "ABV.", "THE",
                "CURRENT", "EVENT", "HAS", "PEAKED", "AND", "LVL", "SLW", "RTN", "TO", "BACKGROUND", "LVL.", "SEE", "WWW.SPACEWEATHERPROVIDER.WEB"};
        assertThat(swx.getRemarks().get()).containsExactly(expectedRemarks);
        assertThat(swx.getNextAdvisory().getTimeSpecifier()).isEqualTo(NextAdvisory.Type.NO_FURTHER_ADVISORIES);

        final List<SpaceWeatherAdvisoryAnalysis> analyses = swx.getAnalyses();
        assertThat(analyses).hasSize(5);

        final ZonedDateTime observationTime = ZonedDateTime.parse("2016-11-08T01:00Z");
        for (int i = 0; i < 5; i++) {
            assertThat(analyses.get(i).getTime().getCompleteTime()).hasValue(observationTime.plusHours(i * 6));
        }


        final SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertThat(analysis.getRegions()).hasSize(2);

        //1st region:
        SpaceWeatherRegion r = analysis.getRegions().get(0);

        assertThat(r.getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertThat(r.getAirSpaceVolume())
                .hasValueSatisfying(volume ->
                        assertThat(volume.getHorizontalProjection()).hasValueSatisfying(projection ->
                                assertThat(projection).isInstanceOf(PolygonGeometry.class)));
        PolygonGeometry poly = (PolygonGeometry) r.getAirSpaceVolume().get().getHorizontalProjection().get();

        Double[] expected = {90d, -180d, 60d, -180d, 60d, 180d, 90d, 180d, 90d, -180d};
        Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertThat(actual).isEqualTo(expected);

        assertThat(r.getAirSpaceVolume().get().getLowerLimit())
                .hasValueSatisfying(limit -> {
                    assertThat(limit.getValue()).isEqualTo(340.0);
                    assertThat(limit.getUom()).isEqualTo("FL");
                });
        assertThat(r.getAirSpaceVolume().get().getLowerLimitReference()).hasValue("STD");

        //2nd region:
        r = analysis.getRegions().get(1);
        assertThat(r.getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE);
        assertThat(r.getAirSpaceVolume())
                .hasValueSatisfying(volume ->
                        assertThat(volume.getHorizontalProjection()).hasValueSatisfying(projection ->
                                assertThat(projection).isInstanceOf(PolygonGeometry.class)));
        poly = (PolygonGeometry) r.getAirSpaceVolume().get().getHorizontalProjection().get();

        expected = new Double[]{-60d, -180d, -90d, -180d, -90d, 180d, -60d, 180d, -60d, -180d};
        actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertThat(actual).isEqualTo(expected);

        assertThat(r.getAirSpaceVolume().get().getLowerLimit())
                .hasValueSatisfying(limit -> {
                    assertThat(limit.getValue()).isEqualTo(340.0);
                    assertThat(limit.getUom()).isEqualTo("FL");
                });
        assertThat(r.getAirSpaceVolume().get().getLowerLimitReference()).hasValue("STD");

    }

    @Test
    public void testAdvancedMessage() throws IOException {
        final String input = getInput("spacewx-advanced.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        assertThat(result.getConvertedMessage()).isPresent();

        final SpaceWeatherAdvisoryAmd82 swx = result.getConvertedMessage().get();
        assertThat(swx.getIssuingCenter().getName())
                .hasValue("PECASUS");
        assertThat(swx.getAdvisoryNumber().getSerialNumber()).isEqualTo(1);
        assertThat(swx.getAdvisoryNumber().getYear()).isEqualTo(2019);
        assertThat(swx.getPhenomena())
                .containsExactly(
                        SpaceWeatherPhenomenon.fromCombinedCode("SATCOM MOD"),
                        SpaceWeatherPhenomenon.fromCombinedCode("RADIATION SEV"));
        assertThat(swx.getRemarks()).isPresent();
        final String[] expectedRemarks = {"TEST", "TEST", "TEST", "TEST", "THIS", "IS", "A", "TEST", "MESSAGE", "FOR", "TECHNICAL", "TEST.", "SEE",//
                "WWW.PECASUS.ORG"};
        assertThat(swx.getRemarks().get()).containsExactly(expectedRemarks);
        assertThat(swx.getNextAdvisory().getTimeSpecifier()).isEqualTo(NextAdvisory.Type.NEXT_ADVISORY_BY);

        final List<SpaceWeatherAdvisoryAnalysis> analyses = swx.getAnalyses();
        assertThat(analyses).hasSize(5);

        SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertThat(analysis.getRegions()).hasSize(2);

        assertThat(analysis.getRegions().get(0).getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume())
                .hasValueSatisfying(volume ->
                        assertThat(volume.getHorizontalProjection()).hasValueSatisfying(projection ->
                                assertThat(projection).isInstanceOf(PolygonGeometry.class)));
        PolygonGeometry geometry = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        List<Double> expectedExteriorRingPositions = Arrays.asList(90d, 160d, 60d, 160d, 60d, -20d, 90d, -20d, 90d, 160d);
        //Double[] expected = { 90d, 160d, 60d, 160d, 60d, -20d, 90d, -20d, 90d, 160d };
        List<Double> actualExteriorRingPositions = geometry.getExteriorRingPositions();
        assertThat(actualExteriorRingPositions).isEqualTo(expectedExteriorRingPositions);

        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit())
                .hasValueSatisfying(limit -> assertThat(limit).isEqualTo(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build()));
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimitReference()).isPresent();
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit()).isEmpty();
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimitReference()).isEmpty();

        assertThat(analysis.getRegions().get(1).getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE);
        assertThat(analysis.getRegions().get(1).getAirSpaceVolume())
                .hasValueSatisfying(volume ->
                        assertThat(volume.getHorizontalProjection()).hasValueSatisfying(projection ->
                                assertThat(projection).isInstanceOf(PolygonGeometry.class)));
        geometry = (PolygonGeometry) analysis.getRegions().get(1).getAirSpaceVolume().get().getHorizontalProjection().get();
        expectedExteriorRingPositions = Arrays.asList(-60d, 160d, -90d, 160d, -90d, -20d, -60d, -20d, -60d, 160d);
        actualExteriorRingPositions = geometry.getExteriorRingPositions();
        assertThat(actualExteriorRingPositions).isEqualTo(expectedExteriorRingPositions);

        analysis = analyses.get(1);
        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertThat(analysis.getRegions()).hasSize(1);
        assertThat(analysis.getRegions().get(0).getLocationIndicator()).isEmpty();
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume())
                .hasValueSatisfying(volume ->
                        assertThat(volume.getHorizontalProjection()).hasValueSatisfying(projection ->
                                assertThat(projection).isInstanceOf(PolygonGeometry.class)));
        final PolygonGeometry poly = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        final Double[] expected = new Double[]{80d, -180d, 70d, -75d, 60d, 15d, 70d, 75d, 80d, -180d};
        final Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertThat(actual).isEqualTo(expected);
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit())
                .hasValueSatisfying(limit -> assertThat(limit).isEqualTo(NumericMeasureImpl.builder().setValue(370d).setUom("FL").build()));
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimitReference()).isPresent();
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit()).isEmpty();
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimitReference()).isEmpty();

        analysis = analyses.get(2);

        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertThat(analysis.getNilPhenomenonReason())
                .hasValue(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_PHENOMENON_EXPECTED);

        analysis = analyses.get(3);

        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertThat(analysis.getRegions()).hasSize(1);
        assertThat(analysis.getRegions().get(0).getLocationIndicator())
                .hasValue(SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE);

        analysis = analyses.get(4);

        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertThat(analysis.getRegions()).hasSize(1);
        assertThat(analysis.getRegions().get(0).getLocationIndicator()).hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume())
                .hasValueSatisfying(volume ->
                        assertThat(volume.getHorizontalProjection()).hasValueSatisfying(projection ->
                                assertThat(projection).isInstanceOf(PolygonGeometry.class)));
        geometry = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        expectedExteriorRingPositions = Arrays.asList(90d, 160d, 60d, 160d, 60d, -20d, 90d, -20d, 90d, 160d);
        actualExteriorRingPositions = geometry.getExteriorRingPositions();
        assertThat(actualExteriorRingPositions).isEqualTo(expectedExteriorRingPositions);
        assertThat(actual).isEqualTo(expected);
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimit())
                .hasValueSatisfying(limit -> assertThat(limit).isEqualTo(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build()));
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getLowerLimitReference()).isPresent();
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimit())
                .hasValueSatisfying(limit -> assertThat(limit).isEqualTo(NumericMeasureImpl.builder().setValue(370d).setUom("FL").build()));
        assertThat(analysis.getRegions().get(0).getAirSpaceVolume().get().getUpperLimitReference()).isPresent();
    }

    @Test
    public void testBadMessage() throws IOException {
        final String input = getInput("spacewx-pecasus-notavbl.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.FAIL);
        assertThat(result.getConversionIssues()).hasSize(16);
        assertThat(result.getConvertedMessage()).isEmpty();
    }

    //@Ignore("suddenly fails after sigmet parser additions")
    @Test
    public void testInvalidMinimalMessage() throws IOException {
        final String input = getInput("spacewx-invalid-minimal.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
    }

    @Test
    public void testInvalidMissingEndToken() throws IOException {
        final String input = getInput("spacewx-invalid-missing-end-token.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(1);
        assertThat(result.getConversionIssues().get(0).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).isEqualTo("Message does not end in end token");
    }

    @Test
    public void testEndTokenAtEndAndRandomPlace() throws IOException {
        final String input = getInput("spacewx-invalid-end-token-at-end-and-elsewhere.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConvertedMessage()).isEmpty();
        assertThat(result.getConversionIssues()).hasSize(1);
        assertThat(result.getConversionIssues().get(0).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).startsWith("Message has an extra end token");
    }

    @Test
    public void testInvalidStatusLabel() throws IOException {
        final String input = getInput("spacewx-invalid-status-label.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(3);
        assertThat(result.getConversionIssues().get(0).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).contains("Input message lexing was not fully successful");
    }

    @Test
    public void testInvalidEmptyStatus() throws IOException {
        final String input = getInput("spacewx-invalid-status-empty.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(12);
        assertThat(result.getConversionIssues().get(3).getType()).isEqualTo(ConversionIssue.Type.MISSING_DATA);
        assertThat(result.getConversionIssues())
                .extracting(ConversionIssue::getMessage)
                .anyMatch(s -> s.contains("Advisory status label was found, but the status could not be parsed in message"));
    }

    @Test
    public void testInvalidRemarkLabel() throws IOException {
        final String input = getInput("spacewx-invalid-remark-label.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(36);
        assertThat(result.getConversionIssues().get(0).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).contains("Input message lexing was not fully successful");
    }

    @Test
    public void testInvalidRmkLabel() throws IOException {
        final String input = getInput("spacewx-invalid-rmk-label.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(6);
        assertThat(result.getConversionIssues().get(1).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(1).getMessage()).isEqualTo("Lexing problem with 'RMK'");

        assertThat(result.getConversionIssues().get(5).getType()).isEqualTo(ConversionIssue.Type.MISSING_DATA);
        assertThat(result.getConversionIssues().get(5).getMessage()).contains("One of REMARKS_START required in message");
    }

    @Test
    public void testInvalidReplaceNumberLabel() throws IOException {
        final String input = getInput("spacewx-invalid-replace-number-label.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(4);
        assertThat(result.getConversionIssues().get(0).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).contains("Input message lexing was not fully successful");
    }

    @Test
    public void testInvalidNextAdvisoryLabelWithoutRemarks() throws IOException {
        final String input = getInput("spacewx-invalid-next-advisory-label.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(8);
        assertThat(result.getConversionIssues().get(0).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).contains("Input message lexing was not fully successful");
    }

    @Test
    public void testInvalidAdvisoryNumber() throws IOException {
        final String input = getInput("spacewx-invalid-advisory-number.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(4);
        final ConversionIssue issue = result.getConversionIssues().get(2);
        assertThat(issue.getType()).isEqualTo(ConversionIssue.Type.MISSING_DATA);
        assertThat(issue.getSeverity()).isEqualTo(ConversionIssue.Severity.ERROR);
        assertThat(issue.getMessage()).contains("One of ADVISORY_NUMBER required in message");
    }

    @Test
    public void testInvalidReplaceNumber() throws IOException {
        final String input = getInput("spacewx-invalid-replace-number.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(3);
        assertThat(result.getConversionIssues().get(2).getType()).isEqualTo(ConversionIssue.Type.MISSING_DATA);
        assertThat(result.getConversionIssues().get(2).getMessage()).contains("Replace advisory number is missing");
    }

    @Test
    public void testInvalidEndTokenMisplaced() throws IOException {
        final String input = getInput("spacewx-invalid-misplaced-end-token.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(1);
        assertThat(result.getConversionIssues().get(0).getSeverity()).isEqualTo(ConversionIssue.Severity.ERROR);
        assertThat(result.getConversionIssues().get(0).getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).isEqualTo("Message does not end in end token");
    }

    @Test
    public void testInvalidDuplicateSwxc() throws IOException {
        final String input = getInput("spacewx-invalid-duplicate-swxc.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(1);
        final ConversionIssue issue = result.getConversionIssues().get(0);
        assertThat(issue.getSeverity()).isEqualTo(ConversionIssue.Severity.ERROR);
        assertThat(issue.getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(issue.getMessage()).contains("More than one of SWX_CENTRE");
    }

    @Test
    public void testInvalidMissingLatitudeBands() throws IOException {
        final String input = getInput("spacewx-invalid-missing-latitude-bands.tac");
        final List<Double> worldPolygonCoords = Arrays.asList(-90d, -180d, 90d, -180d, 90d, 180d, -90d, 180d, -90d, -180d);

        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(4);
        assertThat(result.getConversionIssues()).allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.WARNING && issue.getType() == ConversionIssue.Type.MISSING_DATA);
        assertThat(result.getConvertedMessage()).isPresent();

        final List<AirspaceVolume> airspaceVolumes = result.getConvertedMessage().get().getAnalyses().stream()//
                .flatMap(analysis -> analysis.getRegions().stream())//
                .map(SpaceWeatherRegion::getAirSpaceVolume).map(Optional::get)//
                .collect(Collectors.toList());

        final AirspaceVolume obsVolume = airspaceVolumes.get(0);
        assertThat(obsVolume.getUpperLimitReference()).hasValue("STD");
        assertThat(obsVolume.getLowerLimitReference()).hasValue("STD");
        assertThat(obsVolume.getLowerLimit()).hasValue(NumericMeasureImpl.builder().setValue(250d).setUom("FL").build());
        assertThat(obsVolume.getUpperLimit()).hasValue(NumericMeasureImpl.builder().setValue(350d).setUom("FL").build());
        assertThat(((PolygonGeometry) obsVolume.getHorizontalProjection().get()).getExteriorRingPositions())
                .isEqualTo(Arrays.asList(-90d, -150d, 90d, -150d, 90d, -30d, -90d, -30d, -90d, -150d));

        for (int i = 1; i < 4; i++) {
            final AirspaceVolume forecastVolume = airspaceVolumes.get(i);
            assertThat(forecastVolume.getUpperLimitReference()).isEmpty();
            assertThat(forecastVolume.getUpperLimit()).isEmpty();
            assertThat(forecastVolume.getLowerLimitReference()).hasValue("STD");
            assertThat(forecastVolume.getLowerLimit()).hasValue(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build());
            assertThat(((PolygonGeometry) forecastVolume.getHorizontalProjection().get()).getExteriorRingPositions()).isEqualTo(worldPolygonCoords);
        }
    }

    @Test
    public void testInvalidMissingLatitudeBandsAndLongitudes() throws IOException {
        final String input = getInput("spacewx-invalid-missing-latitude-bands-longitudes.tac");
        final List<Double> worldPolygonCoords = Arrays.asList(-90d, -180d, 90d, -180d, 90d, 180d, -90d, 180d, -90d, -180d);

        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(4);
        assertThat(result.getConversionIssues()).allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.WARNING && issue.getType() == ConversionIssue.Type.MISSING_DATA);
        assertThat(result.getConvertedMessage()).isPresent();

        final List<AirspaceVolume> airspaceVolumes = result.getConvertedMessage().get().getAnalyses().stream()//
                .flatMap(analysis -> analysis.getRegions().stream())//
                .map(SpaceWeatherRegion::getAirSpaceVolume).map(Optional::get)//
                .collect(Collectors.toList());

        final AirspaceVolume obsVolume = airspaceVolumes.get(0);
        assertThat(obsVolume.getUpperLimitReference()).hasValue("STD");
        assertThat(obsVolume.getLowerLimitReference()).hasValue("STD");
        assertThat(obsVolume.getLowerLimit()).hasValue(NumericMeasureImpl.builder().setValue(250d).setUom("FL").build());
        assertThat(obsVolume.getUpperLimit()).hasValue(NumericMeasureImpl.builder().setValue(350d).setUom("FL").build());
        assertThat(((PolygonGeometry) obsVolume.getHorizontalProjection().get()).getExteriorRingPositions()).isEqualTo(worldPolygonCoords);

        for (int i = 1; i < 4; i++) {
            final AirspaceVolume forecastVolume = airspaceVolumes.get(i);
            assertThat(forecastVolume.getUpperLimitReference()).isEmpty();
            assertThat(forecastVolume.getUpperLimit()).isEmpty();
            assertThat(forecastVolume.getLowerLimitReference()).hasValue("STD");
            assertThat(forecastVolume.getLowerLimit()).hasValue(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build());
            assertThat(((PolygonGeometry) forecastVolume.getHorizontalProjection().get()).getExteriorRingPositions()).isEqualTo(worldPolygonCoords);
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
                    final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(modifiedTac, TACConverter.TAC_TO_SWX_AMD82_POJO);
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
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(8);
        assertThat(result.getConversionIssues()).allMatch(issue ->
                issue.getSeverity() == ConversionIssue.Severity.ERROR
                        && issue.getType() == ConversionIssue.Type.SYNTAX
                        && issue.getMessage().contains("Invalid token order"));
    }

    @Test
    public void testInvalidTokenOrder3() throws IOException {
        final String input = getInput("spacewx-invalid-token-order-fcst.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(4);
        assertThat(result.getConversionIssues()).allMatch(issue ->
                issue.getSeverity() == ConversionIssue.Severity.ERROR
                        && issue.getType() == ConversionIssue.Type.SYNTAX
                        && issue.getMessage().contains("Invalid token order"));
    }

    @Test
    public void testInvalidTokenOrderEffect() throws IOException {
        final String input = getInput("spacewx-invalid-token-order-effect.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(2);
        assertThat(result.getConversionIssues()).allMatch(issue ->
                issue.getSeverity() == ConversionIssue.Severity.ERROR
                        && issue.getType() == ConversionIssue.Type.SYNTAX
                        && issue.getMessage().contains("Invalid token order"));
    }

    @Test
    public void testInvalidPhenomenonTokenOrder() throws IOException {
        final String input = getInput("spacewx-invalid-phenomenon-token-order.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(2);

        final ConversionIssue obsIssue = result.getConversionIssues().get(0);
        assertThat(obsIssue.getSeverity()).isEqualTo(ConversionIssue.Severity.ERROR);
        assertThat(obsIssue.getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(obsIssue.getMessage()).isEqualTo("Invalid token order: ''HSH'(SWX_PHENOMENON_PRESET_LOCATION,OK)' was found after one of type SWX_PHENOMENON_LONGITUDE_LIMIT");

        final ConversionIssue forecastIssue = result.getConversionIssues().get(1);
        assertThat(forecastIssue.getSeverity()).isEqualTo(ConversionIssue.Severity.ERROR);
        assertThat(forecastIssue.getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
        assertThat(forecastIssue.getMessage()).isEqualTo("Invalid token order: ''ABV FL340'(SWX_PHENOMENON_VERTICAL_LIMIT,OK)' was found after one of type POLYGON_COORDINATE_PAIR");
    }

    @Test
    public void testInvalidForecastHours() throws IOException {
        final String input = getInput("spacewx-invalid-forecast-hours.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).hasSize(4);
        assertThat(result.getConversionIssues()).allMatch(issue -> issue.getSeverity() == ConversionIssue.Severity.ERROR && issue.getType() == ConversionIssue.Type.SYNTAX);
        assertThat(result.getConversionIssues().get(0).getMessage()).isEqualTo("Invalid forecast hour: +0 HR");
        assertThat(result.getConversionIssues().get(1).getMessage()).isEqualTo("Invalid forecast hour: +15 HR");
        assertThat(result.getConversionIssues().get(2).getMessage()).isEqualTo("Invalid token order: forecast +12 HR after forecast +15 HR");
        assertThat(result.getConversionIssues().get(3).getMessage()).isEqualTo("Invalid forecast hour: +27 HR");
    }

    @Test
    public void testLatitudeBands() throws IOException {
        final String input = getInput("spacewx-latitude-bands.tac");
        final List<Double> expected = Arrays.asList(30.0, -150.0, 0.0, -150.0, 0.0, -30.0, 30.0, -30.0, 30.0, -150.0);
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isEmpty();
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertThat(geom.getExteriorRingPositions()).isEqualTo(expected);
    }

    @Test
    public void testIssuingCenter() throws IOException {
        final String input = getInput("spacewx-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        final IssuingCenterImpl expected = IssuingCenterImpl.builder()//
                .setName("DONLON")//
                .setType("OTHER:SWXC")//
                .build();
        assertThat(result.getConvertedMessage()).isPresent();
        final SpaceWeatherAdvisoryAmd82 swx = result.getConvertedMessage().get();
        assertThat(swx.getIssuingCenter()).isEqualTo(expected);
    }

    @Test
    public void testNilRemark() throws IOException {
        final String input = getInput("spacewx-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConvertedMessage()).isPresent();
        final SpaceWeatherAdvisoryAmd82 swx = result.getConvertedMessage().get();
        assertThat(swx.getRemarks()).isEmpty();
    }

    @Test
    public void testDoubleNilRemark() throws IOException {
        final String input = getInput("spacewx-double-nil-remark.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConvertedMessage()).isPresent();
        final SpaceWeatherAdvisoryAmd82 swx = result.getConvertedMessage().get();
        assertThat(swx.getRemarks()).isPresent();
    }

    @Test
    public void testOperationalPermissibleUsage() throws IOException {
        final String input = getInput("spacewx-pecasus-noswx.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConvertedMessage()).isPresent();
        final SpaceWeatherAdvisoryAmd82 swx = result.getConvertedMessage().get();
        assertThat(swx.getPermissibleUsageReason()).isEmpty();
        assertThat(swx.getPermissibleUsage())
                .hasValue(AviationCodeListUser.PermissibleUsage.OPERATIONAL);
    }

    @Test
    public void testCoordinatePair() throws Exception {
        final String input = getInput("spacewx-coordinate-list.tac");
        final List<Double> expected = Arrays.asList(20.0, -105.0, 20.0, 30.0, -40.0, 30.0, -40.0, -105.0, 20.0, -105.0);
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isEmpty();
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertThat(analysis.getRegions()).hasSize(1);
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertThat(geom.getExteriorRingPositions()).isEqualTo(expected);
    }

    @Test
    public void testLongitudesWithoutSpacesAroundDashes() throws Exception {
        final String spacedInput = getInput("spacewx-latitude-bands.tac");
        final String spacelessInput = getInput("spacewx-latitude-bands-longitudes-spaceless.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> spacedResult = this.converter.convertMessage(spacedInput, TACConverter.TAC_TO_SWX_AMD82_POJO);
        final ConversionResult<SpaceWeatherAdvisoryAmd82> spacelessResult = this.converter.convertMessage(spacelessInput, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(spacedResult.getConversionIssues()).isEmpty();
        assertThat(spacelessResult.getConversionIssues()).isEmpty();
        assertThat(spacedResult.getConvertedMessage()).isPresent();
        assertThat(spacelessResult.getConvertedMessage()).isPresent();

        final SpaceWeatherAdvisoryAmd82 spaced = SpaceWeatherAdvisoryAmd82Impl.Builder.from(spacedResult.getConvertedMessage().get()).setTranslatedTAC("").build();
        final SpaceWeatherAdvisoryAmd82 spaceless = SpaceWeatherAdvisoryAmd82Impl.Builder.from(spacelessResult.getConvertedMessage().get()).setTranslatedTAC("").build();
        assertThat(spaceless).isEqualTo(spaced);
    }

    @Test
    public void testInvalidDaylightSide() throws Exception {
        final String input = getInput("spacewx-invalid-daylight-side.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues())
                .hasOnlyOneElementSatisfying(issue -> {
                    assertThat(issue.getMessage()).contains("DAYLIGHT SIDE");
                    assertThat(issue.getSeverity()).isEqualTo(ConversionIssue.Severity.WARNING);
                    assertThat(issue.getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
                });
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.WITH_WARNINGS);
    }

    @Test
    public void testDaySide() throws Exception {
        final String input = getInput("spacewx-dayside.tac");
        final AirspaceVolume expected = AirspaceVolumeImpl.builder()
                .setHorizontalProjection(
                        CircleByCenterPointImpl.builder()
                                .setCrs(CoordinateReferenceSystemImpl.wgs84())
                                .setCenterPointCoordinates(Arrays.asList(-16.64, 160.94))
                                .setRadius(NumericMeasureImpl.builder()
                                        .setValue(10100.0)
                                        .setUom("km")
                                        .build())
                                .build()
                ).build();

        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isEmpty();
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertThat(analysis.getRegions()).hasSize(3);

        assertThat(analysis.getRegions().get(0).getLocationIndicator()).hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertThat(analysis.getRegions().get(1).getLocationIndicator()).hasValue(SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE);

        final SpaceWeatherRegion dayside = analysis.getRegions().get(2);
        assertThat(dayside.getLocationIndicator()).hasValue(SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE);
        assertThat(dayside.getAirSpaceVolume())
                .hasValue(expected);
        assertThat(dayside.getLongitudeLimitMinimum()).isEmpty();
        assertThat(dayside.getLongitudeLimitMaximum()).isEmpty();
    }

    @Test
    public void testInvalidDayside() throws Exception {
        final String input = getInput("spacewx-invalid-dayside.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues())
                .hasOnlyOneElementSatisfying(issue -> {
                    assertThat(issue.getMessage()).contains(SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE.getCode());
                    assertThat(issue.getSeverity()).isEqualTo(ConversionIssue.Severity.WARNING);
                    assertThat(issue.getType()).isEqualTo(ConversionIssue.Type.SYNTAX);
                });
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.WITH_WARNINGS);
    }

    @Test
    public void testNightside() throws Exception {
        final String input = getInput("spacewx-nightside.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isEmpty();
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertThat(analysis.getRegions()).hasSize(5);
        final SpaceWeatherRegion region = analysis.getRegions().get(4);
        assertThat(region.getLocationIndicator()).hasValue(SpaceWeatherRegion.SpaceWeatherLocation.NIGHTSIDE);
        assertThat(region.getAirSpaceVolume()).isPresent();
        assertThat(region.getAirSpaceVolume().get().getHorizontalProjection()).isEmpty();
        assertThat(region.getLongitudeLimitMinimum()).isEmpty();
        assertThat(region.getLongitudeLimitMaximum()).isEmpty();
    }

    @Test
    public void testPrecisePolygonCoordinates() throws Exception {
        final String input = getInput("spacewx-precise-polygon-coordinates.tac");
        final List<Double> expected = Arrays.asList(-20.0, -170.0, -20.0, -130.0, -10.0, -130.0, -10.0, -170.0, -20.0, -170.0);
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isEmpty();
        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertThat(analysis.getRegions()).hasSize(1);
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertThat(geom.getExteriorRingPositions()).isEqualTo(expected);
    }

    @Test
    public void testPolygonCoordinateLeniency() throws Exception {
        final String input = getInput("spacewx-invalid-polygon-coordinates.tac");
        final List<Double> expected = Arrays.asList(-20.0, -170.0, -20.0, -130.0, -10.0, -130.0, -10.0, -170.0, -20.0, -170.0);
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.WITH_WARNINGS);
        assertThat(result.getConversionIssues().stream()
                .filter(issue -> issue.getMessage().contains("has invalid format"))
                .count()).isEqualTo(7);

        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertThat(analysis.getRegions()).hasSize(1);
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertThat(geom.getExteriorRingPositions()).isEqualTo(expected);
    }

    @Test
    public void testInvalidFractionalPolygonCoordinates() throws Exception {
        final String input = getInput("spacewx-invalid-fractional-coordinates.tac");
        final List<Double> expected = Arrays.asList(-20.0, -170.0, -20.0, -131.0, -11.0, -130.0, -11.0, -171.0, -20.0, -170.0);
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.WITH_WARNINGS);
        assertThat(result.getConversionIssues()).hasSize(15);

        assertThat(result.getConversionIssues().stream()
                .filter(issue -> issue.getMessage().contains("has invalid format"))
                .count()).isEqualTo(8);

        assertThat(result.getConversionIssues().stream()
                .filter(issue -> issue.getMessage().contains("contains fractional degrees"))
                .count()).isEqualTo(7);

        assertThat(result.getConversionIssues()).allMatch(issue ->
                issue.getSeverity() == ConversionIssue.Severity.WARNING &&
                        issue.getType() == ConversionIssue.Type.SYNTAX);

        final SpaceWeatherAdvisoryAnalysis analysis = result.getConvertedMessage().get().getAnalyses().get(0);
        assertThat(analysis.getRegions()).hasSize(1);
        final PolygonGeometry geom = (PolygonGeometry) analysis.getRegions().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        assertThat(geom.getExteriorRingPositions()).isEqualTo(expected);
    }

    @Test
    public void testInvalidIssueTimeDay() throws IOException {
        final String input = getInput("spacewx-invalid-issue-time-day.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX //
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Lexing problem with '20200100/1310Z'"));
    }

    @Test
    public void testInvalidIssueTimeHour() throws IOException {
        final String input = getInput("spacewx-invalid-issue-time-hour.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX //
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Lexing problem with '20200101/2400Z'"));
    }

    @Test
    public void testInvalidNextAdvisoryMonth() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-month.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components"));
    }

    @Test
    public void testInvalidNextAdvisoryDayZero() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-day-zero.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components"));
    }

    @Test
    public void testInvalidNextAdvisoryDay33() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-day-33.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components"));
    }

    @Test
    public void testInvalidNextAdvisoryHour() throws IOException {
        final String input = getInput("spacewx-invalid-nxt-advisory-hour.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.MISSING_DATA//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Missing at least some of the next advisory time components"));
    }

    @Test
    public void testInvalidObservationDay() throws IOException {
        final String input = getInput("spacewx-invalid-obs-day.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Invalid analysis time in OBS SWX:"));
    }

    @Test
    public void testInvalidObservationTime() throws IOException {
        final String input = getInput("spacewx-invalid-obs-hour.tac");
        final ConversionResult<SpaceWeatherAdvisoryAmd82> result = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertThat(result.getConversionIssues()).isNotEmpty();
        assertThat(result.getConversionIssues())
                .anyMatch(issue -> issue.getType() == ConversionIssue.Type.SYNTAX//
                        && issue.getSeverity() == ConversionIssue.Severity.ERROR //
                        && issue.getMessage().equals("Invalid analysis time in OBS SWX:"));
    }

    @Test
    public void testTwoReplaceNumbers() throws Exception {
        final String base = getInput("spacewx-multiple-replace-numbers.tac");
        final String input = withReplaceNumbers(base, "2016/1 2016/2");

        final ConversionResult<SpaceWeatherAdvisoryAmd82> result =
                this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(result.getConversionIssues()).isEmpty();
        assertThat(result.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final SpaceWeatherAdvisoryAmd82 advisory = result.getConvertedMessage().get();

        assertThat(advisory.getReplaceAdvisoryNumbers()).hasSize(2);
        assertThat(advisory.getReplaceAdvisoryNumbers().get(0).getYear()).isEqualTo(2016);
        assertThat(advisory.getReplaceAdvisoryNumbers().get(0).getSerialNumber()).isEqualTo(1);
        assertThat(advisory.getReplaceAdvisoryNumbers().get(1).getYear()).isEqualTo(2016);
        assertThat(advisory.getReplaceAdvisoryNumbers().get(1).getSerialNumber()).isEqualTo(2);
    }

    @Test
    public void testFiveReplaceNumbers() throws Exception {
        final String base = getInput("spacewx-multiple-replace-numbers.tac");
        final String input = withReplaceNumbers(base, "2016/1 2016/2 2016/3 2016/4 2016/5");

        final ConversionResult<SpaceWeatherAdvisoryAmd82> result =
                this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(result.getConversionIssues()).anyMatch(issue ->
                issue.getType() == ConversionIssue.Type.SYNTAX &&
                        issue.getMessage().contains("Too many replacement advisory numbers: 5, maximum is 4"));

        final SpaceWeatherAdvisoryAmd82 advisory = result.getConvertedMessage().get();
        assertThat(advisory.getReplaceAdvisoryNumbers()).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(advisory.getReplaceAdvisoryNumbers().get(i).getYear()).isEqualTo(2016);
            assertThat(advisory.getReplaceAdvisoryNumbers().get(i).getSerialNumber()).isEqualTo(i + 1);
        }
    }

    @Test
    public void testMissingReplaceNumber() throws Exception {
        final String base = getInput("spacewx-multiple-replace-numbers.tac");
        final String input = withReplaceNumbers(base, "");

        final ConversionResult<SpaceWeatherAdvisoryAmd82> res =
                this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);

        assertThat(res.getConversionIssues()).anyMatch(issue ->
                issue.getType() == ConversionIssue.Type.MISSING_DATA &&
                        issue.getMessage().contains("Replace advisory number is missing"));
    }
}
