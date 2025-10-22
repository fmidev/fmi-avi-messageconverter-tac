package fi.fmi.avi.converter.tac.swx.amd79;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.token.*;
import fi.fmi.avi.model.AviationCodeListUser;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

public class SWXTokenTest {

    private static final String WHITESPACE = " ";

    private static final String NO_PREVIOUS = "NO_PREVIOUS";
    private static final String NULL_PREVIOUS = "NULL_PREVIOUS";
    private static final String WRONG_ID_PREVIOUS = "WRONG_ID_PREVIOUS";
    private static final String CORRECT_PREVIOUS = "CORRECT_PREVIOUS";

    @Test
    public void advisoryStatusVisitIfMatchedTest() {
        final String label = "STATUS:";
        final String fieldValue = "TEST";
        final LexemeIdentity LABEL_ID = LexemeIdentity.ADVISORY_STATUS_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new AdvisoryStatus(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(fieldValue);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(label, LABEL_ID, fieldValue, matcher, VISITOR);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());
        assertEquals(LexemeIdentity.ADVISORY_STATUS, resultset.get(CORRECT_PREVIOUS).getIdentity());
        assertEquals(AviationCodeListUser.PermissibleUsageReason.TEST,
                resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class));

    }

    @Test
    public void replaceAdvisoryNumberVisitIfMatchedTest() {
        final String label = "NR RPLC:";
        final String fieldValue = "2020/15";
        final LexemeIdentity LABEL_ID = LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL;
        final ReplaceAdvisoryNumber VISITOR = new ReplaceAdvisoryNumber(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(fieldValue);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(label, LABEL_ID, fieldValue, matcher, VISITOR);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());
        assertEquals(LexemeIdentity.REPLACE_ADVISORY_NUMBER, resultset.get(CORRECT_PREVIOUS).getIdentity());
        final int advisoryYear = resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class);
        final int advisorySerialNumber = resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, Integer.class);

        assertEquals(2020, advisoryYear);
        assertEquals(15, advisorySerialNumber);
    }

    @Test
    public void nextAdvisoryByVisitIfMatchedTest() {
        final String fieldValue = "WILL BE ISSUED BY 20161108/0700Z";
        final Lexeme result = nextAdvisoryVisitIfMatchedTest(fieldValue);

        assertEquals(LexemeIdentity.NEXT_ADVISORY, result.getIdentity());
        assertEquals(Integer.valueOf(2016), result.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
        assertEquals(Integer.valueOf(11), result.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
        assertEquals(Integer.valueOf(8), result.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
        assertEquals(Integer.valueOf(7), result.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
        assertEquals(Integer.valueOf(0), result.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));

        assertEquals(NextAdvisory.Type.WILL_BE_ISSUED_BY,
                result.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));
    }

    @Test
    public void nextAdvisoryAtVisitIfMatchedTest() {
        final String fieldValue = "20161108/0700Z";
        final Lexeme result = nextAdvisoryVisitIfMatchedTest(fieldValue);

        assertEquals(LexemeIdentity.NEXT_ADVISORY, result.getIdentity());
        assertEquals(Integer.valueOf(2016), result.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
        assertEquals(Integer.valueOf(11), result.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
        assertEquals(Integer.valueOf(8), result.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
        assertEquals(Integer.valueOf(7), result.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
        assertEquals(Integer.valueOf(0), result.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));
        assertEquals(NextAdvisory.Type.AT,
                result.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));
    }

    @Test
    public void noFurtherAdvisoryVisitIfMatchedTest() {
        final String fieldValue = "NO FURTHER ADVISORIES";
        final Lexeme result = nextAdvisoryVisitIfMatchedTest(fieldValue);

        assertEquals(NextAdvisory.Type.NO_FURTHER_ADVISORIES,
                result.getParsedValue(Lexeme.ParsedValueName.TYPE, NextAdvisory.Type.class));
    }

    public Lexeme nextAdvisoryVisitIfMatchedTest(final String FIELD_VALUE) {
        final String label = "NXT ADVISORY:";
        final LexemeIdentity labelId = LexemeIdentity.NEXT_ADVISORY_LABEL;
        final RegexMatchingLexemeVisitor visitor = new NextAdvisory(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = visitor.getPattern();
        final Matcher matcher = pattern.matcher(FIELD_VALUE);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(label, labelId, FIELD_VALUE, matcher, visitor);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());

        return resultset.get(CORRECT_PREVIOUS);
    }

    @Test
    public void advisoryNumberVisitIfMatchedTest() {
        final String label = "ADVISORY NR:";
        final String fieldValue = "2020/30";
        final LexemeIdentity labelId = LexemeIdentity.ADVISORY_NUMBER_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new AdvisoryNumber(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(fieldValue);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(label, labelId, fieldValue, matcher, VISITOR);

        assertEquals(LexemeIdentity.ADVISORY_NUMBER, resultset.get(CORRECT_PREVIOUS).getIdentity());
        final int advisoryYear = resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class);
        final int advisorySerialNumber = resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, Integer.class);

        assertEquals(2020, advisoryYear);
        assertEquals(30, advisorySerialNumber);
    }

    @Test
    public void swxCenterVisitIfMatchedTest() {
        final String label = "SWXC:";
        final String fieldValue = "DONLON";
        final LexemeIdentity labelId = LexemeIdentity.SWX_CENTRE_LABEL;
        final SWXCenter visitor = new SWXCenter(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = visitor.getPattern();
        final Matcher matcher = pattern.matcher(fieldValue);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(label, labelId, fieldValue, matcher, visitor);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());

        assertEquals(LexemeIdentity.SWX_CENTRE, resultset.get(CORRECT_PREVIOUS).getIdentity());
    }

    @Test
    public void advisoryTimeGroupVisitIfMatchedTest() {
        final String label = "NR RPLC:";
        final String fieldValue = "08/1254Z";
        final LexemeIdentity labelId = LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL;
        final AdvisoryPhenomenaTimeGroup visitor = new AdvisoryPhenomenaTimeGroup(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = visitor.getPattern();
        final Matcher matcher = pattern.matcher(fieldValue);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(label, labelId, fieldValue, matcher, visitor);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());

        // FIXME: What's the intent?
        //        resultset.get(CORRECT_PREVIOUS);
    }

    public Map<String, Lexeme> visitIfMatchedTest(
            final String previousToken, final LexemeIdentity previousTokenId, final String fielValue,
            final Matcher matcher, final RegexMatchingLexemeVisitor tokenVisitor) {
        final Map<String, Lexeme> resultset = new HashMap<>();

        final LexingFactoryImpl factory = new LexingFactoryImpl();

        tokenVisitor.visitIfMatched(null, matcher, null);

        final LexemeSequenceBuilder noPrevious = factory.createLexemeSequenceBuilder();
        noPrevious.append(factory.createLexeme(fielValue));

        final Lexeme tokenNoPrev = noPrevious.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenNoPrev, matcher, null);
        resultset.put(NO_PREVIOUS, tokenNoPrev);

        final LexemeSequenceBuilder nullPreviousIdentity = factory.createLexemeSequenceBuilder();
        nullPreviousIdentity.append(factory.createLexeme(previousToken));
        nullPreviousIdentity.append((factory.createLexeme(WHITESPACE, LexemeIdentity.WHITE_SPACE)));
        nullPreviousIdentity.append(factory.createLexeme(fielValue));

        final Lexeme tokenNullPrev = nullPreviousIdentity.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenNullPrev, matcher, null);
        resultset.put(NULL_PREVIOUS, tokenNullPrev);

        final LexemeSequenceBuilder wrongPreviousIdentity = factory.createLexemeSequenceBuilder();
        wrongPreviousIdentity.append(factory.createLexeme(previousToken, LexemeIdentity.METAR_START));
        wrongPreviousIdentity.append((factory.createLexeme(WHITESPACE, LexemeIdentity.WHITE_SPACE)));
        wrongPreviousIdentity.append(factory.createLexeme(fielValue));

        final Lexeme tokenWrongPrev = wrongPreviousIdentity.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenWrongPrev, matcher, null);
        resultset.put(WRONG_ID_PREVIOUS, tokenWrongPrev);

        final LexemeSequenceBuilder correctPreviousIdentity = factory.createLexemeSequenceBuilder();
        correctPreviousIdentity.append(factory.createLexeme(previousToken, previousTokenId));
        correctPreviousIdentity.append((factory.createLexeme(WHITESPACE, LexemeIdentity.WHITE_SPACE)));
        correctPreviousIdentity.append(factory.createLexeme(fielValue));

        final Lexeme tokenCorrectPrev = correctPreviousIdentity.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenCorrectPrev, matcher, null);
        resultset.put(CORRECT_PREVIOUS, tokenCorrectPrev);

        return resultset;
    }
}
