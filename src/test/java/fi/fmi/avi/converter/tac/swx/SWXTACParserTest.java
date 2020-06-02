package fi.fmi.avi.converter.tac.swx;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.TACTestConfiguration;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomena;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomenaTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.DTGIssueTime;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = TACTestConfiguration.class, loader = AnnotationConfigContextLoader.class)

public class SWXTACParserTest {

    @Autowired
    private LexingFactory lexingFactory;

    private LexemeSequence getLexedSWXMessage() {
        /*
         + "SWX ADVISORY\n" //
                        + "STATUS: TEST\n"//
                        + "DTG: 20190128/1200Z\n" //
                        + "SWXC: PECASUS\n" //
                        + "ADVISORY NR: 2019/1\n"//
                        + "SWX EFFECT: SATCOM MOD\n" //
                        + "OBS SWX: 08/1200Z HNH HSH E18000 - W18000 ABV FL340\n"//
                        + "FCST SWX +6 HR: 08/1800Z NO SWX EXP\n"//
                        + "FCST SWX +12 HR: 09/0000Z NO SWX EXP\n"//
                        + "FCST SWX +18 HR: 09/0600Z DAYLIGHT SIDE\n"//
                        + "FCST SWX +24 HR: 09/1200Z NO SWX EXP\n"//
                        + "RMK: TEST TEST TEST TEST\n"
                        + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" + "SEE WWW.PECASUS.ORG \n"
                        + "NXT ADVISORY: NO FURTHER ADVISORIES\n \n",
         */
        final ConversionHints hints = ConversionHints.EMPTY;

        final LexemeSequenceBuilder builder = this.lexingFactory.createLexemeSequenceBuilder()
                .append(this.lexingFactory.createLexeme("SWX ADVISORY", LexemeIdentity.SPACE_WEATHER_ADVISORY_START))//
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));
        final AdvisoryPhenomena ap = new AdvisoryPhenomena(PrioritizedLexemeVisitor.Priority.HIGH);
        final AdvisoryPhenomenaTimeGroup aptg = new AdvisoryPhenomenaTimeGroup(PrioritizedLexemeVisitor.Priority.HIGH);
        final DTGIssueTime dtg = new DTGIssueTime(PrioritizedLexemeVisitor.Priority.HIGH);

        builder.append(this.lexingFactory.createLexeme("DTG: 20190128/1200Z"));
        dtg.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: SWX Center
                .append(this.lexingFactory.createLexeme("SWXC: PECASUS"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: Advisory number
                .append(this.lexingFactory.createLexeme("ADVISORY NR: 2019/1"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: SWX effects/phenomena
                .append(this.lexingFactory.createLexeme("SWX EFFECT:"))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("SATCOM MOD"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));
        builder.append(this.lexingFactory.createLexeme("OBS SWX:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("08/1200Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                //TODO: region data
                .append(this.lexingFactory.createLexeme("HNH"))
                .append(this.lexingFactory.createLexeme("HSH"))
                .append(this.lexingFactory.createLexeme("E18000 - W18000"))
                .append(this.lexingFactory.createLexeme("ABV"))
                .append(this.lexingFactory.createLexeme("FL340"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("FCST SWX +6 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("08/1800Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("NO SWX EXP"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("FCST SWX +12 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("09/0000Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("NO SWX EXP"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("FCST SWX +18 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("09/0600Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("DAYLIGHT SIDE"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("FCST SWX +24 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.lexingFactory.createLexeme("09/1200Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("NO SWX EXP"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: "RMK:" remarks
                .append(this.lexingFactory.createLexeme("RMK:", LexemeIdentity.REMARKS_START))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("THIS", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("IS", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("A", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("MESSAGE", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("FOR", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("TECHNICAL", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("TEST.", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("SEE", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("WWW.PECASUS.ORG", LexemeIdentity.REMARK))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                .append(this.lexingFactory.createLexeme("NXT ADVISORY: NO FURTHER ADVISORIES"))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.lexingFactory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        return builder.build();
    }

    @Test
    public void test123() {
        LexemeSequence lexed = getLexedSWXMessage();
        assertEquals(LexemeIdentity.SPACE_WEATHER_ADVISORY_START, lexed.getFirstLexeme().getIdentityIfAcceptable());
        lexed.getFirstLexeme().findNext(LexemeIdentity.ISSUE_TIME, (issueTime) -> {
            assertEquals(Integer.valueOf(2019), issueTime.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class));
            assertEquals(Integer.valueOf(1), issueTime.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class));
            assertEquals(Integer.valueOf(28), issueTime.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class));
            assertEquals(Integer.valueOf(12), issueTime.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class));
            assertEquals(Integer.valueOf(0), issueTime.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class));
        });
    }

}
