package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

public final class LexemeCombiningRules {
    private LexemeCombiningRules() {
        throw new AssertionError();
    }

    @SafeVarargs
    public static List<Predicate<String>> rule(final Predicate<String>... rules) {
        requireNonNull(rules, "rules");
        return Arrays.asList(rules);
    }

    public static List<Predicate<String>> regexRule(final String... patterns) {
        requireNonNull(patterns, "patterns");
        return Arrays.stream(patterns)
                .map(LexemeCombiningRules::regexMatcher)
                .collect(Collectors.toList());
    }

    public static Predicate<String> regexMatcher(final String pattern) {
        requireNonNull(pattern, "pattern");
        return new RegexMatcher(Pattern.compile(pattern));
    }

    public static List<Predicate<String>> equalityRule(final String... expectedTokens) {
        requireNonNull(expectedTokens, "expectedTokens");
        return Arrays.stream(expectedTokens)
                .map(LexemeCombiningRules::equalityMatcher)
                .collect(Collectors.toList());
    }

    public static Predicate<String> equalityMatcher(final String expectedToken) {
        requireNonNull(expectedToken, "expectedToken");
        return new EqualityMatcher(expectedToken);
    }

    private static class EqualityMatcher implements Predicate<String> {
        private final String expected;

        EqualityMatcher(final String expected) {
            this.expected = expected;
        }

        @Override
        public boolean test(final String s) {
            return expected.equals(s);
        }
    }

    private static class RegexMatcher implements Predicate<String> {
        private final Pattern pattern;

        RegexMatcher(final Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean test(final String s) {
            return pattern.matcher(s).matches();
        }
    }
}
