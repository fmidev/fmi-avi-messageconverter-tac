package fi.fmi.avi.converter.tac.taf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACConverter.class, loader = AnnotationConfigContextLoader.class)
public class TAFHorizontalVisibilityLexemeTest {

    @Autowired
    AviMessageLexer aviMessageLexer;

    private void assertExtraVisibilityValue(final Lexeme notOkLexeme) {
        assertEquals(notOkLexeme.getIdentity(), Lexeme.Identity.HORIZONTAL_VISIBILITY);
        assertEquals(notOkLexeme.getLexerMessage(), "Horizontal visibility already given");
        assertEquals(notOkLexeme.getStatus(), Lexeme.Status.SYNTAX_ERROR);
    }

    @Test
    public void testSingleVisibility() {
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage("TAF EFKU 051052Z 0512/0520 35004KT 8000 SCT020CB=");
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertFalse(firstNotOk.isPresent());
    }

    @Test
    public void testMultipleVisibilities() {
        // It is OK to have another visibility if it has a direction
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage("TAF EFKU 051052Z 0512/0520 35004KT 8000 5000NE SCT020CB=");
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertFalse(firstNotOk.isPresent());
    }

    @Test
    public void testMultipleVisibilities2() {
        // It is OK to have another visibility if it has a direction
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage("TAF EFKU 051052Z 0512/0520 35004KT 8000 5000NE 4000SW SCT020CB=");
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertFalse(firstNotOk.isPresent());
    }

    @Test
    public void testMultipleVisibilities3() {
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage("TAF EFKU 051052Z 0512/0520 35004KT 8000 5000NE 4000NE SCT020CB=");
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertTrue(firstNotOk.isPresent());
        assertExtraVisibilityValue(firstNotOk.get());
    }

    @Test
    public void testMultipleVisibilities4() {
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage("TAF EFKU 051052Z 0512/0520 35004KT 8000 6000 SCT020CB=");
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertTrue(firstNotOk.isPresent());
        assertExtraVisibilityValue(firstNotOk.get());
    }

    @Test
    public void testMultipleVisibilities5() {
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage("TAF EFKU 051052Z 0512/0520 35004KT 8000 6000 5000 SCT020CB=");
        final List<Lexeme> notOkLexemes = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .collect(Collectors.toList());
        assertEquals(notOkLexemes.size(), 2);
        notOkLexemes.forEach(lexeme -> assertExtraVisibilityValue(lexeme));
    }

    @Test
    public void testSingleChangeGroupVisibilities() {
        final String tac = "TAF EFTU 230916Z 2309/2409 35004KT 8000 -FZRA SCT020CB BECMG 1517 7000 -TSRA=";
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage(tac);
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertFalse(firstNotOk.isPresent());
    }

    @Test
    public void testSingleChangeGroupVisibilities2() {
        final String tac = "TAF EFTU 230916Z 2309/2409 35004KT 8000 -FZRA SCT020CB BECMG 7000 -TSRA=";
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage(tac);
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertFalse(firstNotOk.isPresent());
    }

    @Test
    public void testMultipleChangeGroupVisibilities() {
        // Ot
        final String tac = "TAF EFTU 230916Z 2309/2409 35004KT 8000 -FZRA SCT020CB BECMG 1517 2000 7000 -TSRA=";
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage(tac);
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertTrue(firstNotOk.isPresent());
        assertExtraVisibilityValue(firstNotOk.get());
    }

    @Test
    public void testMultipleChangeGroupVisibilities2() {
        final String tac = "TAF EFTU 230916Z 2309/2409 35004KT 8000 -FZRA SCT020CB BECMG 7000 2000 -TSRA=";
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage(tac);
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertTrue(firstNotOk.isPresent());
        assertExtraVisibilityValue(firstNotOk.get());
    }

    @Test
    public void testMultipleChangeGroupVisibilities3() {
        final String tac = "TAF EFTU 230916Z 2309/2409 35004KT 8000 -FZRA SCT020CB BECMG 7000 -TSRA TEMPO 9000=";
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage(tac);
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertFalse(firstNotOk.isPresent());
    }
}
