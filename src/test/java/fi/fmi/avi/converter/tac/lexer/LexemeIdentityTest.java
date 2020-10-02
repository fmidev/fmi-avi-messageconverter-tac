package fi.fmi.avi.converter.tac.lexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.junit.Test;

public class LexemeIdentityTest {
    private static Stream<Field> lexemeIdentityConstantFields() {
        return Arrays.stream(LexemeIdentity.class.getFields())//
                .filter(field -> LexemeIdentity.class.equals(field.getType())//
                        && Modifier.isPublic(field.getModifiers())//
                        && Modifier.isStatic(field.getModifiers())//
                        && Modifier.isFinal(field.getModifiers()));
    }

    private static Stream<LexemeIdentity> lexemeIdentityConstants() {
        return lexemeIdentityConstantFields()//
                .map(field -> {
                    try {
                        return (LexemeIdentity) field.get(null);
                    } catch (final IllegalAccessException e) {
                        return null;
                    }
                })//
                .filter(Objects::nonNull);
    }

    @Test
    public void constantEqualsName() {
        lexemeIdentityConstantFields()//
                .forEach(field -> {
                    try {
                        assertEquals(((LexemeIdentity) field.get(null)).name(), field.getName());
                    } catch (final IllegalAccessException e) {
                        fail(e.getMessage());
                    }
                });
    }

    @Test
    public void labelIdentitiesAreSuffixedLabelByConvention() {
        lexemeIdentityConstants()//
                .forEach(lexemeIdentity -> {
                    final boolean expectSuffix = lexemeIdentity.getIdentityProperties().contains(LexemeIdentity.IdentityProperty.LABEL);
                    assertEquals("Expected <" + lexemeIdentity.name() + "> " + (expectSuffix ? "" : "NOT ") + "to end in <_LABEL>", expectSuffix,
                            lexemeIdentity.name().endsWith("_LABEL"));
                });
    }
}
