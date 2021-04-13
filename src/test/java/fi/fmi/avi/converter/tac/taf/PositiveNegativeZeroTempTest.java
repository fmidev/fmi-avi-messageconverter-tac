package fi.fmi.avi.converter.tac.taf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Optional;

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
        final String tac = "TAF EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TXM00/3015Z TNM00/3103Z\n" //
                + "TEMPO 3012/3018 3000 RADZ BR OVC004\n" //
                + "BECMG 3018/3020 BKN008 SCT015CB\n" //
                + "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" //
                + "BECMG 3104/3106 21016G30KT=";
        final ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<TAF> t = result.getConvertedMessage();
        assertTrue(t.isPresent());
        assertEquals(Double.NEGATIVE_INFINITY, 1.0d / t.get().getBaseForecast().get().getTemperatures().get().get(0).getMinTemperature().getValue(), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, 1.0d / t.get().getBaseForecast().get().getTemperatures().get().get(0).getMaxTemperature().getValue(), 0.0);
        final ConversionResult<String> result2 = this.converter.convertMessage(t.get(), TACConverter.TAF_POJO_TO_TAC);
        assertSame(ConversionResult.Status.SUCCESS, result2.getStatus());
        assertTrue(result2.getConvertedMessage().isPresent());
        assertTrue(result2.getConvertedMessage().get().contains("TXM00/3015Z"));
        assertTrue(result2.getConvertedMessage().get().contains("TNM00/3103Z"));

    }

    @Test
    public void testZeroValueRemainsPositive() {
        final String tac = "TAF EETN 301130Z 3012/3112 14016G26KT 8000 BKN010 OVC015 TX00/3015Z TN00/3103Z\n" //
                + "TEMPO 3012/3018 3000 RADZ BR OVC004\n" //
                + "BECMG 3018/3020 BKN008 SCT015CB\n" //
                + "TEMPO 3102/3112 3000 SHRASN BKN006 BKN015CB\n" //
                + "BECMG 3104/3106 21016G30KT=";
        final ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertSame(ConversionResult.Status.SUCCESS, result.getStatus());
        final Optional<TAF> t = result.getConvertedMessage();
        assertTrue(t.isPresent());
        assertEquals(Double.POSITIVE_INFINITY, 1.0d / t.get().getBaseForecast().get().getTemperatures().get().get(0).getMinTemperature().getValue(), 0.0);
        assertEquals(Double.POSITIVE_INFINITY, 1.0d / t.get().getBaseForecast().get().getTemperatures().get().get(0).getMaxTemperature().getValue(), 0.0);
        final ConversionResult<String> result2 = this.converter.convertMessage(t.get(), TACConverter.TAF_POJO_TO_TAC);
        assertSame(ConversionResult.Status.SUCCESS, result2.getStatus());
        assertTrue(result2.getConvertedMessage().isPresent());
        assertTrue(result2.getConvertedMessage().get().contains("TX00/3015Z"));
        assertTrue(result2.getConvertedMessage().get().contains("TN00/3103Z"));

    }

}
