package fi.fmi.avi.converter.tac.metar;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.metar.METAR;

import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class PositiveNegativeZeroTempTest {
    
    @Autowired
    private AviMessageConverter converter;
    
    @Test
    public void testZeroValueRemainsNegative() {
        ConversionResult<METAR> result = this.converter.convertMessage("METAR EFHK 111111Z 15008KT CAVOK M00/M00 Q1023=",
                TACConverter.TAC_TO_METAR_POJO);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());
        assertTrue(1.0d/m.get().getDewpointTemperature().get().getValue() == Double.NEGATIVE_INFINITY);
        assertTrue(1.0d/m.get().getAirTemperature().get().getValue() == Double.NEGATIVE_INFINITY);
        ConversionResult<String> result2 = this.converter.convertMessage(m.get(), TACConverter.METAR_POJO_TO_TAC);
        assertTrue(ConversionResult.Status.SUCCESS == result2.getStatus());
        assertTrue(result2.getConvertedMessage().isPresent());
        assertTrue(result2.getConvertedMessage().get().contains("M00/M00"));

    }
    
    @Test
    public void testZeroValueRemainsPositive() {
        ConversionResult<METAR> result = this.converter.convertMessage("METAR EFHK 111111Z 15008KT CAVOK 00/00 Q1023=",
                TACConverter.TAC_TO_METAR_POJO);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        Optional<METAR> m = result.getConvertedMessage();
        assertTrue(m.isPresent());
        assertTrue(1.0d/m.get().getDewpointTemperature().get().getValue() == Double.POSITIVE_INFINITY);
        assertTrue(1.0d/m.get().getAirTemperature().get().getValue() == Double.POSITIVE_INFINITY);
        ConversionResult<String> result2 = this.converter.convertMessage(m.get(), TACConverter.METAR_POJO_TO_TAC);
        assertTrue(ConversionResult.Status.SUCCESS == result2.getStatus());
        assertTrue(result2.getConvertedMessage().isPresent());
        assertTrue(result2.getConvertedMessage().get().contains("00/00"));

    }
  

}
