package fi.fmi.avi.converter.tac.metar;

import static org.junit.Assert.*;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class PositiveNegativeZeroTempTest {
    
    @Autowired
    private AviMessageConverter converter;
    
    @Test
    public void testZeroValueRemainsNegative() {
        assertTrue(1/Double.valueOf(-0.0d) < 0);
        ConversionResult<METAR> result = this.converter.convertMessage("METAR EFHK 111111Z 15008KT 0700 CAVOK M00/M00 Q1023=", TACConverter.TAC_TO_METAR_POJO);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        METAR m = result.getConvertedMessage();
        assertTrue(1/m.getDewpointTemperature().getValue() < 0);
        assertTrue(1/m.getAirTemperature().getValue() < 0);
        ConversionResult<String> result2 = this.converter.convertMessage(m, TACConverter.METAR_POJO_TO_TAC);
        assertTrue(ConversionResult.Status.SUCCESS == result2.getStatus());
        assertTrue(result2.getConvertedMessage().indexOf("M00/M00") > -1);

    }
    
    @Test
    public void testZeroValueRemainsPositive() {
        assertTrue(1/Double.valueOf(0.0d) > 0);
        ConversionResult<METAR> result = this.converter.convertMessage("METAR EFHK 111111Z 15008KT 0700 CAVOK 00/00 Q1023=", TACConverter.TAC_TO_METAR_POJO);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        METAR m = result.getConvertedMessage();
        assertTrue(1/m.getDewpointTemperature().getValue() > 0);
        assertTrue(1/m.getAirTemperature().getValue() > 0);
        ConversionResult<String> result2 = this.converter.convertMessage(m, TACConverter.METAR_POJO_TO_TAC);
        assertTrue(ConversionResult.Status.SUCCESS == result2.getStatus());
        assertTrue(result2.getConvertedMessage().indexOf("00/00") > -1);

    }
  

}
