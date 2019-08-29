package fi.fmi.avi.converter.tac.bulletin;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.HORIZONTAL_TAB;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static fi.fmi.avi.model.bulletin.DataTypeDesignatorT1.UPPER_AIR_DATA;
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
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.bulletin.DataTypeDesignatorT2;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.bulletin.immutable.GenericMeteorologicalBulletinImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import junit.framework.TestCase;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericMeteorologicalBulletinTACSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testSerialization() {
        final GenericMeteorologicalBulletinImpl.Builder builder = GenericMeteorologicalBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(81)//
                        .setDataTypeDesignatorT1ForTAC(UPPER_AIR_DATA)
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.UpperAirDataTypeDesignatorT2.UA_MISCELLANEOUS)//
                        .setIssueTime(PartialOrCompleteTimeInstant.builder()//
                                .setPartialTime(PartialDateTime.ofDayHourMinute(27, 14, 2)))//
                        .build());

        builder.addMessages(new GenericAviationWeatherMessageImpl.Builder()//
                .setOriginalMessage("LOW WIND EFHK 270925Z\n\n1000FT     2000FT     FL050      FL100\n200/05     260/05     310/05     320/15=")
                .setTranslated(false)
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)
                .build());
        final GenericMeteorologicalBulletin msg = builder.build();

        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                        + "LOW WIND EFHK 270925Z 1000FT 2000FT FL050 FL100 200/05" + LINE_FEED.getContent()//
                        + HORIZONTAL_TAB.getContent() + "260/05 310/05 320/15=", tacBulletin.get());
    }

    @Test
    public void whitespacePassthrough() {
        final GenericMeteorologicalBulletinImpl.Builder builder = GenericMeteorologicalBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(81)//
                        .setDataTypeDesignatorT1ForTAC(UPPER_AIR_DATA)
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.UpperAirDataTypeDesignatorT2.UA_MISCELLANEOUS)//
                        .setIssueTime(PartialOrCompleteTimeInstant.builder()//
                                .setPartialTime(PartialDateTime.ofDayHourMinute(27, 14, 2)))//
                        .build());

        builder.addMessages(new GenericAviationWeatherMessageImpl.Builder()//
                .setOriginalMessage("LOW WIND EFHK 270925Z\n\n1000FT     2000FT     FL050      FL100\n200/05     260/05     310/05     320/15=")
                .setTranslated(false)
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)
                .build());
        final GenericMeteorologicalBulletin msg = builder.build();

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_WHITE_SPACE_PASSTHROUGH, ConversionHints.VALUE_WHITE_SPACE_PASSTHROUGH_ENABLE);
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, hints);
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + LINE_FEED.getContent() + LINE_FEED.getContent()//
                        + "1000FT     2000FT     FL050      FL100" + LINE_FEED.getContent()//
                        + "200/05     260/05     310/05     320/15=", tacBulletin.get());
    }

    @Test
    public void whitespacePassthroughWithNewlineAtMaxRowLength() {
        final GenericMeteorologicalBulletinImpl.Builder builder = GenericMeteorologicalBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(81)//
                        .setDataTypeDesignatorT1ForTAC(UPPER_AIR_DATA)
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.UpperAirDataTypeDesignatorT2.UA_MISCELLANEOUS)//
                        .setIssueTime(PartialOrCompleteTimeInstant.builder()//
                                .setPartialTime(PartialDateTime.ofDayHourMinute(27, 14, 2)))//
                        .build());

        builder.addMessages(new GenericAviationWeatherMessageImpl.Builder()//
                .setOriginalMessage(
                        "LOW WIND EFHK 270925Z\n\n1000FT     2000FT     FL050      FL100 FOOBAR FOOBAR FOOBAR\n200/05     260/05     310/05     320/15=")
                .setTranslated(false)
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)
                .build());
        final GenericMeteorologicalBulletin msg = builder.build();

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_WHITE_SPACE_PASSTHROUGH, ConversionHints.VALUE_WHITE_SPACE_PASSTHROUGH_ENABLE);
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, hints);
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + LINE_FEED.getContent() + LINE_FEED.getContent()//
                        + "1000FT     2000FT     FL050      FL100 FOOBAR FOOBAR FOOBAR" + LINE_FEED.getContent()//
                        + "200/05     260/05     310/05     320/15=", tacBulletin.get());
    }

    @Test
    public void whitespacePassthroughLongRow() {
        final GenericMeteorologicalBulletinImpl.Builder builder = GenericMeteorologicalBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(81)//
                        .setDataTypeDesignatorT1ForTAC(UPPER_AIR_DATA)
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.UpperAirDataTypeDesignatorT2.UA_MISCELLANEOUS)//
                        .setIssueTime(PartialOrCompleteTimeInstant.builder()//
                                .setPartialTime(PartialDateTime.ofDayHourMinute(27, 14, 2)))//
                        .build());

        builder.addMessages(new GenericAviationWeatherMessageImpl.Builder()//
                .setOriginalMessage("LOW WIND EFHK 270925Z\n\nFOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR=")
                .setTranslated(false)
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)
                .build());
        final GenericMeteorologicalBulletin msg = builder.build();

        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_WHITE_SPACE_PASSTHROUGH, ConversionHints.VALUE_WHITE_SPACE_PASSTHROUGH_ENABLE);
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, hints);
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + LINE_FEED.getContent() + LINE_FEED.getContent()//
                        + "FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR" + LINE_FEED.getContent()//
                        + "FOOBAR FOOBAR FOOBAR=", tacBulletin.get());
    }

}
