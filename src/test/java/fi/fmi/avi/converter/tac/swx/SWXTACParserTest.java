package fi.fmi.avi.converter.tac.swx;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

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
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.swx.NextAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherPhenomenon;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

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
                       + "SWX EFFECT: SATCOM MOD AND RADIATION SEV\n" //
                       + "OBS SWX: 08/1200Z HNH HSH E16000 - W2000 ABV FL340\n"//
                       + "FCST SWX +6 HR: 08/1800Z N80 W180 - N70 W75 - N60 E15 - N70 E75 - N80 W180 ABV FL370\n"//
                       + "FCST SWX +12 HR: 09/0000Z NO SWX EXP\n"//
                       + "FCST SWX +18 HR: 09/0600Z DAYLIGHT SIDE\n"//
                       + "FCST SWX +24 HR: 09/1200Z NO SWX EXP\n"//
                       + "RMK: TEST TEST TEST TEST\n"
                       + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" + "SEE WWW.PECASUS.ORG \n"
                       + "NXT ADVISORY: WILL BE ISSUED BY 20161108/0700Z\n \n",
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

        SpaceWeatherAdvisory swx = result.getConvertedMessage().get();
        assertEquals(swx.getIssuingCenter().getName().get(), "PECASUS");
        assertEquals(swx.getAdvisoryNumber().getSerialNumber(), 1);
        assertEquals(swx.getAdvisoryNumber().getYear(), 2019);
        assertEquals(2, swx.getPhenomena().size());
        assertEquals(swx.getPhenomena().get(0), SpaceWeatherPhenomenon.fromCombinedCode("SATCOM MOD"));
        assertEquals(swx.getPhenomena().get(1), SpaceWeatherPhenomenon.fromCombinedCode("RADIATION SEV"));
        assertEquals(swx.getRemarks().get().get(0), "TEST TEST TEST TEST THIS IS A TEST MESSAGE FOR TECHNICAL TEST. SEE WWW.PECASUS.ORG");
        assertEquals(swx.getNextAdvisory().getTimeSpecifier(), NextAdvisory.Type.NEXT_ADVISORY_BY);

        List<SpaceWeatherAdvisoryAnalysis> analyses = swx.getAnalyses();
        assertEquals(5, analyses.size());
        SpaceWeatherAdvisoryAnalysis analysis = analyses.get(0);
        assertTrue(analysis.getAnalysisType().isPresent());
        assertEquals(analysis.getAnalysisType().get(), SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertTrue(analysis.getRegion().isPresent());
        assertEquals(2, analysis.getRegion().get().size());

        assertTrue(analysis.getRegion().get().get(0).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegion().get().get(0).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE);
        assertTrue(analysis.getRegion().get().get(0).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegion().get().get(0).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(
                PolygonGeometry.class.isAssignableFrom(analysis.getRegion().get().get(0).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        PolygonGeometry poly = (PolygonGeometry) analysis.getRegion().get().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        Double[] expected = { -90d, -160d, -60d, -160d, -60d, 20d, -90d, 20d, -90d, -160d };
        Double[] actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));

        assertTrue(analysis.getRegion().get().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
        assertEquals(NumericMeasureImpl.builder().setValue(340d).setUom("FL").build(),
                analysis.getRegion().get().get(0).getAirSpaceVolume().get().getLowerLimit().get());

        assertTrue(analysis.getRegion().get().get(1).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegion().get().get(1).getLocationIndicator().get(),
                SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE);
        assertTrue(analysis.getRegion().get().get(1).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegion().get().get(1).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(
                PolygonGeometry.class.isAssignableFrom(analysis.getRegion().get().get(1).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        poly = (PolygonGeometry) analysis.getRegion().get().get(1).getAirSpaceVolume().get().getHorizontalProjection().get();
        expected = new Double[] { 60d, -160d, 90d, -160d, 90d, 20d, 60d, 20d, 60d, -160d };
        actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));

        analysis = analyses.get(1);
        assertTrue(analysis.getAnalysisType().isPresent());
        assertEquals(analysis.getAnalysisType().get(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertTrue(analysis.getRegion().isPresent());
        assertEquals(1, analysis.getRegion().get().size());
        assertFalse(analysis.getRegion().get().get(0).getLocationIndicator().isPresent());
        assertTrue(analysis.getRegion().get().get(0).getAirSpaceVolume().isPresent());
        assertTrue(analysis.getRegion().get().get(0).getAirSpaceVolume().get().getHorizontalProjection().isPresent());
        assertTrue(
                PolygonGeometry.class.isAssignableFrom(analysis.getRegion().get().get(0).getAirSpaceVolume().get().getHorizontalProjection().get().getClass()));
        poly = (PolygonGeometry) analysis.getRegion().get().get(0).getAirSpaceVolume().get().getHorizontalProjection().get();
        expected = new Double[] { -80d, -180d, -70d, -75d, -60d, 15d, -70d, 75d, -80d, -180d };
        actual = poly.getExteriorRingPositions().toArray(new Double[10]);
        assertTrue(Arrays.deepEquals(expected, actual));
        assertTrue(analysis.getRegion().get().get(0).getAirSpaceVolume().get().getLowerLimit().isPresent());
        assertEquals(NumericMeasureImpl.builder().setValue(370d).setUom("FL").build(),
                analysis.getRegion().get().get(0).getAirSpaceVolume().get().getLowerLimit().get());

        analysis = analyses.get(2);
        assertTrue(analysis.getAnalysisType().isPresent());
        assertEquals(analysis.getAnalysisType().get(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertTrue(analysis.isNoPhenomenaExpected());
        assertFalse(analysis.getRegion().isPresent());

        analysis = analyses.get(3);
        assertTrue(analysis.getAnalysisType().isPresent());
        assertEquals(analysis.getAnalysisType().get(), SpaceWeatherAdvisoryAnalysis.Type.FORECAST);
        assertTrue(analysis.getRegion().isPresent());
        assertEquals(1, analysis.getRegion().get().size());
        assertTrue(analysis.getRegion().get().get(0).getLocationIndicator().isPresent());
        assertEquals(analysis.getRegion().get().get(0).getLocationIndicator().get(), SpaceWeatherRegion.SpaceWeatherLocation.DAYLIGHT_SIDE);
    }

}
