package fi.fmi.avi.converter.tac.taf;

import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationWeatherMessage.ReportStatus;
import fi.fmi.avi.model.taf.TAF;

/**
 * How the TAC TAF valid time field is matched with TAF POJO validityTime or referredReport/validityTime fields.
 *
 * @see ConversionHints#KEY_TAF_REFERENCE_POLICY
 */
public enum TAFReferencePolicy {
    /**
     * When parsing and serializing AMD, COR or CNL TAFs, the referredReport/validityTime should be used to match the valid time of the TAC TAF message.
     * In other cases the validityTime of the TAF object should be matched with the valid time of the TAC TAF message.
     */
    USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD, //
            true, ReportStatus.CORRECTION, ReportStatus.AMENDMENT),

    /**
     * When parsing and serializing COR or CNL TAFs, the referredReport/validityTime should be used to match the valid time of the TAC message.
     * In other cases the validityTime of the TAF object should be matched with the valid time of the TAC TAF message.
     */
    USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL, //
            true, ReportStatus.CORRECTION),

    /**
     * When parsing and serializing CNL TAFs, the referredReport/validityTime should be used to match the valid time of the TAC message.
     * In other cases the validityTime of the TAF object should be matched with the valid time of the TAC TAF message.
     */
    USE_REFERRED_REPORT_VALID_TIME_FOR_CNL(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_REFERRED_REPORT_VALID_TIME_FOR_CNL, //
            true),

    /**
     * validityTime of the TAF object should always be matched with the valid time of the TAC TAF message regardless of the message type.
     */
    USE_OWN_VALID_TIME_ONLY(ConversionHints.VALUE_TAF_REFERENCE_POLICY_USE_OWN_VALID_TIME_ONLY, //
            false);

    /**
     * Key of TAF reference policy ConversionHint.
     */
    public static final ConversionHints.Key CONVERSION_HINT_KEY = ConversionHints.KEY_TAF_REFERENCE_POLICY;

    /**
     * Default TAF reference policy to be used when no ConversionHint has been given.
     */
    public static final TAFReferencePolicy DEFAULT_POLICY = USE_REFERRED_REPORT_VALID_TIME_FOR_COR_CNL_AMD;

    private static final Map<Object, TAFReferencePolicy> TAF_REFERENCE_POLICIES_BY_CONVERSION_HINT = Collections.unmodifiableMap(Arrays.stream(values())//
            .collect(Collectors.toMap(TAFReferencePolicy::getConversionHint, Function.identity())));

    private transient final Object conversionHint;
    private transient final boolean usingReferredOnCancel;
    private transient final Set<ReportStatus> reportStatusesToUseReferred;

    TAFReferencePolicy(final Object conversionHint, final boolean usingReferredOnCancel, final ReportStatus... reportStatusesToUseReferred) {
        this.conversionHint = requireNonNull(conversionHint);
        this.usingReferredOnCancel = usingReferredOnCancel;
        this.reportStatusesToUseReferred = reportStatusesToUseReferred.length == 0
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(Arrays.asList(reportStatusesToUseReferred)));
    }

    public static Optional<TAFReferencePolicy> tryGetFromConversionHints(final ConversionHints conversionHints) {
        return conversionHints == null
                ? Optional.empty()
                : Optional.ofNullable(TAF_REFERENCE_POLICIES_BY_CONVERSION_HINT.get(conversionHints.get(CONVERSION_HINT_KEY)));
    }

    public static TAFReferencePolicy fromConversionHint(final Object conversionHint) {
        requireNonNull(conversionHint, "conversionHint");
        final TAFReferencePolicy tafReferencePolicy = TAF_REFERENCE_POLICIES_BY_CONVERSION_HINT.get(conversionHint);
        if (tafReferencePolicy == null) {
            throw new IllegalArgumentException("Unknown ConversionHint: " + conversionHint);
        }
        return tafReferencePolicy;
    }

    /**
     * Indicates whether referred report validity time shall be used instead of current report validity time on TAF having provided {@code reportStatus} and
     * {@code cancellation} state.
     *
     * @param reportStatus
     *         report status of TAF in question
     * @param cancellation
     *         cancellation status of TAF in question
     *
     * @return {@code true} if
     *
     * @see TAF#getReferredReportValidPeriod()
     * @see TAF#getValidityTime()
     */
    public boolean useReferred(final ReportStatus reportStatus, final boolean cancellation) {
        return usingReferredOnCancel && cancellation || reportStatusesToUseReferred.contains(reportStatus);
    }

    /**
     * Returns related ConversionHint.
     *
     * @return related ConversionHint
     */
    public Object getConversionHint() {
        return conversionHint;
    }

    public boolean isUsingReferredOnCancel() {
        return usingReferredOnCancel;
    }

    public Set<ReportStatus> getReportStatusesToUseReferred() {
        return reportStatusesToUseReferred;
    }
}
