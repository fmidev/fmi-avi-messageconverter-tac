package fi.fmi.avi.converter.tac.swx;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class SWXTACParserTest {

    @Autowired
    @Qualifier("swxDummy")
    private AviMessageLexer swxDummyLexer;

    @Autowired
    private AviMessageConverter converter;

    /*
    Dummy SWX Lexer produces a message like below:

        + "SWX ADVISORY\n" //
                       + "STATUS: TEST\n"//
                       + "DTG: 20190128/1200Z\n" //
                       + "SWXC: PECASUS\n" //
                       + "ADVISORY NR: 2019/1\n"//
                       + "SWX EFFECT: SATCOM MOD\n" //
                       + "OBS SWX: 08/1200Z HNH HSH E18000 - W18000 ABV FL340\n"//
                       + "FCST SWX +6 HR: 08/1800Z NO SWX EXP\n"//
                       + "FCST SWX +12 HR: 09/0000Z NO SWX EXP\n"//
                       + "FCST SWX +18 HR: 09/0600Z DAYLIGHT SIDE\n"//
                       + "FCST SWX +24 HR: 09/1200Z NO SWX EXP\n"//
                       + "RMK: TEST TEST TEST TEST\n"
                       + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" + "SEE WWW.PECASUS.ORG \n"
                       + "NXT ADVISORY: NO FURTHER ADVISORIES\n \n",
        */

    @Test
    public void testLexer() {
        final LexemeSequence lexed = swxDummyLexer.lexMessage("foo");
        assertEquals(LexemeIdentity.SPACE_WEATHER_ADVISORY_START, lexed.getFirstLexeme().getIdentityIfAcceptable());
        lexed.getFirstLexeme().findNext(LexemeIdentity.ISSUE_TIME, (issueTime) -> {
            assertEquals(Integer.valueOf(2019), issueTime.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
            assertEquals(Integer.valueOf(1), issueTime.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
            assertEquals(Integer.valueOf(28), issueTime.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
            assertEquals(Integer.valueOf(12), issueTime.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
            assertEquals(Integer.valueOf(0), issueTime.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));
        });
    }

    @Test
    public void testParser() {
        final ConversionResult<SpaceWeatherAdvisory> result = this.converter.convertMessage("foo", TACConverter.TAC_TO_SWX_POJO);
        assertEquals(ConversionResult.Status.SUCCESS, result.getStatus());
        assertTrue(result.getConvertedMessage().isPresent());
    }

}
