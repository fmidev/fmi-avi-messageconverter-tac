package fi.fmi.avi.converter.tac.taf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Locale;

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
import fi.fmi.avi.model.taf.TAF;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFInvalidTimesTest {
    private static final String[] DAY_HOUR_MINUTE_TEMPLATES = new String[] {//
            "TAF YUDO %02d%02d%02d 0100/0200 000000KT CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK FM%02d%02d%02d 000000KT CAVOK=", //
    };
    private static final String[] DAY_HOUR_TEMPLATES = new String[] {//
            "TAF YUDO 010000 %02d%02d/0100 000000KT CAVOK=", //
            "TAF YUDO 010000 0100/%02d%02d 000000KT CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK BECMG %02d%02d/0112 CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK BECMG 0112/%02d%02d CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK TEMPO %02d%02d/0112 CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK TEMPO 0112/%02d%02d CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB30 %02d%02d/0112 CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB30 0112/%02d%02d CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB30 TEMPO %02d%02d/0112 CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB30 TEMPO 0112/%02d%02d CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB40 %02d%02d/0112 CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB40 0112/%02d%02d CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB40 TEMPO %02d%02d/0112 CAVOK=", //
            "TAF YUDO 010000 0100/0200 000000KT CAVOK PROB40 TEMPO 0112/%02d%02d CAVOK=", //
    };
    private static final int[] VALID_DAYS = new int[] { 1, 31 };
    private static final int[] INVALID_DAYS = new int[] { 0, 32 };
    private static final int[] VALID_HOURS = new int[] { 0, 23 };
    private static final int[] INVALID_HOURS = new int[] { 25 };
    private static final int[] VALID_MINUTES = new int[] { 0, 59 };
    private static final int[] INVALID_MINUTES = new int[] { 60 };
    private static final int[][] VALID_HOUR_MINUTES = new int[][] { new int[] { 24, 0 } };
    private static final int[][] INVALID_HOUR_MINUTES = new int[][] { new int[] { 24, 1 }, new int[] { 24, 59 }, };

    @Autowired
    private AviMessageConverter converter;

    private void assertValid(final String tacTAF) {
        final ConversionResult<TAF> result = converter.convertMessage(tacTAF, TACConverter.TAC_TO_TAF_POJO);
        assertEquals(tacTAF, Collections.emptyList(), result.getConversionIssues());
        assertEquals(tacTAF, ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue("convertedMessage", result.getConvertedMessage().isPresent());
    }

    private void assertInvalid(final String tacTAF) {
        final ConversionResult<TAF> result = converter.convertMessage(tacTAF, TACConverter.TAC_TO_TAF_POJO);
        assertNotEquals(tacTAF, Collections.emptyList(), result.getConversionIssues());
        assertEquals(tacTAF, ConversionResult.Status.FAIL, result.getStatus());
        assertFalse("convertedMessage", result.getConvertedMessage().isPresent());
    }

    @Test
    public void testValidDayHourMinute() {
        for (final String template : DAY_HOUR_MINUTE_TEMPLATES) {
            for (final int day : VALID_DAYS) {
                for (final int hour : VALID_HOURS) {
                    for (final int minute : VALID_MINUTES) {
                        assertValid(String.format(Locale.US, template, day, hour, minute));
                    }
                }
                for (final int[] hourMinute : VALID_HOUR_MINUTES) {
                    final int hour = hourMinute[0];
                    final int minute = hourMinute[1];
                    assertValid(String.format(Locale.US, template, day, hour, minute));
                }
            }
        }
    }

    @Test
    public void testInvalidDayHourMinute() {
        for (final String template : DAY_HOUR_MINUTE_TEMPLATES) {
            for (final int day : INVALID_DAYS) {
                for (final int hour : VALID_HOURS) {
                    for (final int minute : VALID_MINUTES) {
                        assertInvalid(String.format(Locale.US, template, day, hour, minute));
                    }
                }
                for (final int[] hourMinute : VALID_HOUR_MINUTES) {
                    final int hour = hourMinute[0];
                    final int minute = hourMinute[1];
                    assertInvalid(String.format(Locale.US, template, day, hour, minute));
                }
            }
            for (final int day : VALID_DAYS) {
                for (final int hour : INVALID_HOURS) {
                    for (final int minute : VALID_MINUTES) {
                        assertInvalid(String.format(Locale.US, template, day, hour, minute));
                    }
                }
                for (final int hour : VALID_HOURS) {
                    for (final int minute : INVALID_MINUTES) {
                        assertInvalid(String.format(Locale.US, template, day, hour, minute));
                    }
                }
                for (final int[] hourMinute : INVALID_HOUR_MINUTES) {
                    final int hour = hourMinute[0];
                    final int minute = hourMinute[1];
                    assertInvalid(String.format(Locale.US, template, day, hour, minute));
                }
            }
        }
    }

    @Test
    public void testValidDayHour() {
        for (final String template : DAY_HOUR_TEMPLATES) {
            for (final int day : VALID_DAYS) {
                for (final int hour : VALID_HOURS) {
                    assertValid(String.format(Locale.US, template, day, hour));
                }
            }
        }
    }

    @Test
    public void testInvalidDayHour() {
        for (final String template : DAY_HOUR_TEMPLATES) {
            for (final int day : INVALID_DAYS) {
                for (final int hour : VALID_HOURS) {
                    assertInvalid(String.format(Locale.US, template, day, hour));
                }
            }
            for (final int day : VALID_DAYS) {
                for (final int hour : INVALID_HOURS) {
                    assertInvalid(String.format(Locale.US, template, day, hour));
                }
            }
        }
    }
}
