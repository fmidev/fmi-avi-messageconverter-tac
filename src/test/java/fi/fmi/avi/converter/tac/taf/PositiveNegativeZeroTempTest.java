package fi.fmi.avi.converter.tac.taf;

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
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class PositiveNegativeZeroTempTest {
    
    @Autowired
    private AviMessageConverter converter;
    
    @Test
    public void testZeroValueRemainsNegative() {
        String tac = "TAF EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM00/3015Z TNM00/3103Z\n" +
                "TEMPO 3012/3018 3000 RADZ BR OVC004\n" +
                "BECMG 3018/3020 BKN008 SCT015CB\n" +
                "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" +
                "BECMG 3104/3106 21016G30KT=";
        ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        TAF t = result.getConvertedMessage();
        assertTrue(1.0d/t.getBaseForecast().getTemperatures().get(0).getMinTemperature().getValue().doubleValue() == Double.NEGATIVE_INFINITY);
        assertTrue(1.0d/t.getBaseForecast().getTemperatures().get(0).getMaxTemperature().getValue().doubleValue() == Double.NEGATIVE_INFINITY);
        ConversionResult<String> result2 = this.converter.convertMessage(t, TACConverter.TAF_POJO_TO_TAC);
        assertTrue(ConversionResult.Status.SUCCESS == result2.getStatus());
        assertTrue(result2.getConvertedMessage().indexOf("TXM00/3015Z") > -1);
        assertTrue(result2.getConvertedMessage().indexOf("TNM00/3103Z") > -1);

    }
    
    @Test
    public void testZeroValueRemainsPositive() {
        String tac = "TAF EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TX00/3015Z TN00/3103Z\n" +
                "TEMPO 3012/3018 3000 RADZ BR OVC004\n" +
                "BECMG 3018/3020 BKN008 SCT015CB\n" +
                "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" +
                "BECMG 3104/3106 21016G30KT=";
        ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(ConversionResult.Status.SUCCESS == result.getStatus());
        TAF t = result.getConvertedMessage();
        assertTrue(1.0d/t.getBaseForecast().getTemperatures().get(0).getMinTemperature().getValue().doubleValue() == Double.POSITIVE_INFINITY);
        assertTrue(1.0d/t.getBaseForecast().getTemperatures().get(0).getMaxTemperature().getValue().doubleValue() == Double.POSITIVE_INFINITY);
        ConversionResult<String> result2 = this.converter.convertMessage(t, TACConverter.TAF_POJO_TO_TAC);
        assertTrue(ConversionResult.Status.SUCCESS == result2.getStatus());
        assertTrue(result2.getConvertedMessage().indexOf("TX00/3015Z") > -1);
        assertTrue(result2.getConvertedMessage().indexOf("TN00/3103Z") > -1);

    }
  

}
