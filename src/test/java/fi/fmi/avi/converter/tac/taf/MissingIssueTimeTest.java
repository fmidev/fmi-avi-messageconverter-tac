package fi.fmi.avi.converter.tac.taf;

import static junit.framework.TestCase.assertEquals;
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

public class MissingIssueTimeTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testMissingIssueTime() {
        final ConversionResult<TAF> result = this.converter.convertMessage("TAF EFHK=", TACConverter.TAC_TO_TAF_POJO);
        assertEquals(ConversionResult.Status.WITH_ERRORS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());

    }

}
