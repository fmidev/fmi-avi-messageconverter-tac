package fi.fmi.avi.converter.tac.bulletin;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.AbstractTACParser;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.BulletinHeading;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;
import fi.fmi.avi.model.immutable.GenericMeteorologicalBulletinImpl;
import fi.fmi.avi.util.GTSExchangeFileInfo;

public class GenericMeteorologicalBulletinParser extends AbstractTACParser<GenericMeteorologicalBulletin> {
    private static final Lexeme.Identity[] ZERO_OR_ONE_ALLOWED = {Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS,
            Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, Lexeme.Identity.ISSUE_TIME, Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR };

    private AviMessageLexer lexer;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    /**
     * Converts a single message.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the {@link ConversionResult} with the converter message and the possible conversion issues
     */
    @Override
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public ConversionResult<GenericMeteorologicalBulletin> convertMessage(final String input, final ConversionHints hints) {
        final ConversionResult<GenericMeteorologicalBulletin> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        final LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        if (Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS != lexed.getFirstLexeme().getIdentityIfAcceptable()//
                || !lexed.getFirstLexeme().hasNext()//
                || Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR != lexed.getFirstLexeme().getNext().getIdentityIfAcceptable()//
                || !lexed.getFirstLexeme().getNext().hasNext()//
                || Lexeme.Identity.ISSUE_TIME != lexed.getFirstLexeme().getNext().getNext().getIdentityIfAcceptable()) {
            result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "The input message is not recognized as Bulletin"));
            return result;
        }


        final List<ConversionIssue> issues = checkZeroOrOne(lexed, ZERO_OR_ONE_ALLOWED);
        if (!issues.isEmpty()) {
            result.addIssue(issues);
            return result;
        }

        final GenericMeteorologicalBulletinImpl.Builder bulletinBuilder = new GenericMeteorologicalBulletinImpl.Builder();

        //Split & filter in the sequences ending with END_TOKEN, will always return at least one sequence (the original), unless the original is empty:
        final List<LexemeSequence> subSequences = lexed.splitBy(false, Lexeme.Identity.END_TOKEN);

        final StringBuilder abbrHeading = new StringBuilder();
        Lexeme l = subSequences.get(0).getFirstLexeme(); // we have already checked that this is the data designators token
        abbrHeading.append(l.getTACToken());
        l = l.findNext(Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, d -> abbrHeading.append(d.getTACToken()));
        l = l.findNext(Lexeme.Identity.ISSUE_TIME, time -> abbrHeading.append(time.getTACToken()));
        Lexeme lastHeadingToken = l.findNext(Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR, bbb -> abbrHeading.append(bbb.getTACToken()));
        if (lastHeadingToken == null) {
            lastHeadingToken = l;
        }
        BulletinHeading bulletinHeading = BulletinHeadingImpl.Builder.from(abbrHeading.toString()).build();
        bulletinBuilder.setHeading(bulletinHeading);


        //Lex each the contained message again individually to collect more info:
        String msg;
        LexemeSequence messageSequence;
        Lexeme lm;
        String bulletinID = null;
        GTSExchangeFileInfo bulletinMetadata = null;
        if (hints != null && hints.containsKey(ConversionHints.KEY_BULLETIN_ID)) {
            bulletinID = hints.get(ConversionHints.KEY_BULLETIN_ID, String.class);
            try {
                bulletinMetadata = GTSExchangeFileInfo.Builder.from(bulletinID).build();
            } catch (IllegalArgumentException iae) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX, "Could not parse bulletin metadata "
                        + "from bulletinID '" + bulletinID + "'"));
            }
        }
        for (int i = 0; i < subSequences.size(); i++) {
            if (i == 0) {
                msg = lastHeadingToken.getTailSequence().trimWhiteSpace().getTAC();
            } else {
                msg = subSequences.get(i).trimWhiteSpace().getTAC();
            }
            final GenericAviationWeatherMessageImpl.Builder msgBuilder = new GenericAviationWeatherMessageImpl.Builder();
            Optional<AviationCodeListUser.MessageType> messageType = this.lexer.recognizeMessageType(msg, hints);
            messageType.ifPresent(msgBuilder::setMessageType);
            msgBuilder.setMessageFormat(GenericAviationWeatherMessage.Format.TAC);
            messageSequence = this.lexer.lexMessage(msg, hints);

            if (messageType.isPresent() &&
                    (AviationCodeListUser.MessageType.SPACE_WEATHER_ADVISORY != messageType.get()
                            && AviationCodeListUser.MessageType.VOLCANIC_ASH_ADVISORY != messageType.get() )){
                if (!endsInEndToken(messageSequence, hints)) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Contained message #" + (i+1) + " does not "
                            + "end in end token"));
                    return result;
                }
            }

            lm = messageSequence.getFirstLexeme();

            lm.findNext(Lexeme.Identity.AERODROME_DESIGNATOR, designator -> msgBuilder.setTargetAerodrome(
                    AerodromeImpl.builder().setDesignator(designator.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class)).build()));
            lm.findNext(Lexeme.Identity.ISSUE_TIME, (time) -> {
                Integer day = time.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                Integer hour = time.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                Integer minute = time.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                PartialDateTime issueTime = null;
                if (hour != null && minute != null) {
                    issueTime = PartialDateTime.of(day != null?day:-1, hour, minute, ZoneId.of("Z"));
                }
                msgBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(issueTime));

            });
            lm.findNext(Lexeme.Identity.VALID_TIME, (time) -> {
                Integer fromDay = time.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                Integer fromHour = time.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                Integer fromMinute = time.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                Integer toDay = time.getParsedValue(Lexeme.ParsedValueName.DAY2, Integer.class);
                Integer toHour = time.getParsedValue(Lexeme.ParsedValueName.HOUR2, Integer.class);
                Integer toMinute = time.getParsedValue(Lexeme.ParsedValueName.MINUTE2, Integer.class);

                //If there are different VALID_TIME lexemes in the same message, discard the entire valid time info with warning
                boolean conflict = false;
                Lexeme next = time.findNext(Lexeme.Identity.VALID_TIME);
                while (next != null) {
                    if (!next.equals(time)) {
                        conflict = true;
                    }
                    next = next.findNext(Lexeme.Identity.VALID_TIME);
                }
                if (conflict) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL,
                            "There are different valid time tokens in the message, discarding valid time info"));
                } else {
                    PartialOrCompleteTimePeriod.Builder validTime = PartialOrCompleteTimePeriod.builder()
                            .setStartTime(PartialOrCompleteTimeInstant.of(
                                    PartialDateTime.of(fromDay != null ? fromDay : -1, fromHour != null ? fromHour : -1, fromMinute != null ? fromMinute : -1,
                                            ZoneId.of("Z"))))
                            .setEndTime(PartialOrCompleteTimeInstant.of(
                                    PartialDateTime.of(toDay != null ? toDay : -1, toHour != null ? toHour : -1, toMinute != null ? toMinute : -1, ZoneId.of("Z"))));
                    msgBuilder.setValidityTime(validTime.build());
                }

            });
            msgBuilder.setOriginalMessage(messageSequence.getTAC());
            msgBuilder.setTranslated(true);
            msgBuilder.setTranslationTime(ZonedDateTime.now());
            msgBuilder.setNullableTranslatedBulletinID(bulletinID);
            bulletinBuilder.addMessages(msgBuilder.build());
        }
        result.setConvertedMessage(bulletinBuilder.build());

        return result;
    }
}
