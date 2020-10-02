package fi.fmi.avi.converter.tac.taf;

import static org.junit.Assert.assertEquals;
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
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACConverter.class, loader = AnnotationConfigContextLoader.class)
public class TAFSurfaceWindLexemeTest {

    @Autowired
    AviMessageLexer aviMessageLexer;

    private void assertExtraSurfaceWind(final Lexeme notOkLexeme) {
        assertEquals(notOkLexeme.getIdentity(), LexemeIdentity.SURFACE_WIND);
        assertEquals(notOkLexeme.getLexerMessage(), "Surface wind already given");
        assertEquals(notOkLexeme.getStatus(), Lexeme.Status.SYNTAX_ERROR);
    }

    @Test
    public void testMultipleSurfaceWinds() {
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage("TAF EFKU 051052Z 0512/0520 35004KT 19013KT 19013KT 8000 4000SW SCT020CB=");
        final List<Lexeme> notOkLexemes = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .collect(Collectors.toList());
        assertEquals(notOkLexemes.size(), 2);
        notOkLexemes.forEach(lexeme -> assertExtraSurfaceWind(lexeme));
    }

    @Test
    public void testMultipleChangeGroupWinds() {
        final String tac = "TAF EFTU 230916Z 2309/2409 35004KT 8000 -FZRA SCT020CB BECMG 1517 2000 36004KT 37004KT -TSRA=";
        final LexemeSequence lexemeSequence = aviMessageLexer.lexMessage(tac);
        final Optional<Lexeme> firstNotOk = lexemeSequence.getLexemes().stream()//
                .filter(l -> !l.getStatus().equals(Lexeme.Status.OK))//
                .findFirst();
        assertTrue(firstNotOk.isPresent());
        assertExtraSurfaceWind(firstNotOk.get());
    }

}
