package fi.fmi.avi.converter.tac.lexer;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Generic tests for the TAC Lexer implementation
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)
public class LexerTest {

    private static final String SWX_ADVISORY = "SWX ADVISORY\n" //
            + "STATUS: TEST\n"//
            + "DTG: 20190128/1200Z\n" //
            + "SWXC: PECASUS\n" //
            + "ADVISORY NR: 2019/1\n"//
            + "SWX EFFECT: SATCOM MOD AND RADIATION SEV AND HF COM SEV\n" //
            + "OBS SWX: 08/1200Z HNH HSH E16000 - W2000 ABV FL340\n"//
            + "FCST SWX +6 HR: 08/1800Z N80 W180 - N70 W75 - N60 E15 - N70 E75 - N80 W180 ABV FL370\n"//
            + "FCST SWX +12 HR: 09/0000Z NO SWX EXP FL150-250\n" //
            + "FCST SWX +12 HR: 09/0000Z NOT AVBL\n"//
            + "FCST SWX +18 HR: 09/0600Z DAYLIGHT SIDE\n"//
            + "FCST SWX +24 HR: 09/1200Z HNH\n"//
            + "RMK: TEST TEST TEST TEST\n" //
            + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" //
            + "SEE WWW.PECASUS.ORG \n" //
            + "NXT ADVISORY: WILL BE ISSUED BY 20161108/0700Z\n" //
            + "NXT ADVISORY: 20161108/0700Z\n" //
            + "NXT ADVISORY: NO FURTHER ADVISORIES\n";

    @Autowired
    private AviMessageLexer lexer;

    @Test
    public void testSplit() {
        final LexemeSequence seq = lexer.lexMessage("TAF EFHK 011733Z 0118/0218 VRB02KT 4000 -SN BKN003\n" //
                + "TEMPO 0118/0120 1500 SN \n" //
                + "BECMG 0120/0122 1500 BR \t\n" //
                + "PROB40 TEMPO 0122/0203 0700 FG\n" //
                + "BECMG 0204/0206 21010KT 5000 BKN005\n" //
                + "BECMG 0210/0212 9999 BKN010=");

        List<LexemeSequence> splitUp = seq.splitBy(LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR);

        assertThat(splitUp.size())
                .as("Incorrect number of split sequences")
                .isEqualTo(6);
        assertThat(splitUp.get(0).getTAC())
                .as("First split-up sequence does not match")
                .isEqualTo("TAF EFHK 011733Z 0118/0218 VRB02KT 4000 -SN BKN003\n");
        assertThat(splitUp.get(1).getTAC())
                .as("Second split-up sequence does not match")
                .isEqualTo("TEMPO 0118/0120 1500 SN \n");
        assertThat(splitUp.get(2).getTAC())
                .as("Third split-up sequence does not match")
                .isEqualTo("BECMG 0120/0122 1500 BR \t\n");
        assertThat(splitUp.get(3).getTAC())
                .as("Fourth split-up sequence does not match")
                .isEqualTo("PROB40 TEMPO 0122/0203 0700 FG\n");
        assertThat(splitUp.get(4).getTAC())
                .as("Fifth split-up sequence does not match")
                .isEqualTo("BECMG 0204/0206 21010KT 5000 BKN005\n");
        assertThat(splitUp.get(5).getTAC())
                .as("Sixth split-up sequence does not match")
                .isEqualTo("BECMG 0210/0212 9999 BKN010=");

        splitUp = seq.splitBy(false, LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR);
        assertThat(splitUp.size())
                .as("Incorrect number of split sequences")
                .isEqualTo(6);
        assertThat(splitUp.get(0).trimWhiteSpace().getTAC())
                .as("First split-up sequence does not match")
                .isEqualTo("TAF EFHK 011733Z 0118/0218 VRB02KT 4000 -SN BKN003\nTEMPO");
        assertThat(splitUp.get(1).trimWhiteSpace().getTAC())
                .as("Second split-up sequence does not match")
                .isEqualTo("0118/0120 1500 SN \nBECMG");
        assertThat(splitUp.get(2).trimWhiteSpace().getTAC())
                .as("Third split-up sequence does not match")
                .isEqualTo("0120/0122 1500 BR \t\nPROB40 TEMPO");
        assertThat(splitUp.get(3).trimWhiteSpace().getTAC())
                .as("Fourth split-up sequence does not match")
                .isEqualTo("0122/0203 0700 FG\nBECMG");
        assertThat(splitUp.get(4).trimWhiteSpace().getTAC())
                .as("Fifth split-up sequence does not match")
                .isEqualTo("0204/0206 21010KT 5000 BKN005\nBECMG");
        assertThat(splitUp.get(5).trimWhiteSpace().getTAC())
                .as("Sixth split-up sequence does not match")
                .isEqualTo("0210/0212 9999 BKN010=");

    }

    @Test
    public void testBasicLexing() {
        final String original = "TAF EFHK 011733Z 0118/0218 VRB02KT 4000 -SN BKN003\n" //
                + "TEMPO 0118/0120 1500 SN\n" //
                + "BECMG 0120/0122 1500 BR\n" //
                + "PROB40 TEMPO 0122/0203 0700 FG\n" //
                + "BECMG 0204/0206 21010KT 5000 BKN005\n" //
                + "BECMG 0210/0212 9999 BKN010=";
        final LexemeSequence seq = lexer.lexMessage(original);
        assertThat(58).isEqualTo(seq.getLexemes().size());
        final String back2tac = seq.getTAC();
        assertThat(original).isEqualTo(back2tac);
    }

    @Test
    public void testIteratorWithWhiteSpaceEnding() {
        final LexemeSequence seq = lexer.lexMessage("TAF EFHK 011733Z 0118/0218 VRB02KT 4000 -SN BKN003\n" //
                + "TEMPO 0118/0120 1500 SN \n" //
                + "BECMG 0120/0122 1500 BR \t\n" //
                + "PROB40 TEMPO 0122/0203 0700 FG\n" //
                + "BECMG 0204/0206 21010KT 5000 BKN005\n" //
                + "BECMG 0210/0212 9999 BKN010=");

        final List<LexemeSequence> splitUp = seq.splitBy(LexemeIdentity.TAF_FORECAST_CHANGE_INDICATOR);
        Lexeme l = splitUp.get(0).getFirstLexeme();
        while (!l.getTACToken().equals("BKN003")) {
            l = l.getNext();
        }
        assertThat(l.hasNext()).isFalse();
        assertThat(l.getNext()).isNull();

        assertThat(l.hasNext(true)).isTrue();
        assertThat(l.getNext(true)).isNotNull();
    }

    @Test
    public void testMultipleWhitespacesBetweenCombinableLexemes() {
        final LexemeSequence result = lexer.lexMessage("SWX ADVISORY\n" //
                + "FCST SWX +6 HR: 08/1800Z N80 W180 -\n N70\n W75=");
        assertThat(result.getLexemes())
                .extracting(Lexeme::getIdentity)
                .containsExactlyInAnyOrder(
                        LexemeIdentity.SPACE_WEATHER_ADVISORY_START,
                        LexemeIdentity.WHITE_SPACE,
                        LexemeIdentity.ADVISORY_PHENOMENA_LABEL,
                        LexemeIdentity.WHITE_SPACE,
                        LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP,
                        LexemeIdentity.WHITE_SPACE,
                        LexemeIdentity.POLYGON_COORDINATE_PAIR,
                        LexemeIdentity.WHITE_SPACE,
                        LexemeIdentity.POLYGON_COORDINATE_PAIR_SEPARATOR,
                        LexemeIdentity.WHITE_SPACE,
                        LexemeIdentity.WHITE_SPACE,
                        LexemeIdentity.POLYGON_COORDINATE_PAIR,
                        LexemeIdentity.END_TOKEN);
    }

    @Test
    public void testSpaceWeatherLexing() {
        final List<Lexeme> unidentifiedLexemes = new ArrayList<>();

        final LexemeSequence seq = lexer.lexMessage(SWX_ADVISORY);
        Lexeme le = seq.getFirstLexeme();

        while (le != null) {
            if (le.getIdentity() == null) {
                unidentifiedLexemes.add(le);
            }
            le = le.getNext();
        }

        if (!unidentifiedLexemes.isEmpty()) {
            System.out.println("Unidentified lexemes: ");
            unidentifiedLexemes.forEach(lexeme -> System.out.println(lexeme.getTACToken()));
            fail("There were unidentified lexemes");
        }
    }

    @Test
    public void testSpaceWeatherLexingWithConversionHint() {
        final List<Lexeme> unidentifiedLexemes = new ArrayList<>();

        final LexemeSequence seq = lexer.lexMessage(SWX_ADVISORY, ConversionHints.SPACE_WEATHER_ADVISORY);
        Lexeme le = seq.getFirstLexeme();

        while (le != null) {
            if (le.getIdentity() == null) {
                unidentifiedLexemes.add(le);
            }
            le = le.getNext();
        }

        if (!unidentifiedLexemes.isEmpty()) {
            System.out.println("Unidentified lexemes: ");
            unidentifiedLexemes.forEach(lexeme -> System.out.println(lexeme.getTACToken()));
            fail("There were unidentified lexemes");
        }
    }
}
