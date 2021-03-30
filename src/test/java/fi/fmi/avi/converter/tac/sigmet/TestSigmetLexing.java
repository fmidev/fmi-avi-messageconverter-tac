package fi.fmi.avi.converter.tac.sigmet;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.AbstractAviMessageTest;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.sigmet.SIGMET;
import fi.fmi.avi.model.sigmet.immutable.SIGMETImpl;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.*;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TS_ADJECTIVE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.SEV_ICE_FZRA;

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
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, PHENOMENON_TS_ADJECTIVE, PHENOMENON_TS , END_TOKEN }));
    assertEquals("EMBD"    , result.getLexemes().get(2).getTACToken());
    assertEquals("embedded"    , result.getLexemes().get(2).getParsedValue(TS_ADJECTIVE, String.class));
  }

  @Test
  public void shouldBePhenomenonSEV_ICE_FZRA() {
    String tacString = "SEV ICE (FZRA)=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, PHENOMENON_SIGMET, PHENOMENON_SIGMET_FZRA , END_TOKEN }));
    assertEquals("SEV ICE"    , result.getLexemes().get(2).getTACToken());
    assertEquals(true    , result.getLexemes().get(2).getParsedValue(SEV_ICE_FZRA, Boolean.class));
  }


  @Test
  public void shouldBePhenomenonSEV_ICE() {
    String tacString = "SEV ICE=";
    Assume.assumeTrue(String.class.isAssignableFrom(getParsingSpecification().getInputClass()));
    final LexemeSequence result = lexer.lexMessage(tacString, getLexerParsingHints());
    // assertTokenSequenceIdentityMatch(trimWhitespaces(result.getLexemes()), spacify(new LexemeIdentity[] { SIGMET_START, PHENOMENON_SIGMET, PHENOMENON_SIGMET_FZRA , END_TOKEN }));
    assertEquals("SEV ICE"    , result.getLexemes().get(2).getTACToken());


    // TODO: Fix that this is false by default:
    assertEquals(null    , result.getLexemes().get(2).getParsedValue(SEV_ICE_FZRA, Boolean.class));
  }
	

}
