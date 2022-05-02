package fi.fmi.avi.converter.tac.bulletin;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
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
import fi.fmi.avi.model.immutable.GenericAviationWeatherMessageImpl;

public class GenericAviationWeatherMessageParser extends AbstractTACParser<GenericAviationWeatherMessage> {
    private AviMessageLexer lexer;

    public GenericAviationWeatherMessageParser(AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    @Override
    public ConversionResult<GenericAviationWeatherMessage> convertMessage(final String input, final ConversionHints hints) {
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        ConversionResult<GenericAviationWeatherMessage> result = new ConversionResult<>();
        List<ConversionIssue> issues = new ArrayList<>();
        GenericAviationWeatherMessage msg = getGenericAvaitionWeatherMessage(input, hints, issues);
        result.addIssue(issues);
        result.setConvertedMessage(msg);

        return result;
    }

    protected GenericAviationWeatherMessage getGenericAvaitionWeatherMessage(String msg,
            ConversionHints hints, List<ConversionIssue> issues) {
        Optional<MessageType> messageType = this.lexer.recognizeMessageType(msg, hints);

        if (!messageType.isPresent() || MessageType.GENERIC.equals(messageType.get())) {
            messageType = getMessageType(issues, hints);
        }

        LexemeSequence lexed = this.lexer.lexMessage(msg, hints);

        return createGenericAvaitionWeatherMessage(lexed, messageType, hints, issues);
    }

    private Optional<MessageType> getMessageType(List<ConversionIssue> issues, ConversionHints hints) {
        Optional<MessageType> messageType = Optional.empty();

        if (hints != null && hints.containsKey(ConversionHints.KEY_CONTAINED_MESSAGE_TYPE)) {
            messageType = Optional.ofNullable((MessageType) hints.get(ConversionHints.KEY_CONTAINED_MESSAGE_TYPE));
            messageType.ifPresent(mt -> hints.put(ConversionHints.KEY_MESSAGE_TYPE, mt));
        } else {
            //Fallback 2: try to determine message type from the bulletin heading:
            if(hints != null && hints.containsKey(ConversionHints.KEY_BULLETING_HEADING)) {
                BulletinHeading bulletinHeading = (BulletinHeading) hints.get(ConversionHints.KEY_BULLETING_HEADING);
                messageType = bulletinHeading.getExpectedContainedMessageType();
                if (messageType.isPresent()) {
                    if (!hints.containsKey(ConversionHints.KEY_MESSAGE_TYPE)) {
                        hints.put(ConversionHints.KEY_MESSAGE_TYPE, messageType.get());
                    }
                } else {
                    issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                            "Unable to determine contained message type for bulletin data designators " + bulletinHeading.getDataTypeDesignatorT1ForTAC() + " and " + bulletinHeading.getDataTypeDesignatorT2()));
                }
            } else {
                issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                        "Message type could not be determined for for tac message"));
            }
        }
        return messageType;
    }

    protected GenericAviationWeatherMessage createGenericAvaitionWeatherMessage(LexemeSequence lexed, Optional<MessageType> messageType,
            ConversionHints hints, List<ConversionIssue> issues) {
        final GenericAviationWeatherMessageImpl.Builder msgBuilder = GenericAviationWeatherMessageImpl.builder();

        if (messageType.isPresent() && (MessageType.SPACE_WEATHER_ADVISORY != messageType.get() && MessageType.VOLCANIC_ASH_ADVISORY != messageType.get())) {
            if (!endsInEndToken(lexed, hints)) {
                issues.add(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX,
                        "Contained message #" + lexed.getTAC() + " does not end in end token"));
                return null;
            }
        }

        messageType.ifPresent(msgBuilder::setMessageType);
        msgBuilder.setMessageFormat(GenericAviationWeatherMessage.Format.TAC);

        Lexeme l = lexed.getFirstLexeme();

        l.findNext(LexemeIdentity.AERODROME_DESIGNATOR,
                designator -> msgBuilder.putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.AERODROME,
                        designator.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class)));
        l.findNext(LexemeIdentity.SWX_CENTRE, designator -> msgBuilder.putLocationIndicators(GenericAviationWeatherMessage.LocationIndicatorType.ISSUING_CENTRE,
                designator.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class)));

        l.findNext(LexemeIdentity.ISSUE_TIME, (time) -> {
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
                        issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                "Month information available but ignored for parsing contained message issue time, year info missing"));
                    } else if (year != null && month == null) {
                        issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.MISSING_DATA,
                                "Year information available but ignored for parsing contained message issue time, month info missing"));
                    }
                    msgBuilder.setIssueTime(PartialOrCompleteTimeInstant.of(PartialDateTime.of(day != null ? day : -1, hour, minute, ZoneId.of("Z"))));
                }
            }
        });

        if (messageType.isPresent() && (MessageType.SPACE_WEATHER_ADVISORY == messageType.get() || MessageType.VOLCANIC_ASH_ADVISORY == messageType.get())) {
            //Valid time for SWX & VAA is extracted from the included phenomena time offsets:
            final PartialOrCompleteTimeInstant.Builder start = PartialOrCompleteTimeInstant.builder();
            final PartialOrCompleteTimeInstant.Builder end = PartialOrCompleteTimeInstant.builder();
            l.findNext(LexemeIdentity.ADVISORY_PHENOMENA_TIME_GROUP, (time) -> {
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
            l.findNext(LexemeIdentity.VALID_TIME, (time) -> {
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
                    issues.add(new ConversionIssue(ConversionIssue.Severity.WARNING, ConversionIssue.Type.LOGICAL,
                            "There are different valid time tokens in the message, discarding valid time info"));
                } else {
                    final PartialOrCompleteTimePeriod.Builder validTime = PartialOrCompleteTimePeriod.builder()
                            .setStartTime(PartialOrCompleteTimeInstant.of(
                                    PartialDateTime.of(fromDay != null ? fromDay : -1, fromHour != null ? fromHour : -1, fromMinute != null ? fromMinute : -1,
                                            ZoneId.of("Z"))))
                            .setEndTime(PartialOrCompleteTimeInstant.of(
                                    PartialDateTime.of(toDay != null ? toDay : -1, toHour != null ? toHour : -1, toMinute != null ? toMinute : -1,
                                            ZoneId.of("Z"))));
                    msgBuilder.setValidityTime(validTime.build());
                }

            });
        }

        if (hints != null && hints.containsKey(ConversionHints.KEY_BULLETIN_ID)) {
            msgBuilder.setNullableTranslatedBulletinID(hints.get(ConversionHints.KEY_BULLETIN_ID, String.class));
        }

        msgBuilder.setOriginalMessage(lexed.getTAC());
        msgBuilder.setTranslatedTAC(lexed.getTAC());
        msgBuilder.setTranslated(true);
        withTimeForTranslation(hints, msgBuilder::setTranslationTime);

        return msgBuilder.build();
    }
}
