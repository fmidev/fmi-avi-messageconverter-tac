package fi.fmi.avi.converter.tac.sigmet;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.EXER;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.FIR_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.FIR_NAME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.MWO_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.OBS_OR_FORECAST;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.PHENOMENON_SIGMET;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REAL_SIGMET_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SEQUENCE_DESCRIPTOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.TEST;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;
import static org.junit.Assert.assertEquals;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;

/**
 *
 * TODO:
 * - OBS_OR_FORECAST is not detecting correctly
 * - FirType with three words fails (NEW AMSTERDAM FIR)
 * - Wrong phenomenon is returned (EMB_TS instead of SEV_ICE_FZRA)
 * - sigmet1a.json is not yet correct ()
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestSigmetLexing extends AbstractSigmetLexingTest{
  @Autowired
  private AviMessageLexer lexer;

  @Test
  public void shouldBeObservation() {
    String tacString = "OBS=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, OBS_OR_FORECAST, END_TOKEN }));
    assertEquals("OBS"    , result.getLexemes().get(2).getTACToken());
    assertEquals("OBS"    , result.getLexemes().get(2).getParsedValue(VALUE, String.class));
  }

  @Test
  public void shouldBeForecast() {
    String tacString = "FCST=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, OBS_OR_FORECAST, END_TOKEN }));
    assertEquals("FCST"    , result.getLexemes().get(2).getTACToken());
    assertEquals("FCST"    , result.getLexemes().get(2).getParsedValue(VALUE, String.class));
  }

  @Test
  public void shouldBePhenomenonEMBD_TS() {
    String tacString = "EMBD TS=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, PHENOMENON_SIGMET, END_TOKEN }));
  }

  @Test
  public void shouldBePhenomenonSEV_ICE_FZRA() {
    String tacString = "SEV ICE (FZRA)=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("SEV ICE (FZRA)"    , result.getLexemes().get(2).getTACToken());
    assertEquals("SEV_ICE_FZRA" , result.getLexemes().get(2).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
  }


  @Test
  public void shouldBePhenomenonSEV_ICE() {
    String tacString = "SEV ICE=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    // assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, PHENOMENON_SIGMET, PHENOMENON_SIGMET_FZRA , END_TOKEN }));
    assertEquals("SEV ICE"    , result.getLexemes().get(2).getTACToken());

  }

  @Test
  public void shouldBeEHAA_SIGMET() {
    String tacString = "EHAA SIGMET=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, REAL_SIGMET_START, END_TOKEN }));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.LOCATION_INDICATOR, String.class));
  }

  @Test
  public void shouldBeEHAA_SIGMET_01() {
    String tacString = "EHAA SIGMET 01=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, REAL_SIGMET_START, SEQUENCE_DESCRIPTOR, END_TOKEN }));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.LOCATION_INDICATOR, String.class));
    assertEquals("01", trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  public void shouldBeVALID_EHDB() {
    String tacString = "VALID 111130/111530 EHDB-=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, END_TOKEN }));
    assertEquals("EHDB", trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals(Integer.valueOf(11), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.DAY1, Integer.class));
    assertEquals(Integer.valueOf(11), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.DAY2, Integer.class));
    assertEquals(Integer.valueOf(11), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.HOUR1, Integer.class));
    assertEquals(Integer.valueOf(15), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.HOUR2, Integer.class));
    assertEquals(Integer.valueOf(30), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.MINUTE1, Integer.class));
    assertEquals(Integer.valueOf(30), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.MINUTE2, Integer.class));
  }

  @Test
  public void shouldBeFIRName() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, END_TOKEN }));
    assertEquals("EHDB", trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(6).getParsedValue(ParsedValueName.VALUE, String.class));
      }

  @Ignore
  @Test
  public void shouldBeFIRName_2words() {
    String tacString = "VALID 111130/111530 EHDB- EHAA NEW AMSTERDAM FIR=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, END_TOKEN }));
    assertEquals("EHDB", trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(6).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  public void shouldBeTEST() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR TEST=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, TEST, END_TOKEN }));
  }

  @Test
  public void shouldBeEXER() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR EXER=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, EXER, END_TOKEN }));
  }

  @Test
  public void shouldBeOBSC_TS() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR OBSC TS=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("OBSC_TS", trimWhitespaces(result.getLexemes()).get(10).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
  }

  @Test
  public void shouldBeVA_ERUPTION_VA_CLD() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, SIGMET_VA_ERUPTION, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
  }

  @Test
  public void shouldBeVA_CLD() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(10).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
  }

  @Test
  public void shouldBeVA_NAME() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION MT VESUVIUS VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_NAME, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
    assertEquals("VESUVIUS", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  public void shouldBeVA_NAME_2WORDS() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION MT VESUVIUS A VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_NAME, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
    assertEquals("VESUVIUS A", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.VALUE, String.class));
  }
  @Test
  public void shouldBeVA_NAME_3words() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION MT VESUVIUS A B VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_NAME, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
    assertEquals("VESUVIUS A B", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  public void shouldBePSN() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION PSN N5200 E00520 VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_POSITION, PHENOMENON_SIGMET, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.SIGMET_PHENOMENON, String.class));
  }

}
