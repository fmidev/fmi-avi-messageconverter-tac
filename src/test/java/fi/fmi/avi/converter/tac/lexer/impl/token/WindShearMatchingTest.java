package fi.fmi.avi.converter.tac.lexer.impl.token;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.junit.Before;
import org.junit.Test;

import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor.OccurrenceFrequency;

public class WindShearMatchingTest {
    WindShear windShear;

    @Before
    public void setUp() {
        windShear = new WindShear(OccurrenceFrequency.AVERAGE);
    }

    @Test
    public void testWS_ALL_RWY() {
        final Matcher m = windShear.getPattern().matcher("WS ALL RWY");

        assertTrue(m.matches());
    }

    @Test
    public void testWS_RWY03() {
        final Matcher m = windShear.getPattern().matcher("WS RWY03");

        assertTrue(m.matches());
        assertEquals("03", m.group(2));
    }

    @Test
    public void testWS_RWY04R() {
        final Matcher m = windShear.getPattern().matcher("WS RWY04R");

        assertTrue(m.matches());
        assertEquals("04R", m.group(2));
    }

    @Test
    public void testWS_R04R() {
        final Matcher m = windShear.getPattern().matcher("WS R04R");

        assertTrue(m.matches());
        assertEquals("04R", m.group(2));
    }

    @Test
    public void testWS_R18C() {
        final Matcher m = windShear.getPattern().matcher("WS R18C");

        assertTrue(m.matches());
        assertEquals("18C", m.group(2));
    }

}
