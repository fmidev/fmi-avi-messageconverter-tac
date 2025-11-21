package fi.fmi.avi.converter.tac.lexer.impl.util;

import java.util.Arrays;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@SuppressWarnings("UnnecessaryUnicodeEscape")
public enum DashVariant {
    HYPHEN_MINUS("\u002D"), // U+002D "-"
    HYPHEN("\u2010"), // U+2010 "‐"
    EN_DASH("\u2013"), // U+2013 "–"
    EM_DASH("\u2014"), // U+2014 "—"
    FIGURE_DASH("\u2012"), // U+2012 "‒"
    HORIZONTAL_BAR("\u2015"), // U+2015 "―"
    MINUS_SIGN("\u2212"), // U+2212 "−"
    SMALL_HYPHEN_MINUS("\uFE63"), // U+FE63 "﹣"
    FULLWIDTH_HYPHEN_MINUS("\uFF0D"), // U+FF0D "－"
    ;

    public static final String ALL_AS_STRING = Arrays.stream(DashVariant.values())
            .map(DashVariant::getDash)
            .collect(Collectors.joining());

    private final String dash;

    DashVariant(final String dash) {
        this.dash = dash;
    }

    public static DashVariant fromDash(final String dash) {
        requireNonNull(dash, "dash");
        return Arrays.stream(DashVariant.values())
                .filter(variant -> variant.getDash().equals(dash))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("No such dash: " + dash));
    }

    public static boolean isDash(final CharSequence charSequence) {
        return charSequence != null && charSequence.length() == 1 && ALL_AS_STRING.contains(charSequence);
    }

    public String getDash() {
        return dash;
    }
}
