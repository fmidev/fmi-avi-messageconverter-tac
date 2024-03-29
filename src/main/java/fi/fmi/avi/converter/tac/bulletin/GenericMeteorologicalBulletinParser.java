package fi.fmi.avi.converter.tac.bulletin;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.bulletin.immutable.GenericMeteorologicalBulletinImpl;
import fi.fmi.avi.util.GTSExchangeFileInfo;

public class GenericMeteorologicalBulletinParser extends AbstractTACParser<GenericMeteorologicalBulletin> {
    private static final LexemeIdentity[] ZERO_OR_ONE_ALLOWED = { LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS,
            LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR };

    private AviMessageLexer lexer;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Converts a single message.
     *
     * @param input input message
     * @param hints parsing hints
     * @return the {@link ConversionResult} with the converter message and the possible conversion issues
     */
    @Override
    public ConversionResult<GenericMeteorologicalBulletin> convertMessage(final String input, final ConversionHints hints) {
        final ConversionResult<GenericMeteorologicalBulletin> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        final LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS != lexed.getFirstLexeme().getIdentityIfAcceptable()//
                || !lexed.getFirstLexeme().hasNext()//
                || LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR != lexed.getFirstLexeme().getNext().getIdentityIfAcceptable()//
                || !lexed.getFirstLexeme().getNext().hasNext()//
                || LexemeIdentity.ISSUE_TIME != lexed.getFirstLexeme().getNext().getNext().getIdentityIfAcceptable()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as Bulletin"));
            return result;
        }

        final List<ConversionIssue> issues = checkZeroOrOne(lexed, ZERO_OR_ONE_ALLOWED);
        if (!issues.isEmpty()) {
            result.addIssue(issues);
            return result;
        }

        final GenericMeteorologicalBulletinImpl.Builder bulletinBuilder = GenericMeteorologicalBulletinImpl.builder();

        //Split & filter in the sequences ending with END_TOKEN, will always return at least one sequence (the original), unless the original is empty:
        final List<LexemeSequence> subSequences = lexed.splitBy(false, LexemeIdentity.END_TOKEN);

        final StringBuilder abbrHeading = new StringBuilder();
        Lexeme l = subSequences.get(0).getFirstLexeme(); // we have already checked that this is the data designators token
        abbrHeading.append(l.getTACToken());
        l = l.findNext(LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, d -> abbrHeading.append(d.getTACToken()));
        l = l.findNext(LexemeIdentity.ISSUE_TIME, time -> abbrHeading.append(time.getTACToken()));
        Lexeme lastHeadingToken = l.findNext(LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR, bbb -> abbrHeading.append(bbb.getTACToken()));
        if (lastHeadingToken == null) {
            lastHeadingToken = l;
        }
        final BulletinHeading bulletinHeading = BulletinHeadingImpl.Builder.from(abbrHeading.toString()).build();
        bulletinBuilder.setHeading(bulletinHeading);

        //Lex each the contained message again individually to collect more info:
        String msg;

        GTSExchangeFileInfo bulletinMetadata = null;
        if (hints != null && hints.containsKey(ConversionHints.KEY_BULLETIN_ID)) {
            String bulletinID = hints.get(ConversionHints.KEY_BULLETIN_ID, String.class);
            //the bulletinID could be used to provide full (or more complete message time info:
            /*
            try {
                bulletinMetadata = GTSExchangeFileInfo.Builder.from(bulletinID).build();
            } catch (IllegalArgumentException iae) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX,
                        "Could not parse bulletin metadata " + "from bulletinID '" + bulletinID + "'"));
            }
            */
        }
        final ConversionHints messageSpecificHints = new ConversionHints(hints);
        messageSpecificHints.put(ConversionHints.KEY_BULLETING_HEADING, bulletinHeading);
        for (int i = 0; i < subSequences.size(); i++) {
            if (i == 0) {
                msg = lastHeadingToken.getTailSequence().trimWhiteSpace().getTAC();
            } else {
                msg = subSequences.get(i).trimWhiteSpace().getTAC();
            }

            GenericAviationWeatherMessageParser parser = new GenericAviationWeatherMessageParser(lexer);
            GenericAviationWeatherMessage weatherMessage = parser.getGenericAvaitionWeatherMessage(msg,
                    messageSpecificHints, issues);
            if(weatherMessage != null) {
                bulletinBuilder.addMessages(weatherMessage);
            }
        }
        result.addIssue(issues);
        result.setConvertedMessage(bulletinBuilder.build());

        return result;
    }
}
