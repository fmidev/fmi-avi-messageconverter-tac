package fi.fmi.avi.converter.tac.bulletin;

import static junit.framework.TestCase.assertEquals;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.immutable.GenericMeteorologicalBulletinImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericMeteorologicalBulletinSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testSerializer() throws Exception {
        GenericMeteorologicalBulletin pojo = readFromJSON("bulletin/generic-bulletin1.json");
        ConversionResult<String> result = this.converter.convertMessage(pojo, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS,result.getStatus());
    }

    protected GenericMeteorologicalBulletin readFromJSON(final String fileName) throws IOException {
        final GenericMeteorologicalBulletin retval;
        final ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module());
        om.registerModule(new JavaTimeModule());
        final InputStream is = AbstractAviMessageTest.class.getResourceAsStream(fileName);
        if (is != null) {
            retval = om.readValue(is, GenericMeteorologicalBulletinImpl.class);
        } else {
            throw new FileNotFoundException("Resource '" + fileName + "' could not be loaded");
        }
        return retval;
    }
}
