package fi.fmi.avi.converter.tac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.token.ValidTime;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;

/**
 * Created by rinne on 07/08/2018.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TimePeriodSerialisationTest {

    @Autowired
    private LexingFactory lexingFactory;

    @Test
    public void testMidnightEndingPeriodSerialization() throws Exception {
        TAF msg = new TAFImpl.Builder()//
                .setValidityTime(PartialOrCompleteTimePeriod.createValidityTimeDHDH("0100/0124"))//
                .buildPartial();

        final ValidTime.Reconstructor reconstructor = new ValidTime.Reconstructor();
        reconstructor.setLexingFactory(this.lexingFactory);
        final ReconstructorContext<TAF> ctx = new ReconstructorContext<>(msg);

        Optional<Lexeme> l = reconstructor.getAsLexeme(msg, TAF.class, ctx);
        assertTrue(l.isPresent());
        assertEquals("0100/0124", l.get().getTACToken());

        msg = new TAFImpl.Builder()//
                .setValidityTime(PartialOrCompleteTimePeriod.createValidityTimeDHDH("0100/0124"))//
                .withCompleteForecastTimes(YearMonth.of(2017, Month.DECEMBER), 31, 22, ZoneId.of("Z"))//
                .buildPartial();

        l = reconstructor.getAsLexeme(msg, TAF.class, ctx);
        assertTrue(l.isPresent());
        assertEquals("0100/0124", l.get().getTACToken());

        final ZonedDateTime completeStartTime = ZonedDateTime.parse("2018-01-01T00:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        final ZonedDateTime completeEndTime = ZonedDateTime.parse("2018-01-02T00:00:00Z", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        msg = new TAFImpl.Builder()//
                .setValidityTime(new PartialOrCompleteTimePeriod.Builder()//
                        .setStartTime(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHour(completeStartTime, false), completeStartTime))//
                        .setEndTime(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHour(completeEndTime, true), completeEndTime))//
                        .build()).buildPartial();

        l = reconstructor.getAsLexeme(msg, TAF.class, ctx);
        assertTrue(l.isPresent());
        assertEquals("0100/0124", l.get().getTACToken());
    }
}
