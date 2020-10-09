package fi.fmi.avi.converter.tac.swx;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherAdvisoryImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXTACSerializerTest {

    @Autowired
    @Qualifier("tacTokenizer")
    private AviMessageTACTokenizer tokenizer;

    @Autowired
    private AviMessageConverter converter;

    private SpaceWeatherAdvisory msg;

    @Before
    public void setup() throws Exception {
        ObjectMapper om = new ObjectMapper();
        om.registerModule(new Jdk8Module()).registerModule(new JavaTimeModule());

        String input = getInput("spacewx-A2-3.json");
        msg = om.readValue(input, SpaceWeatherAdvisoryImpl.class);
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

    @Test
    public void swxSerializerTest() {
        ConversionResult<String> result = this.converter.convertMessage(msg, TACConverter.SWX_POJO_TO_TAC, new ConversionHints());
        Assert.assertTrue(result.getConvertedMessage().isPresent());
        System.out.println(result.getConvertedMessage().get());
    }

    @Test
    public void testNilRemark() throws Exception {
        String expected = "SWX ADVISORY" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "STATUS:             TEST" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "DTG:                20161108/0000Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWXC:               DONLON" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "ADVISORY NR:        2016/2" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NR RPLC:            2016/1" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "SWX EFFECT:         RADIATION MOD" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "OBS SWX:            08/0100Z HNH HSH E18000 - W18000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +6 HR:     08/0700Z HNH HSH E18000 - W18000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E18000 - W18000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +18 HR:    08/1900Z HNH HSH E18000 - W18000 ABV FL340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "RMK:                NIL" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        final ConversionResult<SpaceWeatherAdvisory> pojoResult = this.converter.convertMessage(expected, TACConverter.TAC_TO_SWX_POJO);
        Assert.assertEquals(ConversionResult.Status.SUCCESS, pojoResult.getStatus());
        Assert.assertTrue(pojoResult.getConvertedMessage().isPresent());
        Assert.assertFalse(pojoResult.getConvertedMessage().get().getRemarks().isPresent());

        ConversionResult<String> stringResult = this.converter.convertMessage(pojoResult.getConvertedMessage().get(), TACConverter.SWX_POJO_TO_TAC,
                new ConversionHints());
        Assert.assertEquals(ConversionResult.Status.SUCCESS, stringResult.getStatus());
        Assert.assertTrue(stringResult.getConvertedMessage().isPresent());
        Assert.assertEquals(expected, stringResult.getConvertedMessage().get());
    }
}
