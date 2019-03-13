package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import fi.fmi.avi.converter.tac.lexer.Lexeme;

public final class LexemeUtils {

    private static final List<Lexeme.Identity> TAF_GROUP_DELIMITERS = Arrays.asList(Lexeme.Identity.TAF_START, Lexeme.Identity.TAF_FORECAST_CHANGE_INDICATOR,
            Lexeme.Identity.TREND_CHANGE_INDICATOR, Lexeme.Identity.END_TOKEN);

    public static boolean existsPreviousLexemesWithinSameGroup(final Lexeme token, final Lexeme.Identity identity) {
        return existsPreviousLexemesWithinSameGroup(token, identity, l -> true);
    }

    public static boolean existsPreviousLexemesWithinSameGroup(final Lexeme token, final Lexeme.Identity identity, final Predicate<Lexeme> extraCondition) {
        // Check if there has been another SurfaceWind in the same group
        Lexeme l = token;
        while ((l = l.getPrevious()) != token.getFirst()) {
            if (TAF_GROUP_DELIMITERS.contains(l.getIdentity())) {
                break;
            }
        }

        boolean hasAnotherValue = false;
        while ((l = l.getNext()) != null && !TAF_GROUP_DELIMITERS.contains(l.getIdentity()) && l != token) {
            if (l.getIdentity() == identity && extraCondition.test(l)) {
                hasAnotherValue = true;
            }
        }
        return hasAnotherValue;
    }

}
