package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AERODROME_DESIGNATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.AMENDMENT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CLOUD;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.END_TOKEN;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.HORIZONTAL_VISIBILITY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.ISSUE_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SURFACE_WIND;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_CHANGE_FORECAST_TIME_GROUP;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.TAF_START;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.VALID_TIME;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.WEATHER;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class TAFBulletinTACParsingTest {
    @Autowired
    private AviMessageConverter converter;

    @Autowired
    private AviMessageLexer lexer;

    @Test
    public void testSingleTAFLexing() throws Exception {
        String tac = "FCFI33 EFPP 020500 AAA" + TAFBulletinTACSerializer.NEW_LINE//
                + "TAF AMD EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004" + TAFBulletinTACSerializer.NEW_LINE//
                + "     BECMG 0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000" + TAFBulletinTACSerializer.NEW_LINE//
                + "     SHRA BKN010 SCT030CB=";
        LexemeSequence sequence = lexer.lexMessage(tac);
        Lexeme.Identity[] expected = { BULLETIN_HEADING_DATA_DESIGNATORS, BULLETIN_HEADING_LOCATION_INDICATOR, ISSUE_TIME, BULLETIN_HEADING_BBB_INDICATOR,
                TAF_START, AMENDMENT, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD,
                TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, CLOUD, CLOUD, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP,
                HORIZONTAL_VISIBILITY, WEATHER, CLOUD, CLOUD, END_TOKEN };
        AbstractAviMessageTest.assertTokenSequenceIdentityMatch(sequence, AbstractAviMessageTest.spacify(expected));
    }

    @Test
    public void testMultipleTAFLexing() throws Exception {
        String tac = "FCFI33 EFPP 020500" + TAFBulletinTACSerializer.NEW_LINE//
                + "TAF EFIV 020532Z 0206/0211 VRB02KT 9999 BKN035=" + TAFBulletinTACSerializer.NEW_LINE//
                + "TAF EFKT 020532Z 0206/0212 07003KT 9999 FEW004 BKN035=" + TAFBulletinTACSerializer.NEW_LINE//
                + "TAF EFKE 020532Z 0206/0215 05005KT 9999 -SHRA BKN004 BECMG" + TAFBulletinTACSerializer.NEW_LINE//
                + "     0206/0208 FEW005 BKN020 TEMPO 0206/0215 4000 SHRA" + TAFBulletinTACSerializer.NEW_LINE//
                + "     BKN010 SCT030CB=" + TAFBulletinTACSerializer.NEW_LINE//
                + "TAF EFKS 020532Z 0206/0213 09005KT 8000 BKN002 TEMPO 0206/0208" + TAFBulletinTACSerializer.NEW_LINE//
                + "     4000 BR BECMG 0208/0210 BKN015 TEMPO 0210/0213" + TAFBulletinTACSerializer.NEW_LINE//
                + "     BKN005=";
        LexemeSequence sequence = lexer.lexMessage(tac);
        Lexeme.Identity[] expected = { BULLETIN_HEADING_DATA_DESIGNATORS, BULLETIN_HEADING_LOCATION_INDICATOR, ISSUE_TIME,//
                TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, END_TOKEN,//
                TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, CLOUD, END_TOKEN,//
                TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, WEATHER, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,//
                TAF_CHANGE_FORECAST_TIME_GROUP, CLOUD, CLOUD, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, HORIZONTAL_VISIBILITY, WEATHER,//
                CLOUD, CLOUD, END_TOKEN,//
                TAF_START, AERODROME_DESIGNATOR, ISSUE_TIME, VALID_TIME, SURFACE_WIND, HORIZONTAL_VISIBILITY, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
                TAF_CHANGE_FORECAST_TIME_GROUP,//
                HORIZONTAL_VISIBILITY, WEATHER, TAF_FORECAST_CHANGE_INDICATOR, TAF_CHANGE_FORECAST_TIME_GROUP, CLOUD, TAF_FORECAST_CHANGE_INDICATOR,
                TAF_CHANGE_FORECAST_TIME_GROUP,//
                CLOUD, END_TOKEN };
        AbstractAviMessageTest.assertTokenSequenceIdentityMatch(sequence, AbstractAviMessageTest.spacify(expected));
    }
}
