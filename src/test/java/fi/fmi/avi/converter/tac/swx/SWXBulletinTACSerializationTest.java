package fi.fmi.avi.converter.tac.swx;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

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
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherBulletin;
import fi.fmi.avi.model.swx.immutable.SpaceWeatherBulletinImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXBulletinTACSerializationTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testBulletinSerialization() throws Exception {
        final String tac = "SWX ADVISORY\n" + "STATUS: TEST\n" + "DTG: 20190128/1200Z\n" //
                + "SWXC: PECASUS\n" //
                + "ADVISORY NR: 2019/1\n"//
                + "SWX EFFECT: SATCOM MOD AND RADIATION SEV\n" //
                + "OBS SWX: 08/1200Z HNH HSH E16000 - W2000 ABV FL340\n"//
                + "FCST SWX +6 HR: 08/1800Z ABV FL370 N80 W180 - N70 W75 - N60 E15 - N70 E75 - N80 W180 \n"//
                + "FCST SWX +12 HR: 09/0000Z NO SWX EXP\n"//
                + "FCST SWX +18 HR: 09/0600Z DAYLIGHT SIDE\n"//
                + "FCST SWX +24 HR: 09/1200Z NO SWX EXP\n"//
                + "RMK: TEST TEST TEST TEST\n" //
                + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" //
                + "SEE WWW.PECASUS.ORG \n" //
                + "NXT ADVISORY: WILL BE ISSUED BY 20161108/0700Z\n \n";
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_SWX_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final Optional<SpaceWeatherAdvisory> pojo = result.getConvertedMessage();
        assertTrue(pojo.isPresent());
        final SpaceWeatherBulletin bulletin = SpaceWeatherBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(1)//
                        .setType(BulletinHeading.Type.NORMAL)//
                        .setGeographicalDesignator("XX")//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_SPACE_WEATHER)
                        .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))//
                        .build())//
                .addMessages(pojo.get())//
                .build();

        final ConversionResult<String> stringResult = this.converter.convertMessage(bulletin, TACConverter.SWX_BULLETIN_POJO_TO_TAC,
                new ConversionHints(ConversionHints.KEY_WHITESPACE_SERIALIZATION_MODE, ConversionHints.VALUE_WHITESPACE_SERIALIZATION_MODE_PASSTHROUGH));
        assertTrue(stringResult.getConversionIssues().isEmpty());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FNXX01 EFKL 020500"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "TAF AMD EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     BKN010 SCT030CB=", //
                stringResult.getConvertedMessage().get());
    }
}
