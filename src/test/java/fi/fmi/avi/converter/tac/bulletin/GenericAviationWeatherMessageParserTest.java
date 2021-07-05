package fi.fmi.avi.converter.tac.bulletin;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;

import java.time.ZonedDateTime;
import java.util.Optional;

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
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericAviationWeatherMessageParserTest {
    @Autowired
    private AviMessageConverter converter;

    @Autowired
    private AviMessageLexer lexer;

    @Test
    public void tafTest() {
        String tacMessage = "TAF EVRA 301103Z 3012/3112 15013KT 8000 OVC008\r\n" //
                + "TEMPO 3012/3015 14015G26KT 5000 -RA OVC010\r\n" //
                + "FM301500 18012KT 9000 NSW SCT015 BKN020\r\n" //
                + "TEMPO 3017/3103 19020G33KT 3000 -SHRA BKN012CB BKN020=";
        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(tacMessage, TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO,
                new ConversionHints());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage msg = result.getConvertedMessage().get();

        assertEquals(tacMessage, msg.getOriginalMessage());
        assertEquals(MessageType.TAF, msg.getMessageType().get());
        assertEquals(GenericAviationWeatherMessage.Format.TAC, msg.getMessageFormat());
        assertEquals("--30T11:03Z",
                msg.getIssueTime().map(PartialOrCompleteTimeInstant::getPartialTime).map(Optional::get).map(PartialDateTime::toString).orElse(null));
        assertEquals("--30T12:Z", msg.getValidityTime()
                .map(PartialOrCompleteTimePeriod::getStartTime)
                .map(Optional::get)
                .map(PartialOrCompleteTimeInstant::getPartialTime)
                .map(Optional::get)
                .map(PartialDateTime::toString)
                .orElse(null));
        assertEquals("--31T12:Z", msg.getValidityTime()
                .map(PartialOrCompleteTimePeriod::getEndTime)
                .map(Optional::get)
                .map(PartialOrCompleteTimeInstant::getPartialTime)
                .map(Optional::get)
                .map(PartialDateTime::toString)
                .orElse(null));
        assertEquals("EVRA", msg.getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME));
    }

    @Test
    public void swxTest() {
        String tacMessage = "SWX ADVISORY\n" + "STATUS:             EXER\n" + "DTG:                20161108/0100Z\n" + "SWXC:               DONLON\n"
                + "ADVISORY NR:        2016/2\n" + "NR RPLC:            2016/1\n" + "SWX EFFECT:         HF COM MOD AND GNSS MOD\n"
                + "OBS SWX:            08/0100Z HNH HSH E18000 - W18000\n" + "FCST SWX +6 HR:     08/0700Z HNH HSH E18000 - W18000\n"
                + "FCST SWX +12 HR:    08/1300Z HNH HSH E18000 - W18000\n" + "FCST SWX +18 HR:    08/1900Z HNH HSH E18000 - W18000\n"
                + "FCST SWX +24 HR:    09/0100Z NO SWX EXP\n"
                + "RMK:                LOW LVL GEOMAGNETIC STORMING CAUSING INCREASED AURORAL ACT AND SUBSEQUENT MOD DEGRADATION OF GNSS AND HF COM AVBL IN THE AURORAL ZONE. THIS STORMING EXP TO SUBSIDE IN THE FCST PERIOD. SEE WWW.SPACEWEATHERPROVIDER.WEB \n"
                + "NXT ADVISORY:       NO FURTHER ADVISORIES=";

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(tacMessage, TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO,
                new ConversionHints());

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage msg = result.getConvertedMessage().get();

        assertEquals(tacMessage, msg.getOriginalMessage());
        assertEquals(MessageType.SPACE_WEATHER_ADVISORY, msg.getMessageType().get());
        assertEquals(GenericAviationWeatherMessage.Format.TAC, msg.getMessageFormat());
        assertEquals("2016-11-08T01:00Z",
                msg.getIssueTime().map(PartialOrCompleteTimeInstant::getCompleteTime).map(Optional::get).map(ZonedDateTime::toString).orElse(null));
        assertEquals("--08T01:00Z", msg.getValidityTime()
                .map(PartialOrCompleteTimePeriod::getStartTime)
                .map(Optional::get)
                .map(PartialOrCompleteTimeInstant::getPartialTime)
                .map(Optional::get)
                .map(PartialDateTime::toString)
                .orElse(null));
        assertEquals("--09T01:00Z", msg.getValidityTime()
                .map(PartialOrCompleteTimePeriod::getEndTime)
                .map(Optional::get)
                .map(PartialOrCompleteTimeInstant::getPartialTime)
                .map(Optional::get)
                .map(PartialDateTime::toString)
                .orElse(null));
        assertEquals("DONLON", msg.getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE ));
    }

    @Test
    public void tafNoEndTokenTest() {
        String tacMessage = "TAF EVRA 301103Z 3012/3112 15013KT 8000 OVC008\r\n" //
                + "TEMPO 3012/3015 14015G26KT 5000 -RA OVC010\r\n" //
                + "FM301500 18012KT 9000 NSW SCT015 BKN020\r\n" //
                + "TEMPO 3017/3103 19020G33KT 3000 -SHRA BKN012CB BKN020";

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(tacMessage, TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO,
                new ConversionHints());

        assertEquals(ConversionResult.Status.FAIL, result.getStatus());
        assertEquals(1, result.getConversionIssues().size());
    }

    @Test
    public void missingMessageTypeTest() {
        String tacMessage = "TF EVRA 301103Z 3012/3112 15013KT 8000 OVC008\r\n" //
                + "TEMPO 3012/3015 14015G26KT 5000 -RA OVC010\r\n" //
                + "FM301500 18012KT 9000 NSW SCT015 BKN020\r\n" //
                + "TEMPO 3017/3103 19020G33KT 3000 -SHRA BKN012CB BKN020=";
        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(tacMessage, TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO);

        assertEquals(ConversionResult.Status.WITH_WARNINGS, result.getStatus());

        GenericAviationWeatherMessage msg = result.getConvertedMessage().get();

        assertEquals(tacMessage, msg.getOriginalMessage());
        assertFalse(msg.getMessageType().isPresent());
        assertEquals(GenericAviationWeatherMessage.Format.TAC, msg.getMessageFormat());
    }

    @Test
    public void missingMessageTypeWithConversionHintTest() {
        String tacMessage = "TF EVRA 301103Z 3012/3112 15013KT 8000 OVC008\r\n" //
                + "TEMPO 3012/3015 14015G26KT 5000 -RA OVC010\r\n" //
                + "FM301500 18012KT 9000 NSW SCT015 BKN020\r\n" //
                + "TEMPO 3017/3103 19020G33KT 3000 -SHRA BKN012CB BKN020=";

        ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_MESSAGE_TYPE, MessageType.TAF);

        ConversionResult<GenericAviationWeatherMessage> result = converter.convertMessage(tacMessage, TACConverter.TAC_TO_GENERIC_AVIATION_WEATHER_MESSAGE_POJO,
                hints);

        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());

        GenericAviationWeatherMessage msg = result.getConvertedMessage().get();

        assertEquals(tacMessage, msg.getOriginalMessage());
        assertEquals(MessageType.TAF, msg.getMessageType().get());
        assertEquals(GenericAviationWeatherMessage.Format.TAC, msg.getMessageFormat());
        assertEquals("--30T11:03Z",
                msg.getIssueTime().map(PartialOrCompleteTimeInstant::getPartialTime).map(Optional::get).map(PartialDateTime::toString).orElse(null));
        assertEquals("--30T12:Z", msg.getValidityTime()
              .map(PartialOrCompleteTimePeriod::getStartTime)
              .map(Optional::get)
              .map(PartialOrCompleteTimeInstant::getPartialTime)
              .map(Optional::get)
              .map(PartialDateTime::toString)
              .orElse(null));
        assertEquals("--31T12:Z", msg.getValidityTime()
              .map(PartialOrCompleteTimePeriod::getEndTime)
              .map(Optional::get)
              .map(PartialOrCompleteTimeInstant::getPartialTime)
              .map(Optional::get)
              .map(PartialDateTime::toString)
              .orElse(null));
        assertEquals("EVRA", msg.getLocationIndicators().get(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME));
    }

}
