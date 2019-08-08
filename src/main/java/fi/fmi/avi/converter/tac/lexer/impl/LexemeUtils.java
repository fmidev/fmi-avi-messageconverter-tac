package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;

public final class LexemeUtils {

    private static final List<LexemeIdentity> TAF_GROUP_DELIMITERS = Arrays.asList(LexemeIdentity.TAF_START, LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR,
            LexemeIdentity.TREND_CHANGE_INDICATOR, LexemeIdentity.END_TOKEN);

    public static boolean existsPreviousLexemesWithinSameGroup(final Lexeme token, final LexemeIdentity identity) {
        return existsPreviousLexemesWithinSameGroup(token, identity, l -> true);
    }

    public static boolean existsPreviousLexemesWithinSameGroup(final Lexeme token, final LexemeIdentity identity, final Predicate<Lexeme> extraCondition) {
        Lexeme l = token;
        while ((l = l.getPrevious()) != token.getFirst()) {
            if (TAF_GROUP_DELIMITERS.contains(l.getIdentity())) {
                break;
            }
        }

        boolean hasAnotherValue = false;
        LexemeIdentity i;
        while ((l = l.getNext()) != null && !TAF_GROUP_DELIMITERS.contains(l.getIdentity()) && l != token) {
            i = l.getIdentity();
            if (i != null && i.equals(identity) && extraCondition.test(l)) {
                hasAnotherValue = true;
            }
        }
        return hasAnotherValue;
    }

}
