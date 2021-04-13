package fi.fmi.avi.converter.tac.swx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryNumber;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomenaTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryStatus;
import fi.fmi.avi.converter.tac.lexer.impl.token.NextAdvisory;
import fi.fmi.avi.converter.tac.lexer.impl.token.ReplaceAdvisoryNumber;
import fi.fmi.avi.converter.tac.lexer.impl.token.SWXCenter;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.swx.immutable.AdvisoryNumberImpl;

public class SWXTokenTest {

    private static final String WHITESPACE = " ";

    private static final String NO_PREVIOUS = "NO_PREVIOUS";
    private static final String NULL_PREVIOUS = "NULL_PREVIOUS";
    private static final String WRONG_ID_PREVIOUS = "WRONG_ID_PREVIOUS";
    private static final String CORRECT_PREVIOUS = "CORRECT_PREVIOUS";

    @Test
    public void advisoryStatusVisitIfMatchedTest() {
        final String LABEL = "STATUS:";
        final String FIELD_VALUE = "TEST";
        final LexemeIdentity LABEL_ID = LexemeIdentity.ADVISORY_STATUS_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new AdvisoryStatus(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(FIELD_VALUE);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());
        assertEquals(LexemeIdentity.ADVISORY_STATUS, resultset.get(CORRECT_PREVIOUS).getIdentity());
        assertEquals(AviationCodeListUser.PermissibleUsageReason.TEST,
                resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.class));

    }

    @Test
    public void replaceAdvisoryNumberVisitIfMatchedTest() {
        final String LABEL = "NR RPLC:";
        final String FIELD_VALUE = "2020/15";
        final LexemeIdentity LABEL_ID = LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL;
        final ReplaceAdvisoryNumber VISITOR = new ReplaceAdvisoryNumber(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(FIELD_VALUE);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());
        assertEquals(LexemeIdentity.REPLACE_ADVISORY_NUMBER, resultset.get(CORRECT_PREVIOUS).getIdentity());
        final AdvisoryNumberImpl advisoryNumber = resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumberImpl.class);

        assertEquals(15, advisoryNumber.getSerialNumber());
        assertEquals(2020, advisoryNumber.getYear());
    }

    @Test
    public void nextAdvisoryByVisitIfMatchedTest() {
        final String FIELD_VALUE = "WILL BE ISSUED BY 20161108/0700Z";
        final Lexeme result = nextAdvisoryVisitIfMatchedTest(FIELD_VALUE);

        assertEquals(LexemeIdentity.NEXT_ADVISORY, result.getIdentity());
        assertEquals(new Integer(2016), result.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
        assertEquals(new Integer(11), result.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
        assertEquals(new Integer(8), result.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
        assertEquals(new Integer(7), result.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
        assertEquals(new Integer(0), result.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));

        assertEquals(fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_BY,
                result.getParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.class));
    }

    @Test
    public void nextAdvisoryAtVisitIfMatchedTest() {
        final String FIELD_VALUE = "20161108/0700Z";
        final Lexeme result = nextAdvisoryVisitIfMatchedTest(FIELD_VALUE);

        assertEquals(LexemeIdentity.NEXT_ADVISORY, result.getIdentity());
        assertEquals(new Integer(2016), result.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
        assertEquals(new Integer(11), result.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
        assertEquals(new Integer(8), result.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
        assertEquals(new Integer(7), result.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
        assertEquals(new Integer(0), result.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));
        assertEquals(fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_AT,
                result.getParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.class));
    }

    @Test
    public void noFurtherAdvisoryVisitIfMatchedTest() {
        final String FIELD_VALUE = "NO FURTHER ADVISORIES";
        final Lexeme result = nextAdvisoryVisitIfMatchedTest(FIELD_VALUE);

        assertEquals(fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES,
                result.getParsedValue(Lexeme.ParsedValueName.TYPE, fi.fmi.avi.model.swx.NextAdvisory.Type.class));
    }

    public Lexeme nextAdvisoryVisitIfMatchedTest(final String FIELD_VALUE) {
        final String LABEL = "NXT ADVISORY:";
        final LexemeIdentity LABEL_ID = LexemeIdentity.NEXT_ADVISORY_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new NextAdvisory(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(FIELD_VALUE);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());

        return resultset.get(CORRECT_PREVIOUS);
    }

    @Test
    public void advisoryNumberVisitIfMatchedTest() {
        final String LABEL = "ADVISORY NR:";
        final String FIELD_VALUE = "2020/30";
        final LexemeIdentity LABEL_ID = LexemeIdentity.ADVISORY_NUMBER_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new AdvisoryNumber(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(FIELD_VALUE);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        assertEquals(LexemeIdentity.ADVISORY_NUMBER, resultset.get(CORRECT_PREVIOUS).getIdentity());
        final AdvisoryNumberImpl advisoryNumber = resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumberImpl.class);

        assertEquals(30, advisoryNumber.getSerialNumber());
        assertEquals(2020, advisoryNumber.getYear());
    }

    @Test
    public void swxCenterVisitIfMatchedTest() {
        final String LABEL = "SWXC:";
        final String FIELD_VALUE = "DONLON";
        final LexemeIdentity LABEL_ID = LexemeIdentity.SWX_CENTRE_LABEL;
        final SWXCenter VISITOR = new SWXCenter(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(FIELD_VALUE);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());

        resultset.get(CORRECT_PREVIOUS);
    }

    @Test
    public void AdvisoryTimeGroupVisitIfMatchedTest() {
        final String LABEL = "NR RPLC:";
        final String FIELD_VALUE = "08/1254Z";
        final LexemeIdentity LABEL_ID = LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL;
        final AdvisoryPhenomenaTimeGroup VISITOR = new AdvisoryPhenomenaTimeGroup(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        final Pattern pattern = VISITOR.getPattern();
        final Matcher matcher = pattern.matcher(FIELD_VALUE);
        assertTrue(matcher.matches());

        final Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());

        resultset.get(CORRECT_PREVIOUS);
    }

    public Map<String, Lexeme> visitIfMatchedTest(final String previousToken, final LexemeIdentity previousTokenId, final String fielValue,
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
