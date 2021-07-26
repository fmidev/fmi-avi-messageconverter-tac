package fi.fmi.avi.converter.tac.airmet;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionSpecification;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.model.sigmet.AIRMET;

/**
 *
*/
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public abstract class AbstractAirmetLexingTest {

  @Autowired
  protected AviMessageLexer lexer;

  public ConversionSpecification<String, AIRMET> getParsingSpecification() {
		return TACConverter.TAC_TO_AIRMET_POJO;
  }

  public ConversionHints getLexerParsingHints() {
    return ConversionHints.AIRMET;
  }

  protected LexemeIdentity[] spacify(final LexemeIdentity[] input) {
    final List<LexemeIdentity> retval = new ArrayList<>();
    if (input != null) {
        for (int i = 0; i < input.length; i++) {
            retval.add(input[i]);
            if ((i < input.length - 1) && !(LexemeIdentity.END_TOKEN.equals(input[i + 1]))) {
                retval.add(LexemeIdentity.WHITE_SPACE);
            }
        }
    }
    return retval.toArray(new LexemeIdentity[retval.size()]);
  }

  protected List<Lexeme> trimWhitespaces(final List<Lexeme> lexemes) {
    final List<Lexeme> trimmed = new ArrayList<>(lexemes.size());
    for (final Lexeme lexeme : lexemes) {
        if (trimmed.isEmpty() //
                || !LexemeIdentity.WHITE_SPACE.equals(lexeme.getIdentity()) //
                || !LexemeIdentity.WHITE_SPACE.equals(trimmed.get(trimmed.size() - 1).getIdentity())) {
            trimmed.add(lexeme);
        }
    }
    return trimmed;
  }

  protected void assertTokenSequenceIdentityMatch(final List<Lexeme> lexemes, final LexemeIdentity... expectedIdentities) {
    // System.err.print("lexemes: ");
    // lexemes.forEach((l)->{ if (! LexemeIdentity.WHITE_SPACE.equals(l.getIdentity())) System.err.println(l);});
    assertEquals("Token sequence size does not match", expectedIdentities.length, lexemes.size());
    for (int i = 0; i < expectedIdentities.length; i++) {
        assertEquals("Mismatch at index " + i, expectedIdentities[i], lexemes.get(i).getIdentityIfAcceptable());
    }
  }
}
