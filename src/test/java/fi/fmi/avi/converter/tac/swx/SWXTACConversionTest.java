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
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXTACConversionTest {
    @Autowired
    private AviMessageConverter converter;

    @Test
    public void parseAndSerialize() throws Exception {
        String input = getInput("spacewx-A2-4.tac");

        final ConversionResult<SpaceWeatherAdvisory> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        SpaceWeatherAdvisory msg = parseResult.getConvertedMessage().get();

        Assert.assertEquals("DONLON", msg.getIssuingCenter().getName().get());
        Assert.assertEquals(2, msg.getAdvisoryNumber().getSerialNumber());
        Assert.assertEquals(2016, msg.getAdvisoryNumber().getYear());
        Assert.assertEquals(1, msg.getReplaceAdvisoryNumber().get().getSerialNumber());
        Assert.assertEquals(2016, msg.getReplaceAdvisoryNumber().get().getYear());
        Assert.assertEquals("RADIATION MOD", msg.getPhenomena().get(0).asCombinedCode());

        Assert.assertEquals(5, msg.getAnalyses().size());
        Assert.assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, msg.getAnalyses().get(0).getAnalysisType().get());

        ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_POJO_TO_TAC, new ConversionHints());
        Assert.assertTrue(SerializeResult.getConvertedMessage().isPresent());

        Assert.assertEquals(input, SerializeResult.getConvertedMessage().get());
    }

    @Test
    public void compareParsedObjects() throws Exception {
        String input = getInput("spacewx-pecasus-mnhmsh.tac");
        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_SWX_LABEL_END_LENGTH, 19);

        final ConversionResult<SpaceWeatherAdvisory> parseResult = this.converter.convertMessage(input, TACConverter.TAC_TO_SWX_POJO);
        assertEquals(0, parseResult.getConversionIssues().size());
        assertEquals(ConversionResult.Status.SUCCESS, parseResult.getStatus());
        assertTrue(parseResult.getConvertedMessage().isPresent());

        SpaceWeatherAdvisory msg = parseResult.getConvertedMessage().get();

        Assert.assertEquals("PECASUS", msg.getIssuingCenter().getName().get());
        Assert.assertEquals(9, msg.getAdvisoryNumber().getSerialNumber());
        Assert.assertEquals(2020, msg.getAdvisoryNumber().getYear());
        Assert.assertEquals(8, msg.getReplaceAdvisoryNumber().get().getSerialNumber());
        Assert.assertEquals(2020, msg.getReplaceAdvisoryNumber().get().getYear());
        Assert.assertEquals("HF COM MOD", msg.getPhenomena().get(0).asCombinedCode());
        Assert.assertEquals(5, msg.getAnalyses().size());
        SpaceWeatherAdvisoryAnalysis analysis = msg.getAnalyses().get(0);
        Assert.assertEquals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION, analysis.getAnalysisType().get());
        Assert.assertEquals("MNH", analysis.getRegion().get().get(0).getLocationIndicator().get().getCode());
        Assert.assertEquals("MSH", analysis.getRegion().get().get(1).getLocationIndicator().get().getCode());
        Assert.assertEquals("EQN", analysis.getRegion().get().get(2).getLocationIndicator().get().getCode());

        analysis = msg.getAnalyses().get(1);
        Assert.assertTrue(analysis.isNoInformationAvailable());

        ConversionResult<String> SerializeResult = this.converter.convertMessage(msg, TACConverter.SWX_POJO_TO_TAC, hints);
        Assert.assertTrue(SerializeResult.getConvertedMessage().isPresent());

        final ConversionResult<SpaceWeatherAdvisory> reparseResult = this.converter.convertMessage(SerializeResult.getConvertedMessage().get(),
                TACConverter.TAC_TO_SWX_POJO);

        Assert.assertEquals(parseResult.getConvertedMessage().get(), reparseResult.getConvertedMessage().get());
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

}

