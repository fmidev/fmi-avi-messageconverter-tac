package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.TACParsingUtils.checkAndReportLexingResult;
import static fi.fmi.avi.converter.tac.TACParsingUtils.checkBeforeAnyOf;
import static fi.fmi.avi.converter.tac.TACParsingUtils.checkZeroOrOne;
import static fi.fmi.avi.converter.tac.TACParsingUtils.findNext;

import java.util.ArrayList;
import java.util.List;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBulletin;
import fi.fmi.avi.model.taf.immutable.TAFBulletinHeadingImpl;
import fi.fmi.avi.model.taf.immutable.TAFBulletinImpl;

public abstract class TAFBulletinTACParserBase extends AbstractTACParser<TAFBulletin> {

    private static final Lexeme.Identity[] zeroOrOneAllowed = { Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS,
            Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR };

    protected AviMessageLexer lexer;

    protected LexemeSequenceParser<TAF> tafParser;

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    public void setTAFLexemeSequenceParser(final LexemeSequenceParser<TAF> parser) {
        this.tafParser = parser;
    }

    protected ConversionResult<TAFBulletin> convertMessageInternal(final String input, final ConversionHints hints) {
        final ConversionResult<TAFBulletin> result = new ConversionResult<>();
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        if (this.tafParser == null) {
            throw new IllegalStateException("TAF lexeme sequence parser not set");
        }
        //1. Lex the bulletin:
        LexemeSequence lexed = this.lexer.lexMessage(input, hints);

        //2. basic validity checks of the lexing result
        if (!checkAndReportLexingResult(lexed, hints, result)) {
            return result;
        }

        final List<ConversionIssue> issues = checkZeroOrOne(lexed, zeroOrOneAllowed);
        if (!issues.isEmpty()) {
            result.addIssue(issues);
            return result;
        }

        final TAFBulletinImpl.Builder builder = new TAFBulletinImpl.Builder();

        //3. Split at each contained TAF message
        final List<LexemeSequence> subSequences = lexed.splitBy(Lexeme.Identity.TAF_START);

        //4. parse headings into TAFBulletin
        final LexemeSequence headingSequence = subSequences.get(0);

        final TAFBulletinHeadingImpl.Builder headingBuilder = new TAFBulletinHeadingImpl.Builder();

        findNext(Lexeme.Identity.BULLETIN_HEADING_DATA_DESIGNATORS, headingSequence.getFirstLexeme(), (match) -> {
            final Lexeme.Identity[] before = { Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                String type = match.getParsedValue(Lexeme.ParsedValueName.TYPE, String.class);
                if (type != null) {
                    if ("FT".equals(type)) {
                        //long TAF
                        headingBuilder.setValidLessThan12Hours(false);
                    } else if ("FC".equals(type)) {
                        //short TAF
                        headingBuilder.setValidLessThan12Hours(true);
                    } else {
                        result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Bulletin is not a TAF bulletin"));
                        return; //from lambda
                    }
                    String geospatialDesignator = match.getParsedValue(Lexeme.ParsedValueName.COUNTRY, String.class);
                    if (geospatialDesignator != null) {
                        headingBuilder.setGeographicalDesignator(geospatialDesignator);
                    } else {
                        result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                "No geospatial designator in " + "TAF bulletin heading"));
                    }
                    Integer sequenceNumber = match.getParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, Integer.class);
                    if (sequenceNumber != null) {
                        headingBuilder.setBulletinNumber(sequenceNumber);
                    } else {
                        result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                                "No bulletin number in " + "TAF bulletin heading"));
                    }
                }
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No data designator heading found")));

        findNext(Lexeme.Identity.BULLETIN_HEADING_LOCATION_INDICATOR, headingSequence.getFirstLexeme(), (match) -> {
            final Lexeme.Identity[] before = { Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR };
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                result.addIssue(issue);
            } else {
                String locationCode = match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class);
                if (locationCode != null) {
                    headingBuilder.setLocationIndicator(locationCode);
                } else {
                    result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA,
                            "No location code in " + "TAF bulletin heading"));
                }
            }
        }, () -> result.addIssue(new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No location designator found")));

        findNext(Lexeme.Identity.BULLETIN_HEADING_BBB_INDICATOR, headingSequence.getFirstLexeme(), (match) -> {
            String type = match.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class);
            if (type != null) {
                switch (type) {
                    case "RR":
                        headingBuilder.setContainingDelayedMessages(true);
                        break;
                    case "AA":
                        headingBuilder.setContainingAmendedMessages(true);
                        break;
                    case "CC":
                        headingBuilder.setContainingCorrectedMessages(true);
                        break;
                    default:
                        result.addIssue(
                                new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.SYNTAX, "Invalid 'BBB' heading type '" + type + "'"));
                }
            } else {
                result.addIssue(
                        new ConversionIssue(ConversionIssue.Severity.ERROR, ConversionIssue.Type.MISSING_DATA, "No type info in bulletin " + "'BBB' heading"));
            }
            Character seqNo = match.getParsedValue(Lexeme.ParsedValueName.SEQUENCE_NUMBER, Character.class);
            if (seqNo != null) {
                headingBuilder.setBulletinAugmentationNumber(seqNo);
            }
        });

        builder.setHeading(headingBuilder.build());

        //5. parse each TAF into a TAFImpl and add the the TAFBulletin
        ConversionResult<TAF> tafResult;
        List<TAF> tafs = new ArrayList<>();
        ConversionResult.Status worstStatus = ConversionResult.Status.SUCCESS;
        for (int i = 1; i < subSequences.size(); i++) {
            tafResult = this.tafParser.convertMessage(subSequences.get(i), hints);
            if (ConversionResult.Status.SUCCESS != tafResult.getStatus()) {
                result.addIssue(tafResult.getConversionIssues());
            } else {
                if (ConversionResult.Status.isMoreCritical(tafResult.getStatus(), worstStatus)) {
                    worstStatus = tafResult.getStatus();
                }
            }
            result.setStatus(worstStatus);
            tafResult.getConvertedMessage().ifPresent(tafs::add);
        }
        if (!tafs.isEmpty()) {
            builder.addAllMessages(tafs);
        }

        //6. profit!
        return result;
    }

}
