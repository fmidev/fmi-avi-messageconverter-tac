package fi.fmi.avi.converter.tac.swx;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherRegion;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd79TACConversionTest {
    @Autowired
    private AviMessageConverter converter;

    @Test
    public void parseAndSerialize() throws Exception {
        final String input = getInput("spacewx-A2-4.tac");

        final ConversionResult<SpaceWeatherAdvisoryAmd79> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD79_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisoryAmd79 msg = parseResult.getConvertedMessage().get();

        assertEquals("DONLON", msg.getIssuingCenter().getName().get());
        assertEquals(2, msg.getAdvisoryNumber().getSerialNumber());
        assertEquals(2016, msg.getAdvisoryNumber().getYear());
        assertEquals(1, msg.getReplaceAdvisoryNumber().get().getSerialNumber());
        assertEquals(2016, msg.getReplaceAdvisoryNumber().get().getYear());
        assertEquals("RADIATION MOD", msg.getPhenomena().get(0).asCombinedCode());

        assertEquals(5, msg.getAnalyses().size());
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, msg.getAnalyses().get(0).getAnalysisType());

        final ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_AMD79_POJO_TO_TAC, new ConversionHints());
        assertTrue(SerializeResult.getConvertedMessage().isPresent());

        //Assert.assertEquals(input.replace("\n", "\r\n").trim().getBytes(), SerializeResult.getConvertedMessage().get().trim().getBytes());
    }

    @Test
    public void compareParsedObjects() throws Exception {
        final String input = getInput("spacewx-pecasus-mnhmsh.tac");
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_ADVISORY_LABEL_WIDTH, 19);

        final ConversionResult<SpaceWeatherAdvisoryAmd79> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_AMD79_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisoryAmd79 msg = parseResult.getConvertedMessage().get();

        assertEquals("PECASUS", msg.getIssuingCenter().getName().get());
        assertEquals(9, msg.getAdvisoryNumber().getSerialNumber());
        assertEquals(2020, msg.getAdvisoryNumber().getYear());
        assertEquals(8, msg.getReplaceAdvisoryNumber().get().getSerialNumber());
        assertEquals(2020, msg.getReplaceAdvisoryNumber().get().getYear());
        assertEquals("HF COM MOD", msg.getPhenomena().get(0).asCombinedCode());
        assertEquals(5, msg.getAnalyses().size());
        SpaceWeatherAdvisoryAnalysis analysis = msg.getAnalyses().get(0);
        assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, analysis.getAnalysisType());
        assertEquals("MNH", analysis.getRegions().get(0).getLocationIndicator().get().getCode());
        assertEquals("MSH", analysis.getRegions().get(1).getLocationIndicator().get().getCode());
        assertEquals("EQN", analysis.getRegions().get(2).getLocationIndicator().get().getCode());

        analysis = msg.getAnalyses().get(1);
        assertTrue(analysis.getNilPhenomenonReason().isPresent());
        assertEquals(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE, analysis.getNilPhenomenonReason().get());

        final ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_AMD79_POJO_TO_TAC, hints);
        assertTrue(SerializeResult.getConvertedMessage().isPresent());

        this.converter.convertMessage(SerializeResult.getConvertedMessage().get(), TACConverter.TAC_TO_SWX_AMD79_POJO);

        final SpaceWeatherAdvisoryAmd79 adv1 = parseResult.getConvertedMessage().get();
        final SpaceWeatherAdvisoryAmd79 adv2 = parseResult.getConvertedMessage().get();

        assertEquals(adv1.getIssuingCenter().getName(), adv2.getIssuingCenter().getName());
        assertEquals(adv1.getRemarks().get(), adv2.getRemarks().get());
        assertEquals(adv1.getReplaceAdvisoryNumber().get().getSerialNumber(), adv2.getReplaceAdvisoryNumber().get().getSerialNumber());
        assertEquals(adv1.getReplaceAdvisoryNumber().get().getYear(), adv2.getReplaceAdvisoryNumber().get().getYear());

        assertEquals(adv1.getNextAdvisory().getTimeSpecifier(), adv2.getNextAdvisory().getTimeSpecifier());
        assertEquals(adv1.getNextAdvisory().getTime().get(), adv2.getNextAdvisory().getTime().get());

        assertEquals(adv1.getPhenomena(), adv2.getPhenomena());
        assertEquals(adv1.getTranslatedTAC().get(), adv2.getTranslatedTAC().get());

        for (int i = 0; i < adv1.getAnalyses().size(); i++) {
            final SpaceWeatherAdvisoryAnalysis analysis1 = adv1.getAnalyses().get(i);
            final SpaceWeatherAdvisoryAnalysis analysis2 = adv2.getAnalyses().get(i);

            assertEquals(analysis1.getAnalysisType(), analysis2.getAnalysisType());
            assertEquals(analysis1.getTime(), analysis2.getTime());
            assertEquals(analysis1.getNilPhenomenonReason(), analysis2.getNilPhenomenonReason());
            assertEquals(analysis1.getRegions().size(), analysis2.getRegions().size());
            if (analysis1.getRegions() != null) {
                for (int a = 0; a < analysis1.getRegions().size(); a++) {
                    final SpaceWeatherRegion region1 = analysis1.getRegions().get(a);
                    final SpaceWeatherRegion region2 = analysis2.getRegions().get(a);

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

    private String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd79ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

}
