package fi.fmi.avi.converter.tac.airmet;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SEQUENCE_DESCRIPTOR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestAirmetLexing extends AbstractAirmetLexingTest{
  @Autowired
  private AviMessageLexer lexer;

  @Test
  public void shouldBeEHAA_AIRMET_01() {
    String tacString = "EHAA AIRMET 01=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, REAL_AIRMET_START, SEQUENCE_DESCRIPTOR, END_TOKEN }));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.LOCATION_INDICATOR, String.class));
    assertEquals("01", trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  public void shouldBeCNL_Airmet() {
    String tacString = "CNL AIRMET 01 101300/101600=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          AIRMET_START,
          AIRMET_CANCEL,
          END_TOKEN}));

    assertEquals("01", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SEQUENCE_DESCRIPTOR, String.class));
    assertEquals(new Integer("10"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.DAY1, Integer.class));
    assertEquals(new Integer("13"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.HOUR1, Integer.class));
    assertEquals(new Integer("00"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.MINUTE1, Integer.class));
  }

  @Test
  public void shouldBeMT_OBSC() {
    String tacString = "MT OBSC";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("MT_OBSC", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeSFC_VIS() {
    String tacString = "SFC VIS 200M (FG)";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("SFC_VIS", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("200", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SURFACE_VISIBILITY, String.class));
  }

  @Test
  public void shouldBeSFC_WINDKT() {
    String tacString = "SFC WIND 100/30KT";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    for (Lexeme l: result.getLexemes()) {
      System.err.println(">:"+l.getIdentity());
    }
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("SFC_WIND", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("100", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SURFACE_WIND_DIRECTION, String.class));
    assertEquals("30", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SURFACE_WIND_SPEED, String.class));
    assertEquals("KT", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SURFACE_WIND_SPEED_UNIT, String.class));
  }

  @Test
  public void shouldBeSFC_WINMPS() {
    String tacString = "SFC WIND 090/030MPS";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertEquals("SFC_WIND", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("090", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SURFACE_WIND_DIRECTION, String.class));
    assertEquals("030", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SURFACE_WIND_SPEED, String.class));
    assertEquals("MPS", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SURFACE_WIND_SPEED_UNIT, String.class));
  }

  @Test
  public void shouldBeBKN_CLD() {
    String tacString = "BKN CLD 200/1200M";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON}));
    assertEquals("BKN_CLD", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("200", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_LOWLEVEL, String.class));
    assertEquals("1200", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_HIGHLEVEL, String.class));
    assertEquals("M", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_LEVELUNIT, String.class));
    assertNull(trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_ABOVE_LEVEL, String.class));
  }

  @Test
  public void shouldBeBKN_CLDABV() {
    String tacString = "BKN CLD 2000/ABV10000FT";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("BKN_CLD", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("2000", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_LOWLEVEL, String.class));
    assertEquals("10000", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_HIGHLEVEL, String.class));
    assertEquals("FT", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_LEVELUNIT, String.class));
    assertEquals(true, trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.CLD_ABOVE_LEVEL, Boolean.class));
  }

  @Test
  public void shouldBeISOL_TCU() {
    String tacString = "ISOL TCU";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("ISOL_TCU", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeMOD_ICE() {
    String tacString = "MOD ICE";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("MOD_ICE", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeISOL_TSGR() {
    String tacString = "ISOL TSGR";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("ISOL_TSGR", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeFRQ_CB() {
    String tacString = "FRQ CB";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { AIRMET_START, AIRMET_PHENOMENON }));
    assertEquals("FRQ_CB", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }
}
