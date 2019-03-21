package fi.fmi.avi.converter.tac.bulletin;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.BulletinHeadingImpl;
import fi.fmi.avi.util.GTSExchangeFileInfo;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericMeteorologicalBulletinParserTest {

    @Autowired
    private AviMessageConverter converter;

    @Autowired
    private AviMessageLexer lexer;

    @Test
    public void testLexing() {
        LexemeSequence seq = lexer.lexMessage(
                "FTFI33 EFPP 020500\n" + "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n" + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n"
                        + "BKN010 SCT030CB=");
        Lexeme l = seq.getFirstLexeme();
        assertEquals(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, l.getIdentityIfAcceptable());
        l = l.getNext();
        assertNotNull(l);
        assertEquals(Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, l.getIdentityIfAcceptable());
        l = l.getNext();
        assertNotNull(l);
        assertEquals(Lexeme.Identity.ISSUE_TIME, l.getIdentityIfAcceptable());
        while (l.hasNext()) {
            l = l.getNext();
        }
        assertEquals(Lexeme.Identity.END_TOKEN, l.getIdentityIfAcceptable());
    }

    @Test
    public void testParsing() {
        BulletinHeading heading = new BulletinHeadingImpl.Builder()//
                .setDataTypeDesignatorT1ForTAC(BulletinHeading.DataTypeDesignatorT1.FORECASTS)
                .setDataTypeDesignatorT2(BulletinHeading.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG)
                .setBulletinNumber(33)
                .setGeographicalDesignator("FI")
                .setLocationIndicator("EFPP")
                .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))
                .setType(BulletinHeading.Type.NORMAL)
                .build();
        GTSExchangeFileInfo info = new GTSExchangeFileInfo.Builder()
                .setHeading(heading)
                .setFileType(GTSExchangeFileInfo.GTSExchangeFileType.TEXT)
                .setMetadataFile(false)
                .setPFlag(GTSExchangeFileInfo.GTSExchangePFlag.A)
                .setTimeStampDay(2)
                .setTimeStampHour(5)
                .setTimeStampMinute(0)
                .build();
        ConversionHints hints = new ConversionHints(ConversionHints.KEY_BULLETIN_ID, info.toGTSExchangeFileName());
        ConversionResult<GenericMeteorologicalBulletin> result = this.converter.convertMessage("FTFI33 EFPP 020500\n"
                + "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n"
                + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n"
                + "BKN010 SCT030CB=\n"
                + "FAK EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG\n"
                + "0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA\n"
                + "BKN010 SCT030CB=", TACConverter.TAC_TO_GENERIC_BULLETIN_POJO, hints);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        Optional<GenericMeteorologicalBulletin> bulletin = result.getConvertedMessage();
        assertTrue(bulletin.isPresent());
        assertEquals(2,bulletin.get().getMessages().size());
        GenericAviationWeatherMessage msg = bulletin.get().getMessages().get(0);
        assertTrue(msg.getMessageType().isPresent());
        assertEquals(AviationCodeListUser.MessageType.TAF,msg.getMessageType().get());

        msg = bulletin.get().getMessages().get(1);
        assertFalse(msg.getMessageType().isPresent());
    }
}
