package fi.fmi.avi.converter.tac.lexer.impl.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.TACTokenReconstructor;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.taf.TAF;

public class ValidTimeReconstructorTest {

    TACTokenReconstructor reconstructor;
    ReconstructorContext<TAF> ctx;

    @Before
    public void setUp() {
        reconstructor = new ValidTime.Reconstructor();
        reconstructor.setLexingFactory(new LexingFactoryImpl());
        ctx = new ReconstructorContext<>(null, new ConversionHints());
    }

    @Test
    public void testValidityLongDateFormat() throws SerializingException {
        final TAF msg = mock(TAF.class);

        injectValidity(msg, 7, 2, 7, 24);

        final List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);

        assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "0702/0724");
    }

    @Test
    public void testValidityShortDateFormat() throws SerializingException {
        final TAF msg = mock(TAF.class);

        ctx.setHint(ConversionHints.KEY_VALIDTIME_FORMAT, ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT);
        injectValidity(msg, 7, 2, 7, 24);

        final List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);

        assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "070224");
    }

    @Test
    public void testValidityShortDateFormatNextDay() throws SerializingException {
        final TAF msg = mock(TAF.class);

        ctx.setHint(ConversionHints.KEY_VALIDTIME_FORMAT, ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT);
        injectValidity(msg, 7, 18, 8, 10);

        final List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);

        assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "071810");
    }

    @Test
    public void testValidityShortDateFormatTooLongPeriod() throws SerializingException {
        final TAF msg = mock(TAF.class);

        ctx.setHint(ConversionHints.KEY_VALIDTIME_FORMAT, ConversionHints.VALUE_VALIDTIME_FORMAT_PREFER_SHORT);
        injectValidity(msg, 7, 18, 8, 20);

        final List<Lexeme> l = reconstructor.getAsLexemes(msg, TAF.class, ctx);

        assertOneLexeme(l, Lexeme.Identity.VALID_TIME, "0718/0820");
    }

    private void injectValidity(final TAF msg, final int startDay, final int startHour, final int endDay, final int endHour) {
        final Optional<PartialOrCompleteTimePeriod> p = Optional.of(PartialOrCompleteTimePeriod.builder()//
                .setStartTime(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHour(startDay, startHour)))//
                .setEndTime(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHour(endDay, endHour)))//
                .build());

        when(msg.getValidityTime()).thenReturn(p);
    }

    private void assertOneLexeme(final List<Lexeme> lexemes, final Identity identity, final String token) {
        assertNotNull(lexemes);
        assertEquals(1, lexemes.size());
        assertLexeme(lexemes.get(0), identity, token);
    }

    private void assertLexeme(final Lexeme l, final Identity identity, final String token) {
        assertNotNull(l);
        assertEquals(identity, l.getIdentity());
        assertEquals(token, l.getTACToken());

    }

}
