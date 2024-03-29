package fi.fmi.avi.converter.tac.sigmet;

import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Ignore;
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
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.sigmet.immutable.SIGMETBulletinImpl;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
@Ignore("Not ready yet")
public class SIGMETBulletinTACSerializationTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testSerialization() {
        final SIGMETBulletinImpl.Builder builder = SIGMETBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(31)//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.WarningsDataTypeDesignatorT2.WRN_SIGMET)//
                        .setIssueTime(PartialOrCompleteTimeInstant.builder()//
                                .setPartialTime(PartialDateTime.ofDayHourMinute(17, 7, 0)))//
                        .build());

        builder.addMessages(SIGMETImpl.builder()//
                .setTranslatedTAC("EFIN SIGMET 1 VALID 170750/170950 EFKL-\n"//
                        + "EFIN FINLAND FIR SEV TURB FCST AT 0740Z "//
                        + "S OF LINE N5953 E01931 -\n"//
                        + "N6001 E02312 - N6008 E02606 - N6008\n"//
                        + "E02628 FL220-340 MOV N 15KT\n"//
                        + "WKN=")//
                .setTranslated(false)//
                .buildPartial());
        final SIGMETBulletin msg = builder.build();

        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.SIGMET_BULLETIN_POJO_TO_TAC, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        //NOTE: the line wrapping does not not work as expected here due to the fact that 'N6008\nE02628' is parsed as a single token
        TestCase.assertEquals(//
                        "WSFI31 EFKL 170700"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                        + "EFIN SIGMET 1 VALID 170750/170950 EFKL- EFIN FINLAND FIR" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     SEV TURB FCST AT 0740Z S OF LINE N5953 E01931 -" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     N6001 E02312 - N6008 E02606 - N6008\n"//
                        + "E02628 FL220-340" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     MOV N 15KT WKN=", //
                tacBulletin.get());
    }
}
