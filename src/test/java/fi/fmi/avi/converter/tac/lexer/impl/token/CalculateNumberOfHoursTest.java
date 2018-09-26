package fi.fmi.avi.converter.tac.lexer.impl.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

import fi.fmi.avi.model.PartialOrCompleteTimePeriod;


public class CalculateNumberOfHoursTest {

	@Test
	public void testSingleDay() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0100/0106");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(6, hours);
	}

	@Test
	public void testNoHours() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0100/0100");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(0, hours);
	}

	@Test
	public void testSpanDaySingleHour() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0823/0900");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(1, hours);
	}
	

	@Test
	public void test24HourIllegalButUsedFormat() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0800/0824");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(24, hours);
	}


	@Test
	public void testSpanDayMoreThan24Hours() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("0806/0912");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(30, hours);
	}
	
	@Test
	public void testSpanToNextMonthStartHoursMoreThanEndHours() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("3122/0108");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(10, hours);
	}

	
	@Test
	public void testSpanToNextMonthStartHoursLessThanEndHours_starts31st() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("3108/0122");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(38, hours);
	}
	
	@Test
	public void testSpanToNextMonthStartHoursLessThanEndHours_starts30th() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("3008/0122");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(38, hours);
	}
	
	@Test
	public void testSpanToNextMonthStartHoursLessThanEndHours_starts29th() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("2908/0122");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(38, hours);
	}
	
	@Test
	public void testSpanToNextMonthStartHoursLessThanEndHours_starts28th() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("2808/0122");
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(38, hours);
	}
	

	@Test
	public void testSpanToNextMonthStartHoursLessThanEndHours_starts27th_illegal() {
		try {
			PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("2708/0122");
			int hours = ValidTime.calculateNumberOfHours(period);
			fail("hours should not have been calculated "+hours);
		} catch(Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}
	
	@Test
	public void testFullTimeReferencesMakesIllegalOk() {
		PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("2708/0122");
		ZonedDateTime refTime = ZonedDateTime.of(2017,2,27,0,0,0,0,ZoneId.of("Z"));
		period = period.toBuilder().completePartialStartingNear(refTime).build();
		int hours = ValidTime.calculateNumberOfHours(period);
		assertEquals(62, hours);
	}
	
	@Test
	public void testIllegalSpanTooLong() {
		try {
			PartialOrCompleteTimePeriod period = PartialOrCompleteTimePeriod.createValidityTime("1522/0108");
			int hours = ValidTime.calculateNumberOfHours(period);
			fail("hours should not have been calculated "+hours);
		} catch(Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}
	
}
