package fi.fmi.avi.converter.tac.swx;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomena;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomenaTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.DTGIssueTime;
import fi.fmi.avi.model.MessageType;

public class DummySWXLexer implements AviMessageLexer {

    private LexingFactory factory;

    public void setLexingFactory(final LexingFactory factory) {
        this.factory = factory;
    }

    public LexingFactory getLexingFactory() {
        return this.factory;
    }

    @Override
    public LexemeSequence lexMessage(final String input) {
        return lexMessage(input, ConversionHints.EMPTY);
    }

    /**
     * Always returns a dummy SWX LexemeSequence regardless of the input. Use only for developing the SWX parser before the actual lexing code for SWX is
     * available.
     *
     * @param input the TAC encoded message
     * @param hints parsing hints to be passed to the lexer implementation
     * @return
     */
    @Override
    public LexemeSequence lexMessage(final String input, final ConversionHints hints) {
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
        final AdvisoryPhenomena ap = new AdvisoryPhenomena(PrioritizedLexemeVisitor.Priority.HIGH);
        final AdvisoryPhenomenaTimeGroup aptg = new AdvisoryPhenomenaTimeGroup(PrioritizedLexemeVisitor.Priority.HIGH);
        final DTGIssueTime dtg = new DTGIssueTime(PrioritizedLexemeVisitor.Priority.HIGH);

        final LexemeSequenceBuilder builder = this.factory.createLexemeSequenceBuilder()
                .append(this.factory.createLexeme("SWX ADVISORY", LexemeIdentity.SPACE_WEATHER_ADVISORY_START))//
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        //TODO: Status
        builder.append(this.factory.createLexeme("STATUS: TEST")).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("DTG: 20190128/1200Z"));
        dtg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: SWX Center
                .append(this.factory.createLexeme("SWXC: PECASUS"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: Advisory number
                .append(this.factory.createLexeme("ADVISORY NR: 2019/1"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: SWX effects/phenomena
                .append(this.factory.createLexeme("SWX EFFECT:"))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("SATCOM MOD"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));
        builder.append(this.factory.createLexeme("OBS SWX:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("08/1200Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                //TODO: region data
                .append(this.factory.createLexeme("HNH"))
                .append(this.factory.createLexeme("HSH"))
                .append(this.factory.createLexeme("E18000 - W18000"))
                .append(this.factory.createLexeme("ABV"))
                .append(this.factory.createLexeme("FL340"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +6 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("08/1800Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("NO SWX EXP"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +12 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("09/0000Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("NO SWX EXP"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +18 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("09/0600Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("DAYLIGHT SIDE"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +24 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("09/1200Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("NO SWX EXP"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                //TODO: "RMK:" remarks
                .append(this.factory.createLexeme("RMK:", LexemeIdentity.REMARKS_START))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("THIS", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("IS", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("A", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("TEST", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("MESSAGE", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("FOR", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("TECHNICAL", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("TEST.", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("SEE", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("WWW.PECASUS.ORG", LexemeIdentity.REMARK))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))

                .append(this.factory.createLexeme("NXT ADVISORY: NO FURTHER ADVISORIES"))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE))
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        return builder.build();
    }

    @Override
    public Optional<MessageType> recognizeMessageType(final String input, final ConversionHints hints) {
        return Optional.of(MessageType.SPACE_WEATHER_ADVISORY);
    }
}
