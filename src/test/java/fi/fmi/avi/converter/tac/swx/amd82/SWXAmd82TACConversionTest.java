package fi.fmi.avi.converter.tac.swx.amd82;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.amd82.*;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd82TACConversionTest {
    @Autowired
    private AviMessageConverter converter;

    private static String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd82ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    @Test
    public void parseAndSerialize() throws Exception {
        final String input = getInput("spacewx-A2-4.tac");

        final ConversionResult<SpaceWeatherAdvisoryAmd82> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisoryAmd82 msg = parseResult.getConvertedMessage().get();

        assertEquals("DONLON", msg.getIssuingCenter().getName().get());
        assertEquals(2, msg.getAdvisoryNumber().getSerialNumber());
        assertEquals(2016, msg.getAdvisoryNumber().getYear());
        assertEquals(1, msg.getReplaceAdvisoryNumbers().get(0).getSerialNumber());
        assertEquals(2016, msg.getReplaceAdvisoryNumbers().get(0).getYear());
        assertEquals(Effect.RADIATION_AT_FLIGHT_LEVELS, msg.getEffect());

        assertEquals(5, msg.getAnalyses().size());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, msg.getAnalyses().get(0).getAnalysisType());
        assertEquals(Intensity.MODERATE, msg.getAnalyses().get(0).getIntensityAndRegions().get(0).getIntensity());

        final ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_AMD82_POJO_TO_TAC, new ConversionHints());
        assertTrue(SerializeResult.getConvertedMessage().isPresent());

        //Assert.assertEquals(input.replace("\n", "\r\n").trim().getBytes(), SerializeResult.getConvertedMessage().get().trim().getBytes());
    }

    @Test
    public void compareParsedObjects() throws Exception {
        final String input = getInput("spacewx-pecasus-mnhmsh.tac");
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_ADVISORY_LABEL_WIDTH, 19);

        final ConversionResult<SpaceWeatherAdvisoryAmd82> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisoryAmd82 msg = parseResult.getConvertedMessage().get();

        assertEquals("PECASUS", msg.getIssuingCenter().getName().get());
        assertEquals(9, msg.getAdvisoryNumber().getSerialNumber());
        assertEquals(2020, msg.getAdvisoryNumber().getYear());
        assertEquals(8, msg.getReplaceAdvisoryNumbers().get(0).getSerialNumber());
        assertEquals(2020, msg.getReplaceAdvisoryNumbers().get(0).getYear());
        assertEquals(Effect.HF_COMMUNICATIONS, msg.getEffect());
        assertEquals(5, msg.getAnalyses().size());
        SpaceWeatherAdvisoryAnalysis analysis = msg.getAnalyses().get(0);
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, analysis.getAnalysisType());
        final SpaceWeatherIntensityAndRegion intensityAndRegion = analysis.getIntensityAndRegions().get(0);
        assertEquals(Intensity.MODERATE, intensityAndRegion.getIntensity());
        final List<SpaceWeatherRegion> regions = intensityAndRegion.getRegions();
        assertEquals("MNH", regions.get(0).getLocationIndicator().get().getCode());
        assertEquals("MSH", regions.get(1).getLocationIndicator().get().getCode());
        assertEquals("EQN", regions.get(2).getLocationIndicator().get().getCode());

        analysis = msg.getAnalyses().get(1);
        assertTrue(analysis.getNilReason().isPresent());
        assertEquals(SpaceWeatherAdvisoryAnalysis.NilReason.NO_INFORMATION_AVAILABLE, analysis.getNilReason().get());

        final ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_AMD82_POJO_TO_TAC, hints);
        assertTrue(SerializeResult.getConvertedMessage().isPresent());

        this.converter.convertMessage(SerializeResult.getConvertedMessage().get(), TACConverter.TAC_TO_SWX_AMD82_POJO);

        final SpaceWeatherAdvisoryAmd82 adv1 = parseResult.getConvertedMessage().get();
        final SpaceWeatherAdvisoryAmd82 adv2 = parseResult.getConvertedMessage().get();

        assertEquals(adv1.getIssuingCenter().getName(), adv2.getIssuingCenter().getName());
        assertEquals(adv1.getRemarks().get(), adv2.getRemarks().get());
        assertEquals(adv1.getReplaceAdvisoryNumbers().get(0).getSerialNumber(), adv2.getReplaceAdvisoryNumbers().get(0).getSerialNumber());
        assertEquals(adv1.getReplaceAdvisoryNumbers().get(0).getYear(), adv2.getReplaceAdvisoryNumbers().get(0).getYear());

        assertEquals(adv1.getNextAdvisory().getTimeSpecifier(), adv2.getNextAdvisory().getTimeSpecifier());
        assertEquals(adv1.getNextAdvisory().getTime().get(), adv2.getNextAdvisory().getTime().get());

        assertEquals(adv1.getEffect(), adv2.getEffect());
        assertEquals(adv1.getTranslatedTAC().get(), adv2.getTranslatedTAC().get());

        for (int i = 0; i < adv1.getAnalyses().size(); i++) {
            final SpaceWeatherAdvisoryAnalysis analysis1 = adv1.getAnalyses().get(i);
            final SpaceWeatherAdvisoryAnalysis analysis2 = adv2.getAnalyses().get(i);

            assertEquals(analysis1.getAnalysisType(), analysis2.getAnalysisType());
            assertEquals(analysis1.getTime(), analysis2.getTime());
            assertEquals(analysis1.getNilReason(), analysis2.getNilReason());
            assertEquals(analysis1.getIntensityAndRegions().size(), analysis2.getIntensityAndRegions().size());
            if (analysis1.getIntensityAndRegions() != null) {
                for (int a = 0; a < analysis1.getIntensityAndRegions().size(); a++) {
                    final SpaceWeatherRegion region1 = analysis1.getIntensityAndRegions().get(0).getRegions().get(a);
                    final SpaceWeatherRegion region2 = analysis2.getIntensityAndRegions().get(0).getRegions().get(a);

                    assertEquals(region1.getLocationIndicator().get(), region2.getLocationIndicator().get());

                    final PolygonGeometry geo1 = (PolygonGeometry) region1.getAirSpaceVolume().get().getHorizontalProjection().get();
                    final PolygonGeometry geo2 = (PolygonGeometry) region2.getAirSpaceVolume().get().getHorizontalProjection().get();

                    assertEquals(geo1.getCrs(), geo2.getCrs());
                    for (int b = 0; b < geo1.getExteriorRingPositions().size(); b++) {
                        assertEquals(geo1.getExteriorRingPositions().get(b), geo2.getExteriorRingPositions().get(b));
                    }
                }
            }
        }
    }

    @Test
    public void parseAndSerializeMultipleReplaceNumbers() throws Exception {
        final String input = getInput("spacewx-multiple-replace-numbers.tac");

        final ConversionResult<SpaceWeatherAdvisoryAmd82> pojoResult =
                this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(0, pojoResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        assertTrue(pojoResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisoryAmd82 advisory = pojoResult.getConvertedMessage().get();

        assertEquals("DONLON", advisory.getIssuingCenter().getName().get());
        assertEquals(8, advisory.getAdvisoryNumber().getSerialNumber());
        assertEquals(2016, advisory.getAdvisoryNumber().getYear());

        assertEquals(4, advisory.getReplaceAdvisoryNumbers().size());
        assertEquals(7, advisory.getReplaceAdvisoryNumbers().get(0).getSerialNumber());
        assertEquals(6, advisory.getReplaceAdvisoryNumbers().get(1).getSerialNumber());
        assertEquals(5, advisory.getReplaceAdvisoryNumbers().get(2).getSerialNumber());
        assertEquals(4, advisory.getReplaceAdvisoryNumbers().get(3).getSerialNumber());
        for (int i = 0; i < advisory.getReplaceAdvisoryNumbers().size(); i++) {
            assertEquals(2016, advisory.getReplaceAdvisoryNumbers().get(i).getYear());
        }

        final ConversionResult<String> tacResult =
                this.converter.convertMessage(advisory, TACConverter.SWX_AMD82_POJO_TO_TAC, new ConversionHints());
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());
        assertTrue(tacResult.getConvertedMessage().isPresent());
        final String tac = tacResult.getConvertedMessage().get();

        final ConversionResult<SpaceWeatherAdvisoryAmd82> reserialized =
                this.converter.convertMessage(tac, TACConverter.TAC_TO_SWX_AMD82_POJO);
        assertEquals(0, reserialized.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, reserialized.getStatus());
        assertTrue(reserialized.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisoryAmd82 roundTripped = reserialized.getConvertedMessage().get();

        assertEquals(advisory.getIssuingCenter().getName(), roundTripped.getIssuingCenter().getName());
        assertEquals(advisory.getAdvisoryNumber().getYear(), roundTripped.getAdvisoryNumber().getYear());
        assertEquals(advisory.getAdvisoryNumber().getSerialNumber(), roundTripped.getAdvisoryNumber().getSerialNumber());

        assertEquals(advisory.getReplaceAdvisoryNumbers().size(), roundTripped.getReplaceAdvisoryNumbers().size());
        for (int i = 0; i < advisory.getReplaceAdvisoryNumbers().size(); i++) {
            assertEquals(advisory.getReplaceAdvisoryNumbers().get(i).getYear(),
                    roundTripped.getReplaceAdvisoryNumbers().get(i).getYear());
            assertEquals(advisory.getReplaceAdvisoryNumbers().get(i).getSerialNumber(),
                    roundTripped.getReplaceAdvisoryNumbers().get(i).getSerialNumber());
        }
    }


}
