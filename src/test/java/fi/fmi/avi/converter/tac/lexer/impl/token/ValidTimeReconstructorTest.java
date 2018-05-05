package fi.fmi.avi.converter.tac.lexer.impl.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import org.junit.Before;
import org.junit.Test;

import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.TACTokenReconstructor;

public class ValidTimeReconstructorTest {

	TACTokenReconstructor reconstructor;
	ReconstructorContext<TAF> ctx;
	
	@Before
	public void setUp() throws Exception {
		reconstructor = new ValidTime.Reconstructor();
		reconstructor.setLexingFactory(new LexingFactoryImpl());
		ctx = new ReconstructorContext<>(null, new ConversionHints());
	}

	@Test
	public void testValidityLongDateFormat() throws SerializingException
	{
		TAF msg = mock(TAF.class);
		
		injectValidity(msg, 7, 2, 7, 24);
		
		List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);
		
		assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "0702/0724");
	}

	@Test
	public void testValidityShortDateFormat() throws SerializingException
	{
		TAF msg = mock(TAF.class);

		ctx.setHint(ConversionHints.KEY_VALIDTIME_FORMAT, ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT);
		injectValidity(msg, 7, 2, 7, 24);
		
		
		List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);
		
		assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "070224");
	}


	@Test
	public void testValidityShortDateFormatNextDay() throws SerializingException
	{
		TAF msg = mock(TAF.class);

		ctx.setHint(ConversionHints.KEY_VALIDTIME_FORMAT, ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT);
		injectValidity(msg, 7, 18, 8, 10);
		
		
		List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);
		
		assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "071810");
	}


	@Test
	public void testValidityShortDateFormatTooLongPeriod() throws SerializingException
	{
		TAF msg = mock(TAF.class);

		ctx.setHint(ConversionHints.KEY_VALIDTIME_FORMAT, ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT);
		injectValidity(msg, 7, 18, 8, 20);
		
		
		List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);
		
		assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "0718/0820");
	}

	private void injectValidity(TAF msg, int startDay, int startHour, int endDay, int endHour) {
		Optional<PartialOrCompleteTimePeriod> p = Optional.of(new PartialOrCompleteTimePeriod.Builder()
				.setStartTime(new PartialOrCompleteTimeInstant.Builder()
						.setPartialTime(String.format("%02d%02d", startDay, startHour))
						.setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
						.build())
				.setEndTime(new PartialOrCompleteTimeInstant.Builder()
						.setPartialTime(String.format("%02d%02d", endDay, endHour))
						.setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHour)
						.build())
				.build());

		when(msg.getValidityTime()).thenReturn(p);
	}

	private void assertOneLexeme(List<Lexeme> lexemes, Identity identity, String token) {
		assertNotNull(lexemes);
		assertEquals(1, lexemes.size());
		assertLexeme(lexemes.get(0), identity, token);
	}
	
	private void assertLexeme(Lexeme l, Identity identity, String token) {
		assertNotNull(l);
		assertEquals(identity, l.getIdentity());
		assertEquals(token, l.getTACToken());
		
	}

}
