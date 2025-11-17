package fi.fmi.avi.converter.tac.swx.amd82;

import fi.fmi.avi.converter.AviMessageConverter;
import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.conf.TACConverter;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.amd82.*;
import fi.fmi.avi.model.swx.amd82.immutable.SpaceWeatherAdvisoryAmd82Impl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.unitils.thirdparty.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class SWXAmd82TACConversionTest {
    @Autowired
    private AviMessageConverter converter;

    private static String getInput(final String fileName) throws IOException {
        try (final InputStream is = SWXAmd82ReconstructorTest.class.getResourceAsStream(fileName)) {
            Objects.requireNonNull(is);
            return IOUtils.toString(is, "UTF-8");
        }
    }

    private static String normalizeTac(final String input) {
        return input
                .replaceAll("([^\\s])\\s*-\\s*([^\\s])", "$1 - $2")
                .replaceAll("\\s*\\r?\\n", "\r\n");
    }

    private static SpaceWeatherAdvisoryAmd82 normalizeTac(final SpaceWeatherAdvisoryAmd82 input) {
        return SpaceWeatherAdvisoryAmd82Impl.Builder.from(input)
                .mapTranslatedTAC(SWXAmd82TACConversionTest::normalizeTac)
                .build();
    }

    private SpaceWeatherAdvisoryAmd82 parseAndAssertSuccess(final String initialTac, final ConversionHints hints) {
        final ConversionResult<SpaceWeatherAdvisoryAmd82> parseResult = this.converter.convertMessage(initialTac, TACConverter.TAC_TO_SWX_AMD82_POJO, hints);
        assertThat(parseResult.getConversionIssues()).isEmpty();
        assertThat(parseResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final SpaceWeatherAdvisoryAmd82 parsedModel = parseResult.getConvertedMessage().orElse(null);
        assertThat(parsedModel).isNotNull();
        return parsedModel;
    }

    private String serializeAndAssertSuccess(final SpaceWeatherAdvisoryAmd82 parsedModel, final ConversionHints hints) {
        final ConversionResult<String> serializeResult = this.converter.convertMessage(parsedModel, TACConverter.SWX_AMD82_POJO_TO_TAC, hints);
        assertThat(serializeResult.getConversionIssues()).isEmpty();
        assertThat(serializeResult.getStatus()).isEqualTo(ConversionResult.Status.SUCCESS);
        final String serializedTac = serializeResult.getConvertedMessage().orElse(null);
        assertThat(serializedTac).isNotNull();
        return serializedTac;
    }

    private SpaceWeatherAdvisoryAmd82 convertAndAssertNoDataLossOnReconversions(final String fileName) throws IOException {
        return convertAndAssertNoDataLossOnReconversions(fileName, new ConversionHints());
    }

    private SpaceWeatherAdvisoryAmd82 convertAndAssertNoDataLossOnReconversions(
            final String fileName, final ConversionHints hints) throws IOException {
        return convertAndAssertNoDataLossOnReconversions(fileName, hints, hints);
    }

    private SpaceWeatherAdvisoryAmd82 convertAndAssertNoDataLossOnReconversions(
            final String fileName, final ConversionHints parseHints, final ConversionHints serializeHints) throws IOException {
        final String initialTac = getInput(fileName);
        final SpaceWeatherAdvisoryAmd82 parsedModel = parseAndAssertSuccess(initialTac, parseHints);
        final String reserializedTac = serializeAndAssertSuccess(parsedModel, serializeHints);
        assertThat(reserializedTac).isEqualTo(normalizeTac(initialTac));
        final SpaceWeatherAdvisoryAmd82 roundTrippedModel = parseAndAssertSuccess(reserializedTac, parseHints);
        assertThat(roundTrippedModel)
                .usingRecursiveComparison()
                .isEqualTo(normalizeTac(parsedModel));
        return roundTrippedModel;
    }

    @Test
    public void testA2_4() throws IOException {
        final SpaceWeatherAdvisoryAmd82 msg = convertAndAssertNoDataLossOnReconversions("spacewx-A2-4.tac");

        assertThat(msg.getIssuingCenter().getName()).hasValue("DONLON");
        assertThat(msg.getAdvisoryNumber().getSerialNumber()).isEqualTo(2);
        assertThat(msg.getAdvisoryNumber().getYear()).isEqualTo(2016);
        assertThat(msg.getReplaceAdvisoryNumbers().get(0).getSerialNumber()).isEqualTo(1);
        assertThat(msg.getReplaceAdvisoryNumbers().get(0).getYear()).isEqualTo(2016);
        assertThat(msg.getEffect()).isEqualTo(Effect.RADIATION_AT_FLIGHT_LEVELS);

        assertThat(msg.getAnalyses()).hasSize(5);
        assertThat(msg.getAnalyses().get(0).getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        assertThat(msg.getAnalyses().get(0).getIntensityAndRegions().get(0).getIntensity()).isEqualTo(Intensity.MODERATE);
    }

    @Test
    public void testA7_3() throws IOException {
        final SpaceWeatherAdvisoryAmd82 msg = convertAndAssertNoDataLossOnReconversions("spacewx-A7-3.tac");

        assertThat(msg.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime))
                .hasValue(ZonedDateTime.parse("2020-11-08T01:00:00Z"));
        assertThat(msg.getIssuingCenter().getName()).hasValue("DONLON");
        assertThat(msg.getEffect()).isEqualTo(Effect.HF_COMMUNICATIONS);
        assertThat(msg.getAdvisoryNumber()).satisfies(num -> {
            assertThat(num.getYear()).isEqualTo(2020);
            assertThat(num.getSerialNumber()).isEqualTo(1);
        });
        assertThat(msg.getAnalyses())
                .hasSize(5)
                .first()
                .satisfies(analysis -> {
                    assertThat(analysis.getTime().getCompleteTime().orElse(null))
                            .isEqualTo(ZonedDateTime.parse("2020-11-08T01:00:00Z"));
                    assertThat(analysis.getIntensityAndRegions())
                            .extracting(SpaceWeatherIntensityAndRegion::getIntensity)
                            .containsExactly(Intensity.SEVERE, Intensity.MODERATE);
                    assertThat(analysis.getIntensityAndRegions().get(0).getRegions())
                            .allSatisfy(region -> {
                                assertThat(region.getLongitudeLimitMinimum()).isEmpty();
                                assertThat(region.getLongitudeLimitMaximum()).isEmpty();
                            })
                            .extracting(region -> region.getLocationIndicator().orElse(null))
                            .containsExactly(
                                    SpaceWeatherRegion.SpaceWeatherLocation.MIDDLE_NORTHERN_HEMISPHERE,
                                    SpaceWeatherRegion.SpaceWeatherLocation.EQUATORIAL_LATITUDES_NORTHERN_HEMISPHERE,
                                    SpaceWeatherRegion.SpaceWeatherLocation.EQUATORIAL_LATITUDES_SOUTHERN_HEMISPHERE,
                                    SpaceWeatherRegion.SpaceWeatherLocation.MIDDLE_LATITUDES_SOUTHERN_HEMISPHERE,
                                    SpaceWeatherRegion.SpaceWeatherLocation.DAYSIDE
                            );
                    assertThat(analysis.getIntensityAndRegions().get(1).getRegions())
                            .extracting(region -> region.getLocationIndicator().orElse(null))
                            .containsExactly(SpaceWeatherRegion.SpaceWeatherLocation.NIGHTSIDE);
                });
        assertThat(msg.getAnalyses().stream().skip(1))
                .allSatisfy(nilAnalysis -> assertThat(nilAnalysis.getNilReason())
                        .hasValue(SpaceWeatherAdvisoryAnalysis.NilReason.NO_SWX_EXPECTED));
        assertThat(msg.getRemarks().orElse(null)).isNotEmpty();
        assertThat(msg.getNextAdvisory()).satisfies(next -> {
            assertThat(next.getTimeSpecifier()).isEqualTo(NextAdvisory.Type.NEXT_ADVISORY_BY);
            assertThat(next.getTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime)).
                    hasValue(ZonedDateTime.parse("2020-11-08T07:00:00Z"));
        });
    }

    @Test
    public void testA7_4() throws IOException {
        final SpaceWeatherAdvisoryAmd82 msg = convertAndAssertNoDataLossOnReconversions("spacewx-A7-4.tac");

        assertThat(msg.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime))
                .hasValue(ZonedDateTime.parse("2020-11-08T01:00:00Z"));
        assertThat(msg.getIssuingCenter().getName()).hasValue("DONLON");
        assertThat(msg.getEffect()).isEqualTo(Effect.GNSS_BASED_NAVIGATION_AND_SURVEILLANCE);
        assertThat(msg.getAdvisoryNumber()).satisfies(num -> {
            assertThat(num.getYear()).isEqualTo(2020);
            assertThat(num.getSerialNumber()).isEqualTo(2);
        });
        assertThat(msg.getReplaceAdvisoryNumbers())
                .hasSize(1)
                .first()
                .satisfies(num -> {
                    assertThat(num.getYear()).isEqualTo(2020);
                    assertThat(num.getSerialNumber()).isEqualTo(1);
                });
        assertThat(msg.getAnalyses()).hasSize(5);
        assertThat(msg.getAnalyses().stream().limit(2))
                .allSatisfy(analysis -> {
                    assertThat(analysis.getIntensityAndRegions())
                            .extracting(SpaceWeatherIntensityAndRegion::getIntensity)
                            .containsExactly(Intensity.MODERATE);
                    assertThat(analysis.getIntensityAndRegions().get(0).getRegions())
                            .allSatisfy(region -> {
                                assertThat(region.getLongitudeLimitMinimum()).hasValue(-180.0);
                                assertThat(region.getLongitudeLimitMaximum()).hasValue(180.0);
                            })
                            .extracting(region -> region.getLocationIndicator().orElse(null))
                            .containsExactly(
                                    SpaceWeatherRegion.SpaceWeatherLocation.HIGH_NORTHERN_HEMISPHERE,
                                    SpaceWeatherRegion.SpaceWeatherLocation.HIGH_LATITUDES_SOUTHERN_HEMISPHERE
                            );
                })
                .extracting(analysis -> analysis.getTime().getCompleteTime().orElse(null))
                .containsExactly(
                        ZonedDateTime.parse("2020-11-08T01:00:00Z"),
                        ZonedDateTime.parse("2020-11-08T07:00:00Z")
                );
        assertThat(msg.getAnalyses().stream().skip(2))
                .allSatisfy(nilAnalysis -> assertThat(nilAnalysis.getNilReason())
                        .hasValue(SpaceWeatherAdvisoryAnalysis.NilReason.NO_SWX_EXPECTED));
        assertThat(msg.getRemarks().orElse(null)).isNotEmpty();
        assertThat(msg.getNextAdvisory()).satisfies(next -> {
            assertThat(next.getTimeSpecifier()).isEqualTo(NextAdvisory.Type.NEXT_ADVISORY_BY);
            assertThat(next.getTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime)).
                    hasValue(ZonedDateTime.parse("2020-11-08T07:00:00Z"));
        });
    }

    @Test
    public void testA7_5() throws IOException {
        final SpaceWeatherAdvisoryAmd82 msg = convertAndAssertNoDataLossOnReconversions("spacewx-A7-5.tac");

        assertThat(msg.getIssueTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime))
                .hasValue(ZonedDateTime.parse("2020-11-08T01:00:00Z"));
        assertThat(msg.getIssuingCenter().getName()).hasValue("DONLON");
        assertThat(msg.getEffect()).isEqualTo(Effect.RADIATION_AT_FLIGHT_LEVELS);
        assertThat(msg.getAdvisoryNumber()).satisfies(num -> {
            assertThat(num.getYear()).isEqualTo(2020);
            assertThat(num.getSerialNumber()).isEqualTo(15);
        });
        assertThat(msg.getReplaceAdvisoryNumbers())
                .hasSize(2)
                .allSatisfy(num -> assertThat(num.getYear()).isEqualTo(2020))
                .extracting(AdvisoryNumber::getSerialNumber)
                .containsExactly(13, 14);
        assertThat(msg.getAnalyses())
                .hasSize(5)
                .first()
                .satisfies(analysis -> {
                    assertThat(analysis.getTime().getCompleteTime().orElse(null))
                            .isEqualTo(ZonedDateTime.parse("2020-11-08T01:00:00Z"));
                    assertThat(analysis.getIntensityAndRegions())
                            .extracting(SpaceWeatherIntensityAndRegion::getIntensity)
                            .containsExactly(Intensity.MODERATE);
                    assertThat(analysis.getIntensityAndRegions().get(0).getRegions())
                            .hasSize(1)
                            .first()
                            .satisfies(region -> {
                                assertThat(region.getLongitudeLimitMinimum()).isEmpty();
                                assertThat(region.getLongitudeLimitMaximum()).isEmpty();
                                assertThat(region.getLocationIndicator()).isEmpty();
                                assertThat(region.getAirSpaceVolume()
                                        .flatMap(AirspaceVolume::getHorizontalProjection)
                                        .map(geometry -> ((PolygonGeometry) geometry).getExteriorRingPositions())
                                        .orElse(Collections.emptyList()))
                                        .containsExactly(80.0, -180.0, 70.0, -75.0, 60.0, 15.0, 70.0, 75.0, 80.0, -180.0);
                                assertThat(region.getAirSpaceVolume()
                                        .flatMap(AirspaceVolume::getLowerLimit)
                                        .map(NumericMeasure::getValue))
                                        .hasValue(400.0);
                                assertThat(region.getAirSpaceVolume().flatMap(AirspaceVolume::getUpperLimit))
                                        .isEmpty();
                            });
                });
        assertThat(msg.getAnalyses().stream().skip(1))
                .allSatisfy(nilAnalysis -> assertThat(nilAnalysis.getNilReason())
                        .hasValue(SpaceWeatherAdvisoryAnalysis.NilReason.NO_SWX_EXPECTED));
        assertThat(msg.getRemarks().orElse(null)).isNotEmpty();
        assertThat(msg.getNextAdvisory()).satisfies(next -> {
            assertThat(next.getTimeSpecifier()).isEqualTo(NextAdvisory.Type.NEXT_ADVISORY_BY);
            assertThat(next.getTime().flatMap(PartialOrCompleteTimeInstant::getCompleteTime)).
                    hasValue(ZonedDateTime.parse("2020-11-08T07:00:00Z"));
        });
    }

    @Test
    public void test_pecasus_mnhmsh() throws IOException {
        final ConversionHints hints = new ConversionHints();
        hints.put(ConversionHints.KEY_ADVISORY_LABEL_WIDTH, 19);
        final SpaceWeatherAdvisoryAmd82 msg = convertAndAssertNoDataLossOnReconversions("spacewx-pecasus-mnhmsh.tac", hints);

        assertThat(msg.getIssuingCenter().getName()).hasValue("PECASUS");
        assertThat(msg.getAdvisoryNumber().getSerialNumber()).isEqualTo(9);
        assertThat(msg.getAdvisoryNumber().getYear()).isEqualTo(2020);
        assertThat(msg.getReplaceAdvisoryNumbers().get(0).getSerialNumber()).isEqualTo(8);
        assertThat(msg.getReplaceAdvisoryNumbers().get(0).getYear()).isEqualTo(2020);
        assertThat(msg.getEffect()).isEqualTo(Effect.HF_COMMUNICATIONS);
        assertThat(msg.getAnalyses()).hasSize(5);
        final SpaceWeatherAdvisoryAnalysis analysis = msg.getAnalyses().get(0);
        assertThat(analysis.getAnalysisType()).isEqualTo(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION);
        final SpaceWeatherIntensityAndRegion intensityAndRegion = analysis.getIntensityAndRegions().get(0);
        assertThat(intensityAndRegion.getIntensity()).isEqualTo(Intensity.MODERATE);
        assertThat(intensityAndRegion.getRegions())
                .extracting(region -> region.getLocationIndicator()
                        .map(SpaceWeatherRegion.SpaceWeatherLocation::getCode)
                        .orElse(null))
                .containsExactly("MNH", "MSH", "EQN");

        assertThat(msg.getAnalyses().stream().skip(1))
                .allSatisfy(nilAnalysis -> assertThat(nilAnalysis.getNilReason())
                        .hasValue(SpaceWeatherAdvisoryAnalysis.NilReason.NO_INFORMATION_AVAILABLE));
    }

    @Test
    public void testMultipleReplaceNumbers() throws IOException {
        final SpaceWeatherAdvisoryAmd82 advisory = convertAndAssertNoDataLossOnReconversions("spacewx-multiple-replace-numbers.tac");

        assertThat(advisory.getIssuingCenter().getName()).hasValue("DONLON");
        assertThat(advisory.getAdvisoryNumber().getSerialNumber()).isEqualTo(8);
        assertThat(advisory.getAdvisoryNumber().getYear()).isEqualTo(2016);

        assertThat(advisory.getReplaceAdvisoryNumbers())
                .hasSize(4)
                .extracting(AdvisoryNumber::getSerialNumber)
                .containsExactly(7, 6, 5, 4);
        assertThat(advisory.getReplaceAdvisoryNumbers())
                .extracting(AdvisoryNumber::getYear)
                .allSatisfy(year -> assertThat(year).isEqualTo(2016));
    }
}
