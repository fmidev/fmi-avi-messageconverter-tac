package fi.fmi.avi.converter.tac.swx;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
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
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXTACConversionTest {
    @Autowired
    private AviMessageConverter converter;

    @Test
    public void parseAndSerialize() throws Exception {
        final String input = getInput("spacewx-A2-4.tac");

        final ConversionResult<SpaceWeatherAdvisory> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory msg = parseResult.getConvertedMessage().get();

        Assert.assertEquals("DONLON", msg.getIssuingCenter().getName().get());
        Assert.assertEquals(2, msg.getAdvisoryNumber().getSerialNumber());
        Assert.assertEquals(2016, msg.getAdvisoryNumber().getYear());
        Assert.assertEquals(1, msg.getReplaceAdvisoryNumber().get().getSerialNumber());
        Assert.assertEquals(2016, msg.getReplaceAdvisoryNumber().get().getYear());
        Assert.assertEquals("RADIATION MOD", msg.getPhenomena().get(0).asCombinedCode());

        Assert.assertEquals(5, msg.getAnalyses().size());
        Assert.assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, msg.getAnalyses().get(0).getAnalysisType());

        final ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_POJO_TO_TAC, new ConversionHints());
        Assert.assertTrue(SerializeResult.getConvertedMessage().isPresent());

        //Assert.assertEquals(input.replace("\n", "\r\n").trim().getBytes(), SerializeResult.getConvertedMessage().get().trim().getBytes());
    }

    @Test
    public void compareParsedObjects() throws Exception {
        final String input = getInput("spacewx-pecasus-mnhmsh.tac");
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_ADVISORY_LABEL_WIDTH, 19);

        final ConversionResult<SpaceWeatherAdvisory> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        final SpaceWeatherAdvisory msg = parseResult.getConvertedMessage().get();

        Assert.assertEquals("PECASUS", msg.getIssuingCenter().getName().get());
        Assert.assertEquals(9, msg.getAdvisoryNumber().getSerialNumber());
        Assert.assertEquals(2020, msg.getAdvisoryNumber().getYear());
        Assert.assertEquals(8, msg.getReplaceAdvisoryNumber().get().getSerialNumber());
        Assert.assertEquals(2020, msg.getReplaceAdvisoryNumber().get().getYear());
        Assert.assertEquals("HF COM MOD", msg.getPhenomena().get(0).asCombinedCode());
        Assert.assertEquals(5, msg.getAnalyses().size());
        SpaceWeatherAdvisoryAnalysis analysis = msg.getAnalyses().get(0);
        Assert.assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, analysis.getAnalysisType());
        Assert.assertEquals("MNH", analysis.getRegions().get(0).getLocationIndicator().get().getCode());
        Assert.assertEquals("MSH", analysis.getRegions().get(1).getLocationIndicator().get().getCode());
        Assert.assertEquals("EQN", analysis.getRegions().get(2).getLocationIndicator().get().getCode());

        analysis = msg.getAnalyses().get(1);
        Assert.assertTrue(analysis.getNilPhenomenonReason().isPresent());
        Assert.assertEquals(SpaceWeatherAdvisoryAnalysis.NilPhenomenonReason.NO_INFORMATION_AVAILABLE, analysis.getNilPhenomenonReason().get());

        final ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_POJO_TO_TAC, hints);
        Assert.assertTrue(SerializeResult.getConvertedMessage().isPresent());

        this.converter.convertMessage(SerializeResult.getConvertedMessage().get(), TACConverter.TAC_TO_SWX_POJO);

        final SpaceWeatherAdvisory adv1 = parseResult.getConvertedMessage().get();
        final SpaceWeatherAdvisory adv2 = parseResult.getConvertedMessage().get();

        Assert.assertEquals(adv1.getIssuingCenter().getName(), adv2.getIssuingCenter().getName());
        Assert.assertEquals(adv1.getRemarks().get(), adv2.getRemarks().get());
        Assert.assertEquals(adv1.getReplaceAdvisoryNumber().get().getSerialNumber(), adv2.getReplaceAdvisoryNumber().get().getSerialNumber());
        Assert.assertEquals(adv1.getReplaceAdvisoryNumber().get().getYear(), adv2.getReplaceAdvisoryNumber().get().getYear());

        Assert.assertEquals(adv1.getNextAdvisory().getTimeSpecifier(), adv2.getNextAdvisory().getTimeSpecifier());
        Assert.assertEquals(adv1.getNextAdvisory().getTime().get(), adv2.getNextAdvisory().getTime().get());

        Assert.assertEquals(adv1.getPhenomena(), adv2.getPhenomena());
        Assert.assertEquals(adv1.getTranslatedTAC().get(), adv2.getTranslatedTAC().get());

        for (int i = 0; i < adv1.getAnalyses().size(); i++) {
            final SpaceWeatherAdvisoryAnalysis analysis1 = adv1.getAnalyses().get(i);
            final SpaceWeatherAdvisoryAnalysis analysis2 = adv2.getAnalyses().get(i);

            Assert.assertEquals(analysis1.getAnalysisType(), analysis2.getAnalysisType());
            Assert.assertEquals(analysis1.getTime(), analysis2.getTime());
            Assert.assertEquals(analysis1.getNilPhenomenonReason(), analysis2.getNilPhenomenonReason());
            Assert.assertEquals(analysis1.getRegions().size(), analysis2.getRegions().size());
            if (analysis1.getRegions() != null) {
                for (int a = 0; a < analysis1.getRegions().size(); a++) {
                    final SpaceWeatherRegion region1 = analysis1.getRegions().get(a);
                    final SpaceWeatherRegion region2 = analysis2.getRegions().get(a);

                    Assert.assertEquals(region1.getLocationIndicator().get(), region2.getLocationIndicator().get());

                    final PolygonGeometry geo1 = (PolygonGeometry) region1.getAirSpaceVolume().get().getHorizontalProjection().get();
                    final PolygonGeometry geo2 = (PolygonGeometry) region2.getAirSpaceVolume().get().getHorizontalProjection().get();

                    Assert.assertEquals(geo1.getCrs(), geo2.getCrs());
                    for (int b = 0; b < geo1.getExteriorRingPositions().size(); b++) {
                        Assert.assertEquals(geo1.getExteriorRingPositions().get(b), geo2.getExteriorRingPositions().get(b));
                    }
                }
            }
        }
    }

    private String getInput(final String fileName) throws IOException {
        try (InputStream is = SWXReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

}
