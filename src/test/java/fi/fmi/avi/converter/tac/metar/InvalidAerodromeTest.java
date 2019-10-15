package fi.fmi.avi.converter.tac.metar;

import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.metar.METAR;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class InvalidAerodromeTest {

    @Autowired
    private AviMessageConverter converter;

    @Test
    public void invalidAerodromeWithSyntaxErrorsAllowed() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS);
        ConversionResult<METAR> result = this.converter.convertMessage("METAR EF 011350Z AUTO VRB02KT CAVOK 22/12 Q1008=", TACConverter.TAC_TO_METAR_POJO,
                hints);
        assertEquals(ConversionResult.Status.FAIL, result.getStatus());
        assertFalse(result.getConvertedMessage().isPresent());
        assertThat(result.getConversionIssues()).anySatisfy(issue -> assertThat(issue.getMessage()).startsWith("Aerodrome designator not given in"));
    }

    @Test
    public void invalidAerodromeWithAnyErrorsAllowed() {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_PARSING_MODE, ConversionHints.VALUE_PARSING_MODE_ALLOW_ANY_ERRORS);
        ConversionResult<METAR> result = this.converter.convertMessage("METAR EF 011350Z AUTO VRB02KT CAVOK 22/12 Q1008=", TACConverter.TAC_TO_METAR_POJO,
                hints);
        assertEquals(ConversionResult.Status.FAIL, result.getStatus());
        assertFalse(result.getConvertedMessage().isPresent());
        assertThat(result.getConversionIssues()).anySatisfy(issue -> assertThat(issue.getMessage()).startsWith("Aerodrome designator not given in"));
    }

    @Test
    public void invalidAerodromeWithStrictParsing() {
        ConversionResult<METAR> result = this.converter.convertMessage("METAR EF 011350Z AUTO VRB02KT CAVOK 22/12 Q1008=", TACConverter.TAC_TO_METAR_POJO);
        assertEquals(ConversionResult.Status.FAIL, result.getStatus());
        assertFalse(result.getConvertedMessage().isPresent());
        assertThat(result.getConversionIssues()).anySatisfy(
                issue -> assertThat(issue.getMessage()).startsWith("Input message lexing was not fully successful"));
    }

}
