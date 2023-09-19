package fi.fmi.avi.converter.tac.sigmet;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.IS_FORECAST;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.FIR_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.FIR_NAME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.MWO_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.OBS_OR_FORECAST;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.POLYGON_COORDINATE_PAIR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_START;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SEQUENCE_DESCRIPTOR;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_2_LINES;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_APRX_LINE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_BETWEEN_LATLON;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_CANCEL;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_ENTIRE_AREA;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_FCST_AT;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_FIR_NAME_WORD;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_INTENSITY;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_LEVEL;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_LINE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_MOVING;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_NO_VA_EXP;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_OUTSIDE_LATLON;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_PHENOMENON;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_USAGE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_VA_ERUPTION;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_VA_NAME;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_VA_POSITION;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_WITHIN;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.VALID_TIME;
import static org.junit.Assert.assertEquals;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;

/**
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TestSigmetLexing extends AbstractSigmetLexingTest{

    @Test
  public void shouldBeObservation() {
    String tacString = "SQL TS OBS=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, SIGMET_PHENOMENON, OBS_OR_FORECAST, END_TOKEN }));
    assertEquals("OBS"    , result.getLexemes().get(4).getTACToken());
    assertEquals(false    , result.getLexemes().get(4).getParsedValue(IS_FORECAST, Boolean.class));
  }

  @Test
  public void shouldBeForecast() {
    String tacString = "SQL TS FCST=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, SIGMET_PHENOMENON, OBS_OR_FORECAST, END_TOKEN }));
    assertEquals("FCST"    , result.getLexemes().get(4).getTACToken());
    assertEquals(true    , result.getLexemes().get(4).getParsedValue(IS_FORECAST, Boolean.class));
  }

  @Test
  public void shouldBeForecastAt1200() {
    String tacString = "EHAA SIGMET SQL TS FCST AT 1200Z=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, SIGMET_PHENOMENON, OBS_OR_FORECAST, END_TOKEN }));
    assertEquals(true    , result.getLexemes().get(4).getParsedValue(IS_FORECAST, Boolean.class));
    assertEquals(new Integer("12"), result.getLexemes().get(4).getParsedValue(HOUR1, Integer.class));
    assertEquals(new Integer("00"), result.getLexemes().get(4).getParsedValue(MINUTE1, Integer.class));
  }

  @Test
  public void shouldBePhenomenonEMBD_TS() {
    String tacString = "EHAA SIGMET EMBD TS=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, SIGMET_PHENOMENON, END_TOKEN }));
  }

  @Test
  public void shouldBePhenomenonSEV_ICE_FZRA() {
    String tacString = "EHAA SIGMET SEV ICE (FZRA)=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("SEV ICE (FZRA)"    , result.getLexemes().get(2).getTACToken());
    assertEquals("SEV_ICE_FZRA" , result.getLexemes().get(2).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }


  @Test
  public void shouldBePhenomenonSEV_ICE() {
    String tacString = "SEV ICE=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    // assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { REAL_SIGMET_START, SIGMET_PHENOMENON_SIGMET, PHENOMENON_SIGMET_FZRA , END_TOKEN }));
    assertEquals("SEV ICE"    , result.getLexemes().get(2).getTACToken());

  }

  @Test
  public void shouldBeEHAA_SIGMET() {
    String tacString = "EHAA SIGMET=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, END_TOKEN }));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(0).getParsedValue(ParsedValueName.LOCATION_INDICATOR, String.class));
  }

  @Test
  public void shouldBeEHAA_SIGMET_01() {
    String tacString = "EHAA SIGMET 01=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, SEQUENCE_DESCRIPTOR, END_TOKEN }));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(0).getParsedValue(ParsedValueName.LOCATION_INDICATOR, String.class));
    assertEquals("01", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.VALUE, String.class));
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
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, END_TOKEN }));
    assertEquals("EHDB", trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(6).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals("AMSTERDAM", trimWhitespaces(result.getLexemes()).get(8).getTACToken());
  }


  @Test
  public void shouldBeFIRName_2words() {
    String tacString = "VALID 111130/111530 EHDB- EHAA KUALA LUMPUR FIR=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, SIGMET_FIR_NAME_WORD, FIR_NAME, END_TOKEN }));
    assertEquals("EHDB", trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals("EHAA", trimWhitespaces(result.getLexemes()).get(6).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals("KUALA", trimWhitespaces(result.getLexemes()).get(8).getTACToken());
    assertEquals("LUMPUR", trimWhitespaces(result.getLexemes()).get(10).getTACToken());
    assertEquals("FIR", trimWhitespaces(result.getLexemes()).get(12).getTACToken());
  }

  @Test
  public void shouldBeTEST() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR TEST=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_USAGE, END_TOKEN }));
  }

  @Test
  public void shouldBeEXER() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR EXER=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_USAGE, END_TOKEN }));
  }

  @Test
  public void shouldBeOBSC_TS() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR OBSC TS=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("OBSC_TS", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeVA_ERUPTION_VA_CLD() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeVA_CLD() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeVA_NAME() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION MT VESUVIUS VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_NAME, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(16).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("VESUVIUS", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  @Ignore("2 word volcano names not fixed yet")
  public void shouldBeVA_NAME_2WORDS() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION MT VESUVIUS A VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_NAME, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(16).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("MT VESUVIUS", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  @Ignore("3 word volcano names not fixed yet")
  public void shouldBeVA_NAME_3words() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION MT VESUVIUS A B VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_NAME, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(16).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("VESUVIUS A B", trimWhitespaces(result.getLexemes()).get(14).getParsedValue(ParsedValueName.VALUE, String.class));
  }

  @Test
  public void shouldBeVA() {
    String tacString = "VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION PSN N5200 E00520 VA CLD=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_POSITION, SIGMET_PHENOMENON, END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(16).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeVA_PSN() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA ERUPTION PSN N5200 E00520 VA CLD OBS N5310 E00520=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR, FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_VA_ERUPTION, SIGMET_VA_POSITION, SIGMET_PHENOMENON, OBS_OR_FORECAST, POLYGON_COORDINATE_PAIR,END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(16).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_WI() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS WI N5310 E00520 - N5260 E00420 - N5210 E00500 - N5310 E00520=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_WITHIN, POLYGON_COORDINATE_PAIR, POLYGON_COORDINATE_PAIR_SEPARATOR, POLYGON_COORDINATE_PAIR,
          POLYGON_COORDINATE_PAIR_SEPARATOR, POLYGON_COORDINATE_PAIR,
          POLYGON_COORDINATE_PAIR_SEPARATOR, POLYGON_COORDINATE_PAIR,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_N_OF() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS N OF N5310=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_OUTSIDE_LATLON,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_N_OF_S_OF() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS N OF N5310 AND S OF N55=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_BETWEEN_LATLON,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_N_OF_AND_S_OF() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS N OF N5310 AND S OF N55=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_BETWEEN_LATLON,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_N_OF_W_OF() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS N OF N5310 AND W OF E00520=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_OUTSIDE_LATLON,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_S_OF_AND_E_OF() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS S OF N5310 AND E OF E00520=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_OUTSIDE_LATLON,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_POINT() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR HVY SS OBS N5210 E00520=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          POLYGON_COORDINATE_PAIR,
          END_TOKEN }));
    assertEquals("HVY_SS", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_N_OF_LINE() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS NE OF LINE N5210 E00520 - N5410 E00540 - N5510 E00530=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_LINE,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeGeometry_E_OF_LINE_AND_W_OF_LINE() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS E OF LINE N5210 E00520 - N5410 E00540 - N5510 E00530 AND W OF LINE N4800 E00700 - N5600 E00700=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_2_LINES,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeENTIRE_UIR() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS ENTIRE UIR=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_ENTIRE_AREA,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
  }

  @Test
  public void shouldBeAPRX() {
    String tacString = "EHAA SIGMET VALID 111130/111530 EHDB- EHAA AMSTERDAM FIR VA CLD OBS APRX 50KM WID LINE BTN N5210 E00220 - N5210 E01020 - N53 E012=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START, VALID_TIME, MWO_DESIGNATOR,
          FIR_DESIGNATOR, SIGMET_FIR_NAME_WORD, FIR_NAME, SIGMET_PHENOMENON, OBS_OR_FORECAST,
          SIGMET_APRX_LINE,
          END_TOKEN }));
    assertEquals("VA_CLD", trimWhitespaces(result.getLexemes()).get(12).getParsedValue(ParsedValueName.PHENOMENON, String.class));
    assertEquals("50", trimWhitespaces(result.getLexemes()).get(16).getParsedValue(ParsedValueName.APRX_LINE_WIDTH, String.class));
    assertEquals("KM", trimWhitespaces(result.getLexemes()).get(16).getParsedValue(ParsedValueName.APRX_LINE_WIDTH_UNIT, String.class));
  }

  @Test
  public void shouldBeLevel() {
    String[] tacStrings = {
      "1000/2000M=", "1000M=", "2000FT=", "1000/2000FT=", "10100/12000FT=", "1000/10000FT=",
      "1000M/FL200=", "2000FT/FL050=", "20000FT/FL240=",
      "TOP ABV FL100=", "ABV FL200=", "ABV 10000FT=", "TOP ABV 10000FT=",
      "FL100=", "SFC/FL100=", "1000M=", "SFC/1000M=", "1000FT=", "SFC/1000FT=",
      "10000FT=", "SFC/10000FT=", "FL100/200=",
      "TOP FL100=",
    };
    for (String tacString: tacStrings) {
      Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
      final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
      assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
            SIGMET_START, SIGMET_LEVEL,
            END_TOKEN }));
    }
  }

  @Test
  public void shouldBeMOV() {
    String tacString = "MOV ENE 11KT=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SIGMET_MOVING,
          END_TOKEN }));
    assertEquals(false, trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.STATIONARY, Boolean.class));
    assertEquals("11", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals("KT", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.UNIT, String.class));
    assertEquals("ENE", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.DIRECTION, String.class));
  }

  @Test
  public void shouldBeSTNR() {
    String tacString = "STNR=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SIGMET_MOVING,
          END_TOKEN }));
    assertEquals(true, trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.STATIONARY, Boolean.class));
  }

  @Test
  public void shouldBeINTSF() {
    String tacString = "INTSF=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SIGMET_INTENSITY,
          END_TOKEN }));
    assertEquals("INTSF", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.INTENSITY, String.class));
  }

  @Test
  public void shouldBeFCST_AT_1200() {
    String tacString = "NC FCST AT 1200Z=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SIGMET_INTENSITY,
          SIGMET_FCST_AT,
          END_TOKEN }));
    assertEquals(new Integer("12"), trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.HOUR1, Integer.class));
    assertEquals(new Integer("00"), trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.MINUTE1, Integer.class));
  }

  @Test
  public void shouldBeCNL_Sigmet() {
    String tacString = "CNL SIGMET 01 101300/101600=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SIGMET_CANCEL,
          END_TOKEN}));

    assertEquals("01", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SEQUENCE_DESCRIPTOR, String.class));
    assertEquals(new Integer("10"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.DAY1, Integer.class));
    assertEquals(new Integer("13"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.HOUR1, Integer.class));
    assertEquals(new Integer("00"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.MINUTE1, Integer.class));
  }

  @Test
  public void shouldBeNOVAEXP() {
    String tacString = "NO VA EXP=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SIGMET_NO_VA_EXP,
          END_TOKEN}));
  }

  @Test
  public void shouldBeCNL_VASigmet() {
    String tacString = "CNL SIGMET M01 101300/101600 VA MOV TO LFFF FIR=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SIGMET_CANCEL,
          END_TOKEN}));

    assertEquals("M01", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.SEQUENCE_DESCRIPTOR, String.class));
    assertEquals(new Integer("10"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.DAY1, Integer.class));
    assertEquals(new Integer("13"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.HOUR1, Integer.class));
    assertEquals(new Integer("00"), trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.MINUTE1, Integer.class));
    assertEquals("LFFF", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.MOVED_TO, String.class));
  }

  @Test
  public void shouldBeRdoactCld_Sigmet() {
    String tacString = "EHAA SIGMET 1 VALID 111130/111530 EHDB-\r\n"+
            "EHAA AMSTERDAM FIR RDOACT CLD FCST AT 1200Z WI 25KM OF N5200 E00520 STNR NC=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    for (Lexeme l: result.getLexemes()) {
        System.out.println(l);
    }
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] {
          SIGMET_START,
          SEQUENCE_DESCRIPTOR,
          VALID_TIME,
          MWO_DESIGNATOR,
          FIR_DESIGNATOR,
          SIGMET_FIR_NAME_WORD,
          FIR_NAME,
          SIGMET_PHENOMENON,
          OBS_OR_FORECAST,
          SIGMET_WITHIN_RADIUS_OF_POINT,
          SIGMET_MOVING,
          SIGMET_INTENSITY,
          END_TOKEN}));

    assertEquals("1", trimWhitespaces(result.getLexemes()).get(2).getParsedValue(ParsedValueName.VALUE, String.class));
    assertEquals(new Integer("11"), trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.DAY1, Integer.class));
    assertEquals(new Integer("11"), trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.HOUR1, Integer.class));
    assertEquals(new Integer("30"), trimWhitespaces(result.getLexemes()).get(4).getParsedValue(ParsedValueName.MINUTE1, Integer.class));
  }

}
