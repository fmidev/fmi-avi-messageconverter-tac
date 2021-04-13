package fi.fmi.avi.converter.tac.lexer.impl.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import fi.fmi.avi.model.PartialOrCompleteTimePeriod;

public class CalculateNumberOfHoursTest {

    @Test
    public void testSingleDay() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0100/0106");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(6, hours);
    }

    @Test
    public void testNoHours() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0100/0100");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(0, hours);
    }

    @Test
    public void testSpanDaySingleHour() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0823/0900");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(1, hours);
    }

    @Test
    public void test24HourIllegalButUsedFormat() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0800/0824");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(24, hours);
    }

    @Test
    public void testSpanDayMoreThan24Hours() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0806/0912");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(30, hours);
    }

    @Test
    public void testSpanToNextMonthStartHoursMoreThanEndHours() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("3122/0108");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(10, hours);
    }

    @Test
    public void testSpanToNextMonthStartHoursLessThanEndHours_starts31st() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("3108/0122");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(38, hours);
    }

    @Test
    public void testSpanToNextMonthStartHoursLessThanEndHours_starts30th() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("3008/0122");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(38, hours);
    }

    @Test
    public void testSpanToNextMonthStartHoursLessThanEndHours_starts29th() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("2908/0122");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(38, hours);
    }

    @Test
    public void testSpanToNextMonthStartHoursLessThanEndHours_starts28th() {
        final PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("2808/0122");
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(38, hours);
    }

    @Test
    public void testSpanToNextMonthStartHoursLessThanEndHours_starts27th_illegal() {
        assertThrows("hours should not have been calculated", IllegalArgumentException.class,
                () -> ValidTime.calculateNumberOfHours(PartialOrCompleteTimePeriod.createValidityTime("2708/0122")));
    }

    @Test
    public void testFullTimeReferencesMakesIllegalOk() {
        PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("2708/0122");
        final ZonedDateTime refTime = ZonedDateTime.of(2017, 2, 27, 0, 0, 0, 0, ZoneId.of("Z"));
        period = period.toBuilder().completePartialStartingNear(refTime).build();
        final int hours = ValidTime.calculateNumberOfHours(period);
        assertEquals(62, hours);
    }

    @Test
    public void testIllegalSpanTooLong() {
        assertThrows("hours should not have been calculated", IllegalArgumentException.class,
                () -> ValidTime.calculateNumberOfHours(PartialOrCompleteTimePeriod.createValidityTime("1522/0108")));
    }

}
