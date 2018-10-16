package fi.fmi.avi.converter.tac;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import java.util.Optional;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageTACTokenizer;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFBulletinHeadingImpl;
import fi.fmi.avi.model.taf.immutable.TAFBulletinImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFBulletinTACSerializationTest {

    @Autowired
    @Qualifier("tacTokenizer")
    private AviMessageTACTokenizer tokenizer;

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void testTokenizer() throws Exception {
        String tac =
                "TAF AMD EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004\n" + "BECMG 0206/0208 FEW005 BKN020\n" + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        ConversionResult<TAF> result = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(result.getConversionIssues().isEmpty());
        Optional<TAF> taf = result.getConvertedMessage();
        assertTrue(taf.isPresent());
        TAFBulletin bulletin = new TAFBulletinImpl.Builder()//
                .setHeading(new TAFBulletinHeadingImpl.Builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33)
                        .setContainingAmendedMessages(true)
                        .setBulletinAugmentationNumber(1)
                        .setGeographicalDesignator("FI")
                        .setValidLessThan12Hours(true)
                        .build()).setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500")).addMessages(taf.get()).build();

        LexemeSequence lexemeSequence = tokenizer.tokenizeMessage(bulletin);
        assertEquals("FCFI33 EFPP 020500 AAA" + TAFBulletinTACSerializer.NEW_LINE + "TAF AMD EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004"
                + TAFBulletinTACSerializer.NEW_LINE + "     BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000" + TAFBulletinTACSerializer.NEW_LINE
                + "     SHRA BKN010 SCT030CB=", lexemeSequence.getTAC());
    }

    @Test
    public void testConverter() throws Exception {
        String tac =
                "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004\n" + "BECMG 0206/0208 FEW005 BKN020\n" + "TEMPO 0206/0215 4000 SHRA BKN010 SCT030CB=";
        ConversionResult<TAF> pojoResult = this.converter.convertMessage(tac, TACConverter.TAC_TO_TAF_POJO);
        assertTrue(pojoResult.getConversionIssues().isEmpty());
        Optional<TAF> taf = pojoResult.getConvertedMessage();
        assertTrue(taf.isPresent());
        TAFBulletin bulletin = new TAFBulletinImpl.Builder()//
                .setHeading(new TAFBulletinHeadingImpl.Builder()//
                        .setLocationIndicator("EFPP")//
                        .setBulletinNumber(33).setGeographicalDesignator("FI").setValidLessThan12Hours(true).build())
                .setIssueTime(PartialOrCompleteTimeInstant.createIssueTime("020500"))
                .addMessages(taf.get())
                .build();

        ConversionResult<String> tacResult = converter.convertMessage(bulletin, TACConverter.TAF_BULLETIN_POJO_TO_TAC);
        assertTrue(tacResult.getConversionIssues().isEmpty());
        Optional<String> tacBulletin = tacResult.getConvertedMessage();
        assertTrue(tacBulletin.isPresent());
        assertEquals("FCFI33 EFPP 020500" + TAFBulletinTACSerializer.NEW_LINE + "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004 BECMG"
                + TAFBulletinTACSerializer.NEW_LINE + "     0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA" + TAFBulletinTACSerializer.NEW_LINE
                + "     BKN010 SCT030CB=", tacBulletin.get());
    }
}
