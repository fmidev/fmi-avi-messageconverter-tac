package fi.fmi.avi.converter.tac.sigmet;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

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
import fi.fmi.avi.converter.tac.AbstractTACBulletinSerializer;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.sigmet.SIGMETBulletin;
import fi.fmi.avi.model.sigmet.SIGMETBulletinHeading;
import fi.fmi.avi.model.sigmet.immutable.SIGMETBulletinHeadingImpl;
import fi.fmi.avi.model.sigmet.immutable.SIGMETBulletinImpl;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SIGMETBulletinTACSerializationTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testSerialization() throws Exception {
        SIGMETBulletinImpl.Builder builder = new SIGMETBulletinImpl.Builder()//
                .setIssueTime(new PartialOrCompleteTimeInstant.Builder()//
                        .setPartialTime(PartialDateTime.ofDayHourMinute(17, 7, 0))).setHeading(new SIGMETBulletinHeadingImpl.Builder()//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(31)//
                        .setSIGMETType(SIGMETBulletinHeading.SIGMETType.SEVERE_WEATHER).build());

        builder.addMessages(new SIGMETImpl.Builder()//
                .setTranslatedTAC("EFIN SIGMET 1 VALID 170750/170950 EFKL-\n"//
                        + "EFIN FINLAND FIR SEV TURB FCST AT 0740Z\n"//
                        + "S OF LINE N5953 E01931 -\n"//
                        + "N6001 E02312 - N6008 E02606 - N6008\n"//
                        + "E02628 FL220-340 MOV N 15KT\n"//
                        + "WKN=").setTranslated(false).build());
        SIGMETBulletin msg = builder.build();

        ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.SIGMET_BULLETIN_POJO_TO_TAC, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals("WSFI31 EFKL 170700" + AbstractTACBulletinSerializer.NEW_LINE//
                + "EFIN SIGMET 1 VALID 170750/170950 EFKL- EFIN FINLAND FIR" + AbstractTACBulletinSerializer.NEW_LINE//
                + "     SEV TURB FCST AT 0740Z S OF LINE N5953 E01931 - N6001" + AbstractTACBulletinSerializer.NEW_LINE//
                + "     E02312 - N6008 E02606 - N6008 E02628 FL220-340 MOV N" + AbstractTACBulletinSerializer.NEW_LINE//
                + "     15KT WKN=", tacBulletin.get());
    }
}
