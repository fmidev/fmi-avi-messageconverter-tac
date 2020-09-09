package fi.fmi.avi.converter.tac.swx;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Assert;
import org.junit.Test;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.impl.LexingFactoryImpl;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryNumber;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryStatus;
import fi.fmi.avi.converter.tac.lexer.impl.token.NextAdvisory;
import fi.fmi.avi.converter.tac.lexer.impl.token.ReplaceAdvisoryNumber;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.swx.immutable.AdvisoryNumberImpl;

public class SWXTokenTest {

    final String WHITESPACE = " ";

    final String NO_PREVIOUS = "NO_PREVIOUS";
    final String NULL_PREVIOUS = "NULL_PREVIOUS";
    final String WRONG_ID_PREVIOUS = "WRONG_ID_PREVIOUS";
    final String CORRECT_PREVIOUS = "CORRECT_PREVIOUS";

    @Test
    public void advisoryStatusVisitIfMatchedTest() {
        final String LABEL = "STATUS:";
        final String FIELD_VALUE = "TEST";
        final LexemeIdentity LABEL_ID = LexemeIdentity.TEST_OR_EXCERCISE_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new AdvisoryStatus(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);

        Pattern pattern = Pattern.compile("^(?<status>TEST|EXERCISE){1}$");
        Matcher matcher = pattern.matcher(FIELD_VALUE);
        Assert.assertTrue(matcher.matches());

        Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        Assert.assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        Assert.assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        Assert.assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());
        Assert.assertEquals(LexemeIdentity.TEST_OR_EXCERCISE, resultset.get(CORRECT_PREVIOUS).getIdentity());
        Assert.assertEquals(AviationCodeListUser.PermissibleUsageReason.TEST, resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.VALUE,
                AviationCodeListUser.PermissibleUsageReason.class));

    }

    @Test
    public void replaceAdvisoryNumberVisitIfMatchedTest() {
        final String LABEL = "NR RPLC:";
        final String FIELD_VALUE = "2020/15";
        final LexemeIdentity LABEL_ID = LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new ReplaceAdvisoryNumber(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);


        Pattern pattern = Pattern.compile("^(?<advisoryNumber>[\\d]{4}/[\\d]*)$");
        Matcher matcher = pattern.matcher(FIELD_VALUE);
        Assert.assertTrue(matcher.matches());

        Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        Assert.assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        Assert.assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        Assert.assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());
        Assert.assertEquals(LexemeIdentity.REPLACE_ADVISORY_NUMBER, resultset.get(CORRECT_PREVIOUS).getIdentity());
        AdvisoryNumberImpl advisoryNumber = resultset.get(CORRECT_PREVIOUS).getParsedValue(Lexeme.ParsedValueName.VALUE, AdvisoryNumberImpl.class);

        Assert.assertEquals(15, advisoryNumber.getSerialNumber());
        Assert.assertEquals(2020, advisoryNumber.getYear());
    }

    @Test
    public void nextAdvisoryByVisitIfMatchedTest() {
        final String FIELD_VALUE = "WILL BE ISSUED BY 20161108/0700Z";
        Lexeme result = nextAdvisoryVisitIfMatchedTest(FIELD_VALUE);

        Assert.assertEquals(LexemeIdentity.NEXT_ADVISORY, result.getIdentity());
        Assert.assertEquals(new Integer(2016), result.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
        Assert.assertEquals(new Integer(11), result.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
        Assert.assertEquals(new Integer(8), result.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
        Assert.assertEquals(new Integer(7), result.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
        Assert.assertEquals(new Integer(0), result.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));


        Assert.assertEquals(fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_BY, result.getParsedValue(Lexeme.ParsedValueName.TYPE,
                fi.fmi.avi.model.swx.NextAdvisory.Type.class));
    }

    @Test
    public void nextAdvisoryAtVisitIfMatchedTest() {
        final String FIELD_VALUE = "20161108/0700Z";
        Lexeme result = nextAdvisoryVisitIfMatchedTest(FIELD_VALUE);

        Assert.assertEquals(LexemeIdentity.NEXT_ADVISORY, result.getIdentity());
        Assert.assertEquals(new Integer(2016), result.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
        Assert.assertEquals(new Integer(11), result.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
        Assert.assertEquals(new Integer(8), result.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
        Assert.assertEquals(new Integer(7), result.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
        Assert.assertEquals(new Integer(0), result.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));
        Assert.assertEquals(fi.fmi.avi.model.swx.NextAdvisory.Type.NEXT_ADVISORY_AT, result.getParsedValue(Lexeme.ParsedValueName.TYPE,
                fi.fmi.avi.model.swx.NextAdvisory.Type.class));
    }

    @Test
    public void noFurtherAdvisoryVisitIfMatchedTest() {
        final String FIELD_VALUE = "NO FURTHER ADVISORIES";
        Lexeme result = nextAdvisoryVisitIfMatchedTest(FIELD_VALUE);

        Assert.assertEquals(fi.fmi.avi.model.swx.NextAdvisory.Type.NO_FURTHER_ADVISORIES, result.getParsedValue(Lexeme.ParsedValueName.TYPE,
                fi.fmi.avi.model.swx.NextAdvisory.Type.class));
    }


    public Lexeme nextAdvisoryVisitIfMatchedTest(String FIELD_VALUE) {
        final String LABEL = "NXT ADVISORY:";
        final LexemeIdentity LABEL_ID = LexemeIdentity.NEXT_ADVISORY_LABEL;
        final RegexMatchingLexemeVisitor VISITOR = new NextAdvisory(PrioritizedLexemeVisitor.OccurrenceFrequency.AVERAGE);


        Pattern pattern = Pattern.compile("(?<type>WILL\\sBE\\sISSUED\\sBY\\s?)?((?<year>[0-9]{4})(?<month>[0-1][0-9])(?<day>[0-3][0-9])\\/"
                + "(?<hour>[0-2][0-9])(?<minute>[0-5][0-9])Z)|(?<nofurther>NO\\sFURTHER\\sADVISORIES)");
        Matcher matcher = pattern.matcher(FIELD_VALUE);
        Assert.assertTrue(matcher.matches());

        Map<String, Lexeme> resultset = visitIfMatchedTest(LABEL, LABEL_ID, FIELD_VALUE, matcher, VISITOR);

        Assert.assertNull(resultset.get(NO_PREVIOUS).getIdentity());
        Assert.assertNull(resultset.get(NULL_PREVIOUS).getIdentity());
        Assert.assertNull(resultset.get(WRONG_ID_PREVIOUS).getIdentity());

        return resultset.get(CORRECT_PREVIOUS);
    }


    public Map<String, Lexeme> visitIfMatchedTest(String previousToken, LexemeIdentity previousTokenId, String fielValue, Matcher matcher,
            RegexMatchingLexemeVisitor tokenVisitor) {
        Map<String, Lexeme> resultset = new HashMap<>();

        LexingFactoryImpl factory = new LexingFactoryImpl();

        tokenVisitor.visitIfMatched(null, matcher, null);

        LexemeSequenceBuilder noPrevious = factory.createLexemeSequenceBuilder();
        noPrevious.append(factory.createLexeme(fielValue));

        Lexeme tokenNoPrev = noPrevious.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenNoPrev, matcher, null);
        resultset.put(NO_PREVIOUS, tokenNoPrev);

        LexemeSequenceBuilder nullPreviousIdentity = factory.createLexemeSequenceBuilder();
        nullPreviousIdentity.append(factory.createLexeme(previousToken));
        nullPreviousIdentity.append((factory.createLexeme(WHITESPACE, LexemeIdentity.WHITE_SPACE)));
        nullPreviousIdentity.append(factory.createLexeme(fielValue));

        Lexeme tokenNullPrev = nullPreviousIdentity.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenNullPrev, matcher, null);
        resultset.put(NULL_PREVIOUS, tokenNullPrev);

        LexemeSequenceBuilder wrongPreviousIdentity = factory.createLexemeSequenceBuilder();
        wrongPreviousIdentity.append(factory.createLexeme(previousToken, LexemeIdentity.METAR_START));
        wrongPreviousIdentity.append((factory.createLexeme(WHITESPACE, LexemeIdentity.WHITE_SPACE)));
        wrongPreviousIdentity.append(factory.createLexeme(fielValue));

        Lexeme tokenWrongPrev = wrongPreviousIdentity.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenWrongPrev, matcher, null);
        resultset.put(WRONG_ID_PREVIOUS, tokenWrongPrev);

        LexemeSequenceBuilder correctPreviousIdentity = factory.createLexemeSequenceBuilder();
        correctPreviousIdentity.append(factory.createLexeme(previousToken, previousTokenId));
        correctPreviousIdentity.append((factory.createLexeme(WHITESPACE, LexemeIdentity.WHITE_SPACE)));
        correctPreviousIdentity.append(factory.createLexeme(fielValue));

        Lexeme tokenCorrectPrev = correctPreviousIdentity.build().getLastLexeme();
        tokenVisitor.visitIfMatched(tokenCorrectPrev, matcher, null);
        resultset.put(CORRECT_PREVIOUS, tokenCorrectPrev);

        return resultset;
    }
}
