package fi.fmi.avi.converter.tac.bulletin;

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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Optional;

import static fi.fmi.avi.model.bulletin.DataTypeDesignatorT1.UPPER_AIR_DATA;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.CARRIAGE_RETURN;
import static fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter.LINE_FEED;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class GenericMeteorologicalBulletinTACSerializerTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testSerialization() {
        final GenericMeteorologicalBulletin msg = createGenericBulletin(
                "LOW WIND EFHK 270925Z\n\n1000FT     2000FT     FL050      FL100\n200/05     260/05     310/05     320/15=");
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, ConversionHints.EMPTY);
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                        + "LOW WIND EFHK 270925Z 1000FT 2000FT FL050 FL100 200/05" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "     260/05 310/05 320/15=", tacBulletin.get());
    }

    @Test
    public void whitespacePassthrough() {
        final GenericMeteorologicalBulletin msg = createGenericBulletin(
                "LOW WIND EFHK 270925Z\r\n\r\n1000FT     2000FT     FL050      FL100\r\n200/05     260/05     310/05     320/15=");
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, passthroughHints());
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "1000FT     2000FT     FL050      FL100" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "200/05     260/05     310/05     320/15=", tacBulletin.get());
    }

    @Test
    public void whitespacePassthroughConvertsLFtoCRLF() {
        final GenericMeteorologicalBulletin msg = createGenericBulletin(
                "LOW WIND EFHK 270925Z\n\n1000FT     2000FT     FL050      FL100\n200/05     260/05     310/05     320/15=");
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, passthroughHints());
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "1000FT     2000FT     FL050      FL100" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "200/05     260/05     310/05     320/15=", tacBulletin.get());
    }

    @Test
    public void whitespacePassthroughWithNewlineAtMaxRowLength() {
        final GenericMeteorologicalBulletin msg = createGenericBulletin(
                "LOW WIND EFHK 270925Z\n\n1000FT     2000FT     FL050      FL100 FOOBAR FOOBAR FOOBAR\n200/05     260/05     310/05     320/15=");
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, passthroughHints());
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "1000FT     2000FT     FL050      FL100 FOOBAR FOOBAR FOOBAR" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "200/05     260/05     310/05     320/15=", tacBulletin.get());
    }

    @Test
    public void whitespacePassthroughLongRow() {
        final GenericMeteorologicalBulletin msg = createGenericBulletin(
                "LOW WIND EFHK 270925Z\n\nFOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR=");
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, passthroughHints());
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FOOBAR FOOBAR FOOBAR=", tacBulletin.get());
    }

    @Test
    public void lineWrapDisabled() {
        final GenericMeteorologicalBulletin msg = createGenericBulletin(
                "LOW WIND EFHK 270925Z\n\n1000FT     2000FT     FL050      FL100\n200/05     260/05     310/05     320/15=");
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, lineWrapDisabledHints());
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()
                        + "LOW WIND EFHK 270925Z 1000FT 2000FT FL050 FL100 200/05 260/05 310/05 320/15=", tacBulletin.get());
    }

    @Test
    public void disabledLinewrapWhitespacePassthrough() {
        final GenericMeteorologicalBulletin msg = createGenericBulletin(
                "LOW WIND EFHK 270925Z\n\nFOOBAR FOOBAR FOOBAR FOOBAR FOOBAR\n\nFOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR " +
                        "FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR=");
        final ConversionResult<String> tacResult = this.converter.convertMessage(msg, TACConverter.GENERIC_BULLETIN_POJO_TO_TAC, passthroughWithLinewrapDisabledHints());
        assertEquals(ConversionResult.Status.SUCCESS, tacResult.getStatus());

        final Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        TestCase.assertEquals(//
                "UXFI81 EFKL 271402"//
                        + CARRIAGE_RETURN.getContent() + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "LOW WIND EFHK 270925Z" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + "FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR" + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent()//
                        + CARRIAGE_RETURN.getContent() + LINE_FEED.getContent() +//
                        "FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR FOOBAR=", tacBulletin.get());
    }

    private GenericMeteorologicalBulletin createGenericBulletin(final String message) {
        final GenericMeteorologicalBulletinImpl.Builder builder = createBulletinBuilder();
        builder.addMessages(GenericAviationWeatherMessageImpl.builder()//
                .setOriginalMessage(message)//
                .setTranslated(false)//
                .setMessageFormat(GenericAviationWeatherMessage.Format.TAC)//
                .build());
        return builder.build();
    }

    private GenericMeteorologicalBulletinImpl.Builder createBulletinBuilder() {
        return GenericMeteorologicalBulletinImpl.builder()//
                .setHeading(BulletinHeadingImpl.builder()//
                        .setGeographicalDesignator("FI")//
                        .setLocationIndicator("EFKL")//
                        .setBulletinNumber(81)//
                        .setDataTypeDesignatorT1ForTAC(UPPER_AIR_DATA)
                        .setDataTypeDesignatorT2(DataTypeDesignatorT2.UpperAirDataTypeDesignatorT2.UA_MISCELLANEOUS)//
                        .setIssueTime(PartialOrCompleteTimeInstant.builder()//
                                .setPartialTime(PartialDateTime.ofDayHourMinute(27, 14, 2)))//
                        .build());
    }

    private static ConversionHints passthroughHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_WHITESPACE_SERIALIZATION_MODE, ConversionHints.VALUE_WHITESPACE_SERIALIZATION_MODE_PASSTHROUGH);
        return hints;
    }

    private static ConversionHints lineWrapDisabledHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_DISABLE_LINEWRAP_SERIALIZATION_MODE, ConversionHints.VALUE_DISABLE_LINEWRAP_SERIALIZATION_MODE);
        return hints;
    }

    private static ConversionHints passthroughWithLinewrapDisabledHints() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_WHITESPACE_SERIALIZATION_MODE, ConversionHints.VALUE_WHITESPACE_SERIALIZATION_MODE_PASSTHROUGH);
        hints.put(ConversionHints.KEY_DISABLE_LINEWRAP_SERIALIZATION_MODE, ConversionHints.VALUE_DISABLE_LINEWRAP_SERIALIZATION_MODE);
        return hints;
    }

}
