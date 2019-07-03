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
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.GenericAviationWeatherMessage;
import fi.fmi.avi.model.MessageType;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.PartialOrCompleteTimePeriod;
import fi.fmi.avi.model.bulletin.BulletinHeading;
import fi.fmi.avi.model.bulletin.GenericMeteorologicalBulletin;
import fi.fmi.avi.model.bulletin.immutable.BulletinHeadingImpl;
import fi.fmi.avi.model.bulletin.immutable.GenericMeteorologicalBulletinImpl;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericMeteorologicalBulletinParser extends AbstractTACParser<GenericMeteorologicalBulletin> {
    private static final LexemeIdentity[] ZERO_OR_ONE_ALLOWED = {LexemeIdentity.BULLETIN_HEADING_DATA_DESIGNATORS,
            LexemeIdentity.BULLETIN_HEADING_LOCATION_INDICATOR, LexemeIdentity.ISSUE_TIME, LexemeIdentity.BULLETIN_HEADING_BBB_INDICATOR };

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
        Lexeme lm;

        String bulletinID = null;
        //GTSExchangeFileInfo bulletinMetadata = null;
        if (hints != null && hints.containsKey(ConversionHints.KEY_BULLETIN_ID)) {
            bulletinID = hints.get(ConversionHints.KEY_BULLETIN_ID, String.class);
            //the bulletinID could be used to provide full (or more complete message time info:
            /*
            try {
                bulletinMetadata = GTSExchangeFileInfo.Builder.from(bulletinID).build();
            } catch (IllegalArgumentException iae) {
                result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.SYNTAX, "Could not parse bulletin metadata "
                        + "from bulletinID '" + bulletinID + "'"));
            }
            */
        }
        final ConversionHints messageSpecificHints = new ConversionHints(hints);
        for (int i = 0; i < subSequences.size(); i++) {
            if (i == 0) {
                msg = lastHeadingToken.getTailSequence().trimWhiteSpace().getTAC();
            } else {
                msg = subSequences.get(i).trimWhiteSpace().getTAC();
            }
            final GenericAviationWeatherMessageImpl.Builder msgBuilder = new GenericAviationWeatherMessageImpl.Builder();
            Optional<MessageType> messageType = this.lexer.recognizeMessageType(msg, hints);
            if (!messageType.isPresent() || MessageType.GENERIC.equals(messageType.get())) {
                //Fallback: check a hint for contained message type:
                if (messageSpecificHints.containsKey(ConversionHints.KEY_CONTAINED_MESSAGE_TYPE)) {
                    messageType = Optional.ofNullable((MessageType) messageSpecificHints.get(ConversionHints.KEY_CONTAINED_MESSAGE_TYPE));
                    messageType.ifPresent(mt -> messageSpecificHints.put(ConversionHints.KEY_MESSAGE_TYPE, mt));
                } else {
                    //Fallback 2: try to determine message type from the bulletin heading:
                    messageType = bulletinHeading.getExpectedContainedMessageType();
                    if (messageType.isPresent()) {
                        if (!messageSpecificHints.containsKey(ConversionHints.KEY_MESSAGE_TYPE)) {
                            messageSpecificHints.put(ConversionHints.KEY_MESSAGE_TYPE, messageType.get());
                        }
                    } else {
                        result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                "Unable to determine contained " + "message type for bulletin data designators " + bulletinHeading.getDataTypeDesignatorT1ForTAC() + " and " + bulletinHeading.getDataTypeDesignatorT2()));
                    }
                }
            }

            messageType.ifPresent(msgBuilder::setMessageType);
            msgBuilder.setMessageFormat(GenericAviationWeatherMessage.Format.TAC);
            final LexemeSequence messageSequence = this.lexer.lexMessage(msg, messageSpecificHints);

            if (messageType.isPresent() &&
                    (MessageType.SPACE_WEATHER_ADVISORY != messageType.get()
                            && MessageType.VOLCANIC_ASH_ADVISORY != messageType.get() )){
                if (!endsInEndToken(messageSequence, hints)) {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Contained message #" + (i+1) + " does not "
                            + "end in end token"));
                    return result;
                }
            }

            lm = messageSequence.getFirstLexeme();

            lm.findNext(LexemeIdentity.AERODROME_DESIGNATOR, designator -> msgBuilder.setTargetAerodrome(
                    AerodromeImpl.builder().setDesignator(designator.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class)).build()));
            lm.findNext(LexemeIdentity.ISSUE_TIME, (time) -> {
                final Integer year = time.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class);
                final Integer month = time.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class);
                final Integer day = time.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                final Integer hour = time.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                final Integer minute = time.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                if (hour != null && minute != null) {
                    //Do we have enough info for a complete time?
                    if (year != null && month != null && day != null) {
                        msgBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Z"))));
                    } else {
                        if (year == null && month != null) {
                            result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Month information "
                                    + "available but ignored for parsing contained message issue time, year info missing"));
                        } else if (year != null && month == null) {
                            result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA, "Year information "
                                    + "available but ignored for parsing contained message issue time, month info missing"));
                        }
                        msgBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(PartialDateTime.of(day != null?day:-1, hour, minute, ZoneId.of("Z"))));
                    }
                }
            });

            if (messageType.isPresent() &&
                    (MessageType.SPACE_WEATHER_ADVISORY == messageType.get()
                            || MessageType.VOLCANIC_ASH_ADVISORY == messageType.get())) {
                //Valid time for SWX & VAA is extracted from the included phenomena time offsets:
                final PartialOrCompleteTimeInstant.Builder start = PartialOrCompleteTimeInstant.builder();
                final PartialOrCompleteTimeInstant.Builder end = PartialOrCompleteTimeInstant.builder();
                lm.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, (time) -> {
                    Integer day = time.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    Integer hour = time.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    Integer minute = time.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                    start.setPartialTime(PartialDateTime.of(day != null ? day : -1, hour != null ? hour : -1, minute != null ? minute : -1, ZoneId.of("Z")));

                    //Valid time end is the last time group value
                    Lexeme ll = time.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
                    while (ll != null) {
                        day = ll.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                        hour = ll.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                        minute = ll.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                        end.setPartialTime(PartialDateTime.of(day != null ? day : -1, hour != null ? hour : -1, minute != null ? minute : -1, ZoneId.of("Z")));
                        ll = ll.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP);
                    }
                    msgBuilder.setValidityTime(PartialOrCompleteTimePeriod.builder()//
                            .setStartTime(start.build())//
                            .setEndTime(end.build())//
                            .build());
                });
            } else {
                lm.findNext(LexemeIdentity.VALID_TIME, (time) -> {
                    final Integer fromDay = time.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                    final Integer fromHour = time.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                    final Integer fromMinute = time.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                    final Integer toDay = time.getParsedValue(Lexeme.ParsedValueName.DAY2, Integer.class);
                    final Integer toHour = time.getParsedValue(Lexeme.ParsedValueName.HOUR2, Integer.class);
                    final Integer toMinute = time.getParsedValue(Lexeme.ParsedValueName.MINUTE2, Integer.class);

                    //If there are different VALID_TIME lexemes in the same message, discard the entire valid time info with warning
                    boolean conflict = false;
                    Lexeme next = time.findNext(LexemeIdentity.VALID_TIME);
                    while (next != null) {
                        if (!next.equals(time)) {
                            conflict = true;
                        }
                        next = next.findNext(LexemeIdentity.VALID_TIME);
                    }
                    if (conflict) {
                        result.addIssue(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL,
                                "There are different valid time tokens in the message, discarding valid time info"));
                    } else {
                        final PartialOrCompleteTimePeriod.Builder validTime = PartialOrCompleteTimePeriod.builder()
                                .setStartTime(PartialOrCompleteTimeInstant.of(
                                        PartialDateTime.of(fromDay != null ? fromDay : -1, fromHour != null ? fromHour : -1, fromMinute != null ? fromMinute : -1,
                                                ZoneId.of("Z"))))
                                .setEndTime(PartialOrCompleteTimeInstant.of(
                                        PartialDateTime.of(toDay != null ? toDay : -1, toHour != null ? toHour : -1, toMinute != null ? toMinute : -1, ZoneId.of("Z"))));
                        msgBuilder.setValidityTime(validTime.build());
                    }

                });
            }
            msgBuilder.setOriginalMessage(messageSequence.getTAC());
            msgBuilder.setTranslatedTAC(messageSequence.getTAC());
            msgBuilder.setTranslated(true);
            withTimeForTranslation(hints, (time) -> {
                msgBuilder.setTranslationTime(time);
            });
            msgBuilder.setNullableTranslatedBulletinID(bulletinID);
            bulletinBuilder.addMessages(msgBuilder.build());
        }
        result.setConvertedMessage(bulletinBuilder.build());

        return result;
    }
}
