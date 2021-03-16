package fi.fmi.avi.converter.tac.taf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.EnumSet;
import java.util.Optional;

import org.junit.Test;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;

public class TAFReferencePolicyTest {
    private static <T extends Serializable> T reserialize(final T object) throws IOException, ClassNotFoundException {
        final byte[] serialized;
        try (final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                final ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
            serialized = byteArrayOutputStream.toByteArray();
        }
        try (final ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serialized);
                final ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream)) {
            @SuppressWarnings("unchecked")
            final T deserialized = (T) objectInputStream.readObject();
            return deserialized;
        }
    }

    private static String msg(final TAFReferencePolicy policy, final ReportStatus reportStatus, final boolean cancellation) {
        return policy + ": " + reportStatus + "; " + cancellation;
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        for (final TAFReferencePolicy policy : TAFReferencePolicy.values()) {
            final TAFReferencePolicy reserialized = reserialize(policy);
            assertEquals(policy + "", policy, reserialized);
            assertEquals(policy + ".getConversionHint()", policy.getConversionHint(), reserialized.getConversionHint());
            assertEquals(policy + ".getReportStatusesToUseReferred()", policy.getReportStatusesToUseReferred(), reserialized.getReportStatusesToUseReferred());
            assertEquals(policy + ".isUsingReferredOnCancel()", policy.isUsingReferredOnCancel(), reserialized.isUsingReferredOnCancel());
        }
    }

    @Test
    public void testTryGetFromConversionHintsReturnsExpectedInstance() {
        for (final TAFReferencePolicy policy : TAFReferencePolicy.values()) {
            final TAFReferencePolicy actual = TAFReferencePolicy.tryGetFromConversionHints(
                    new ConversionHints(TAFReferencePolicy.CONVERSION_HINT_KEY, policy.getConversionHint()))//
                    .orElse(null);
            assertEquals(policy, actual);
        }
    }

    @Test
    public void testTryGetFromConversionHintsReturnsEmptyWhenNoValueForKey() {
        final Optional<TAFReferencePolicy> actual = TAFReferencePolicy.tryGetFromConversionHints(ConversionHints.EMPTY);
        assertFalse(actual.isPresent());
    }

    @Test
    public void testFromConversionHintReturnsExpectedInstance() {
        for (final TAFReferencePolicy policy : TAFReferencePolicy.values()) {
            final TAFReferencePolicy actual = TAFReferencePolicy.fromConversionHint(policy.getConversionHint());
            assertEquals(policy, actual);
        }
    }

    @Test
    public void testFromConversionHintFailsOnWrongConversionHint() {
        final Object invalidConversionHint = "WRONG";
        final IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> TAFReferencePolicy.fromConversionHint(invalidConversionHint));
        assertTrue(exception.getMessage().contains(invalidConversionHint.toString()));
    }

    @Test
    public void testUseReferredReturnsTrueOnCancellationWhenAppropriate() {
        final boolean cancellation = true;
        for (final TAFReferencePolicy policy : TAFReferencePolicy.values()) {
            if (policy.isUsingReferredOnCancel()) {
                for (final ReportStatus reportStatus : ReportStatus.values()) {
                    assertTrue(msg(policy, reportStatus, cancellation), policy.useReferred(reportStatus, cancellation));
                }
            }
        }
    }

    @Test
    public void testUseReferredReturnsTrueOnReportStatusWhenAppropriate() {
        final boolean cancellation = false;
        for (final TAFReferencePolicy policy : TAFReferencePolicy.values()) {
            for (final ReportStatus reportStatus : policy.getReportStatusesToUseReferred()) {
                assertTrue(msg(policy, reportStatus, cancellation), policy.useReferred(reportStatus, cancellation));
            }
        }
    }

    @Test
    public void testUseReferredReturnsFalseWhenAppropriate() {
        for (final TAFReferencePolicy policy : TAFReferencePolicy.values()) {
            final EnumSet<ReportStatus> reportStatusesNotToUseReferred = policy.getReportStatusesToUseReferred().isEmpty()
                    ? EnumSet.allOf(ReportStatus.class)
                    : EnumSet.complementOf(EnumSet.copyOf(policy.getReportStatusesToUseReferred()));
            final boolean cancellation = !policy.isUsingReferredOnCancel();
            for (final ReportStatus reportStatus : reportStatusesNotToUseReferred) {
                assertFalse(msg(policy, reportStatus, cancellation), policy.useReferred(reportStatus, cancellation));
            }
        }
    }
}
