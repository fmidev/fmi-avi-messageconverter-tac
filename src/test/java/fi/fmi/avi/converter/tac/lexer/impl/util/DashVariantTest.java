package fi.fmi.avi.converter.tac.lexer.impl.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class DashVariantTest {
    @Test
    public void allDashesAreOneCharacter() {
        assertThat(DashVariant.values()).allSatisfy(variant ->
                assertThat(variant.getDash()).hasSize(1));
    }

    @Test
    public void allDashesAreUnique() {
        final Set<String> dashSet = Arrays.stream(DashVariant.values())
                .map(DashVariant::getDash)
                .collect(Collectors.toSet());
        final List<String> dashList = Arrays.stream(DashVariant.values())
                .map(DashVariant::getDash)
                .collect(Collectors.toList());

        assertThat(dashSet).containsExactlyInAnyOrderElementsOf(dashList);
    }

    @Test
    public void fromDashReturnsCorrectElement() {
        assertThat(DashVariant.values()).allSatisfy(variant ->
                assertThat(DashVariant.fromDash(variant.getDash())).isEqualTo(variant));
    }

    @Test
    public void allAsStringContainsAllDashes() {
        assertThat(DashVariant.ALL_AS_STRING)
                .contains(Arrays.stream(DashVariant.values())
                        .map(DashVariant::getDash)
                        .toArray(CharSequence[]::new));
    }

    @Test
    public void isDashReturnsTrueForAllDashes() {
        assertThat(Arrays.stream(DashVariant.values())).allSatisfy(variant ->
                assertThat(DashVariant.isDash(variant.getDash()))
                        .as(variant.toString())
                        .isTrue());
    }

    @Test
    public void isDashReturnsFalseForNonDashCharSequences() {
        //noinspection UnnecessaryCallToStringValueOf - ensure null gets translated into string
        Arrays.asList(null, "", " ", "_", "--")
                .forEach(string -> assertThat(DashVariant.isDash(string))
                        .as(Objects.toString(string))
                        .isFalse());
    }
}
