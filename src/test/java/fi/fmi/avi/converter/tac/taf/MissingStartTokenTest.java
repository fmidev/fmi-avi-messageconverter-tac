package fi.fmi.avi.converter.tac.taf;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class MissingStartTokenTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void withoutConversionHints() {
        ConversionResult<TAF> result = this.converter.convertMessage("EFHK 111733Z 0118/0218 00000KT CAVOK=", TACConverter.TAC_TO_TAF_POJO);
        assertEquals(ConversionResult.Status.FAIL, result.getStatus());
        assertFalse(result.getConvertedMessage().isPresent());
    }

    @Test
    public void withConversionHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TAF);
        ConversionResult<TAF> result = this.converter.convertMessage("EFHK 111733Z 0118/0218 00000KT CAVOK=", TACConverter.TAC_TO_TAF_POJO, hints);
        assertEquals(ConversionResult.Status.WITH_WARNINGS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
    }

}
