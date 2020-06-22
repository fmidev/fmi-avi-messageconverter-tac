package fi.fmi.avi.converter.tac.swx;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryNumber;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomena;
import fi.fmi.avi.converter.tac.lexer.impl.token.AdvisoryPhenomenaTimeGroup;
import fi.fmi.avi.converter.tac.lexer.impl.token.DTGIssueTime;
import fi.fmi.avi.converter.tac.lexer.impl.token.SpaceWeatherCenter;
import fi.fmi.avi.converter.tac.lexer.impl.token.SpaceWeatherEffect;
import fi.fmi.avi.converter.tac.lexer.impl.token.SpaceWeatherPresetLocation;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.immutable.PolygonGeometryImpl;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

public class DummySWXLexer implements AviMessageLexer {

    private LexingFactory factory;

    public LexingFactory getLexingFactory() {
        return this.factory;
    }

    public void setLexingFactory(final LexingFactory factory) {
        this.factory = factory;
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
                        + "SWX EFFECT: SATCOM MOD AND RADIATION SEV\n" //
                        + "OBS SWX: 08/1200Z HNH HSH E16000 - W2000 ABV FL340\n"//
                        + "FCST SWX +6 HR: 08/1800Z N80 W180 - N70 W75 - N60 E15 - N70 E75 - N80 W180 ABV FL370\n"//
                        + "FCST SWX +12 HR: 09/0000Z NO SWX EXP\n"//
                        + "FCST SWX +18 HR: 09/0600Z DAYLIGHT SIDE\n"//
                        + "FCST SWX +24 HR: 09/1200Z HNH\n"//
                        + "RMK: TEST TEST TEST TEST\n"
                        + "THIS IS A TEST MESSAGE FOR TECHNICAL TEST.\n" + "SEE WWW.PECASUS.ORG \n"
                        + "NXT ADVISORY: WILL BE ISSUED BY 20161108/0700Z\n \n",
         */
        final AdvisoryPhenomena ap = new AdvisoryPhenomena(PrioritizedLexemeVisitor.Priority.HIGH);
        final AdvisoryPhenomenaTimeGroup aptg = new AdvisoryPhenomenaTimeGroup(PrioritizedLexemeVisitor.Priority.HIGH);
        final DTGIssueTime dtg = new DTGIssueTime(PrioritizedLexemeVisitor.Priority.HIGH);

        Lexeme l;

        final LexemeSequenceBuilder builder = this.factory.createLexemeSequenceBuilder()
                .append(this.factory.createLexeme("SWX ADVISORY", LexemeIdentity.SPACE_WEATHER_ADVISORY_START))//
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("STATUS: TEST");
        l.identify(LexemeIdentity.TEST_OR_EXCERCISE);
        l.setParsedValue(Lexeme.ParsedValueName.VALUE, AviationCodeListUser.PermissibleUsageReason.TEST);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("DTG: 20190128/1200Z"));
        dtg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("SWXC: PECASUS");
        new SpaceWeatherCenter(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, null);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("ADVISORY NR: 2019/1");
        new AdvisoryNumber(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, null);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("SWX EFFECT:");
        l.identify(LexemeIdentity.SWX_EFFECT_LABEL);
        builder.append(l).append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("SATCOM MOD");
        new SpaceWeatherEffect(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, null);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("AND");
        l.identify(LexemeIdentity.SWX_EFFECT_CATENATION);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("RADIATION SEV");
        new SpaceWeatherEffect(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, null);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("OBS SWX:"));
        ap.visit(builder.getLast().get(), hints);
        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("08/1200Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("HNH");
        new SpaceWeatherPresetLocation(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, null);
        builder.append(l).append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("HSH");
        new SpaceWeatherPresetLocation(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, null);
        builder.append(l).append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("E16000 - W2000");
        l.identify(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        l.setParsedValue(Lexeme.ParsedValueName.MIN_VALUE, -160.0);
        l.setParsedValue(Lexeme.ParsedValueName.MAX_VALUE, 20.0);
        builder.append(l).append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("ABV FL340");
        l.identify(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
        l.setParsedValue(Lexeme.ParsedValueName.VALUE, 340);
        l.setParsedValue(Lexeme.ParsedValueName.UNIT, "FL");
        l.setParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.ABOVE);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +6 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("08/1800Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
        l = this.factory.createLexeme("N80 W180 - N70 W75 - N60 E15 - N70 E75 - N80 W180");
        l.identify(LexemeIdentity.SWX_PHENOMENON_POLYGON_LIMIT);
        l.setParsedValue(Lexeme.ParsedValueName.VALUE, PolygonGeometryImpl.builder()//
                .setSrsName("http://www.opengis.net/def/crs/EPSG/0/4326")//
                .addExteriorRingPositions(-80d, -180d, -70d, -75d, -60d, 15d, -70d, 75d, -80d, -180d).build());
        builder.append(l).append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        l = this.factory.createLexeme("ABV FL370");
        l.identify(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);
        l.setParsedValue(Lexeme.ParsedValueName.VALUE, 370);
        l.setParsedValue(Lexeme.ParsedValueName.UNIT, "FL");
        l.setParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.ABOVE);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +12 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("09/0000Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
        l = this.factory.createLexeme("NO SWX EXP");
        l.identify(LexemeIdentity.NO_SWX_EXPECTED);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +18 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("09/0600Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
        l = this.factory.createLexeme("DAYLIGHT SIDE");
        l.identify(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
        l.setParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.DAYLIGHT_SIDE);
        builder.append(l).append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("FCST SWX +24 HR:"));
        ap.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));

        builder.append(this.factory.createLexeme("09/1200Z"));
        aptg.visit(builder.getLast().get(), hints);

        builder.append(this.factory.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
        l = this.factory.createLexeme("HNH");
        new SpaceWeatherPresetLocation(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, null);
        builder.append(l)
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE))
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
                .append(this.factory.createLexeme("\n", LexemeIdentity.WHITE_SPACE));
        l = this.factory.createLexeme("NXT ADVISORY: WILL BE ISSUED BY 20161108/0700Z");
        //l = this.factory.createLexeme("NXT ADVISORY: NO FURTHER ADVISORIES");
        new fi.fmi.avi.converter.tac.lexer.impl.token.NextAdvisory(PrioritizedLexemeVisitor.Priority.HIGH).visit(l, hints);
        builder.append(l)
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
