package fi.fmi.avi.converter.tac.taf;

import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
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
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFBulletinImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFBulletinTACSerializationTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testTokenizingAmendment() {
        final String tac = "TAF AMD EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004\r\n"//
                + "BECMG 0206/0208 FEW005 BKN020\r\n"//
                + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        final ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final Optional<TAF> taf = result.getConvertedMessage();
        assertTrue(taf.isPresent());
        final TAFBulletin bulletin = TAFBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33)//
                        .setType(BulletinHeading.Type.AMENDED)//
                        .setBulletinAugmentationNumber(1)//
                        .setGeographicalDesignator("FI")//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_SHORT)
                        .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))//
                        .build())//
                .addMessages(taf.get())//
                .build();

        final ConversionResult<String> stringResult = this.converter.convertMessage(bulletin, TACConverter.TAF_BULLETIN_POJO_TO_TAC);
        assertTrue(stringResult.getConversionIssues().isEmpty());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FCFI33 EFPP 020500 AAA"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "TAF AMD EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     SHRA BKN010 SCT030CB=", //
                stringResult.getConvertedMessage().get());
    }

    @Test
    public void testTokenizingDelayed() {
        final String tac = "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004\r\n"//
                + "BECMG 0206/0208 FEW005 BKN020\r\n"//
                + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        final ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final Optional<TAF> taf = result.getConvertedMessage();
        assertTrue(taf.isPresent());
        final TAFBulletin bulletin = TAFBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33)//
                        .setType(BulletinHeading.Type.DELAYED)//
                        .setBulletinAugmentationNumber(26)//
                        .setGeographicalDesignator("FI")//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_SHORT)
                        .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))//
                        .build())//
                .addMessages(taf.get())//
                .build();

        final ConversionResult<String> stringResult = this.converter.convertMessage(bulletin, TACConverter.TAF_BULLETIN_POJO_TO_TAC);
        assertTrue(stringResult.getConversionIssues().isEmpty());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FCFI33 EFPP 020500 RRZ" //
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004 BECMG" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     BKN010 SCT030CB=", //
                stringResult.getConvertedMessage().get());
    }

    //TODO: move to fmi-avi-messageconverter project
    @Test(expected = IllegalArgumentException.class)
    public void testBuildingWithTooManyAugmentations() {
        final String tac = "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004\r\n"//
                + "BECMG 0206/0208 FEW005 BKN020\r\n"//
                + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        final ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final Optional<TAF> taf = result.getConvertedMessage();
        assertTrue(taf.isPresent());
        TAFBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33)//
                        .setType(BulletinHeading.Type.DELAYED)//
                        .setBulletinAugmentationNumber(27)//
                        .setGeographicalDesignator("FI")//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_SHORT)
                        .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))//
                        .build())//
                .addMessages(taf.get())//
                .build();
    }

    @Test
    public void testTokenizingCorrection() {
        final String tac = "TAF COR EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004\r\n"//
                + "BECMG 0206/0208 FEW005 BKN020\r\n"//
                + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        final ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        final Optional<TAF> taf = result.getConvertedMessage();
        assertTrue(taf.isPresent());
        final TAFBulletin bulletin = TAFBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33)//
                        .setType(BulletinHeading.Type.CORRECTED)//
                        .setBulletinAugmentationNumber(2)//
                        .setGeographicalDesignator("FI")//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_SHORT)
                        .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))//
                        .build())//
                .addMessages(taf.get())//
                .build();

        final ConversionResult<String> stringResult = this.converter.convertMessage(bulletin, TACConverter.TAF_BULLETIN_POJO_TO_TAC);
        assertTrue(stringResult.getConversionIssues().isEmpty());
        assertTrue(stringResult.getConvertedMessage().isPresent());
        assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FCFI33 EFPP 020500 CCB"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "TAF COR EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     SHRA BKN010 SCT030CB=", //
                stringResult.getConvertedMessage().get());
    }

    @Test
    public void testConvertingShortTAF() {
        final String tac = "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004\r\n"//
                + "BECMG 0206/0208 FEW005 BKN020\r\n"//
                + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        final ConversionResult<TAF> pojoResult = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(pojoResult.getConversionIssues().isEmpty());
        final Optional<TAF> taf = pojoResult.getConvertedMessage();
        assertTrue(taf.isPresent());
        final TAFBulletin bulletin = TAFBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33)//
                        .setGeographicalDesignator("FI")//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_SHORT)
                        .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))//
                        .build())//
                .addMessages(taf.get())//
                .build();

        final ConversionResult<String> tacResult = converter.convertMessage(bulletin, TACConverter.TAF_BULLETIN_POJO_TO_TAC);
        assertTrue(tacResult.getConversionIssues().isEmpty());
        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FCFI33 EFPP 020500"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004 BECMG" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     BKN010 SCT030CB=", //
                tacBulletin.get());
    }

    @Test
    public void testConvertingLongTAF() {
        final String tac = "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004\r\n"//
                + "BECMG 0206/0208 FEW005 BKN020\r\n"//
                + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        final ConversionResult<TAF> pojoResult = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(pojoResult.getConversionIssues().isEmpty());
        final Optional<TAF> taf = pojoResult.getConvertedMessage();
        assertTrue(taf.isPresent());
        final TAFBulletin bulletin = TAFBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33)//
                        .setGeographicalDesignator("FI")//
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.ForecastsDataTypeDesignatorT2.FCT_AERODROME_VT_LONG)
                        .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))//
                        .build())//
                .addMessages(taf.get())//
                .build();

        final ConversionResult<String> tacResult = converter.convertMessage(bulletin, TACConverter.TAF_BULLETIN_POJO_TO_TAC);
        assertTrue(tacResult.getConversionIssues().isEmpty());
        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FTFI33 EFPP 020500"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "TAF EFKE 020532Z 0206/0312 05005KT 9999 -SHRA BKN004 BECMG" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     BKN010 SCT030CB=", //
                tacBulletin.get());
    }
}
