package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover.SKY_OBSCURED;
import static fi.fmi.avi.model.immutable.WeatherImpl.WEATHER_CODES;

/**
 * Common parent class for AviMessageConverter implementations.
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public abstract class AbstractTACParser<T extends AviationWeatherMessage> implements TACParser<T> {

    /**
     * Finds the next {@link Lexeme} identified as <code>needle</code> in the sequence of Lexemes starting from <code>from</code>.
     *
     * @param needle
     *         the identity of the Lexeme to find
     * @param from
     *         the starting point
     *
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     */
    protected static Lexeme findNext(final Lexeme.Identity needle, final Lexeme from) {
        return findNext(needle, from, null, null);
    }

    /**
     * Finds the next {@link Lexeme} identified as <code>needle</code> in the sequence of Lexemes starting
     * from <code>from</code>.
     *
     * If the <code>found</code> is not null, it's {@link Consumer#accept(Object)} is called with the
     * possible match. If not match is found, this method is not called.
     *
     * As {@link Consumer} is a functional interface, it can be implemented as a lambda expression:
     * <pre>
     *     findNext(CORRECTION, lexed.getFirstLexeme(), stopAt, (match) -&gt; taf.setStatus(AviationCodeListUser.TAFStatus.CORRECTION));
     * </pre>
     * or, if the expression is not easily inlined:
     * <pre>
     *     findNext(AMENDMENT, lexed.getFirstLexeme(), stopAt, (match) -&gt; {
     *       TAF.TAFStatus status = taf.getStatus();
     *         if (status != null) {
     *           retval.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX_ERROR,
     *             "TAF cannot be both " + TAF.TAFStatus.AMENDMENT + " and " + status + " at " + "the same time"));
     *         } else {
     *           taf.setStatus(AviationCodeListUser.TAFStatus.AMENDMENT);
     *         }
     *     });
     * </pre>
     *
     * @param needle the identity of the Lexeme to find
     * @param from the starting point
     * @param found the function to execute with the match
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     */
    protected static Lexeme findNext(final Lexeme.Identity needle, final Lexeme from, final Consumer<Lexeme> found) {
        return findNext(needle, from, found, null);
    }

    /**
     * Finds the next {@link Lexeme} identified as <code>needle</code> in the sequence of Lexemes starting
     * from <code>from</code>.
     *
     * If the <code>found</code> is not null, it's {@link Consumer#accept(Object)} is called with the
     * possible match. If not match is found and <code>notFound</code> is not null, the function <code>notFound</code> is
     * called instead of <code>found</code>.
     *
     * @param needle
     *         the identity of the Lexeme to find
     * @param from
     *         the starting point
     * @param found
     *         the function to execute with the match
     * @param notFound
     *         the function to execute if not match was found
     *
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     */
    protected static Lexeme findNext(final Lexeme.Identity needle, final Lexeme from, final Consumer<Lexeme> found, final LexemeParsingNotifyer notFound) {
        Lexeme retval = null;
        Lexeme current = from.getNext();
        if (current != null) {
            boolean stop = false;
            Lexeme.Identity currentId;
            while (!stop) {
                currentId = current.getIdentityIfAcceptable();
                if (needle == null || currentId == needle) {
                    retval = current;
                }
                stop = !current.hasNext() || retval != null;
                current = current.getNext();
            }
        }
        if (retval != null) {
            if (found != null) {
                found.accept(retval);
            }
        } else {
            if (notFound != null) {
                notFound.ping();
            }
        }
        return retval;
    }

    protected static ConversionIssue checkBeforeAnyOf(final Lexeme lexeme, final Lexeme.Identity[] toMatch) {
        ConversionIssue retval = null;
        if (lexeme != null && toMatch != null) {
            Lexeme toCheck = lexeme;
            while (toCheck.hasPrevious()) {
                toCheck = toCheck.getPrevious();
                for (Lexeme.Identity i : toMatch) {
                    if (i == toCheck.getIdentity()) {
                        retval = new ConversionIssue(ConversionIssue.Type.SYNTAX, "Token '" + lexeme + "' was found before one of type " + i);
                        break;
                    }
                }
            }
        }
        return retval;
    }

    /**
     * Convenience method for verifying that the {@link LexemeSequence} given only contains maximum of one
     * any of the {@link Lexeme}s identified as one of <code>ids</code>.
     *
     * @param lexed sequence to check
     * @param ids the identities to verify
     * @return list the ParsingIssues to report for found extra Lexemes
     */
    protected static List<ConversionIssue> checkZeroOrOne(LexemeSequence lexed, Lexeme.Identity[] ids) {
        List<ConversionIssue> retval = new ArrayList<>();
        boolean[] oneFound = new boolean[ids.length];
        List<Lexeme> recognizedLexemes = lexed.getLexemes().stream().filter((lexeme) -> Lexeme.Status.UNRECOGNIZED != lexeme.getStatus()).collect(Collectors.toList());
        for (Lexeme l : recognizedLexemes) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i] == l.getIdentity()) {
                    if (!oneFound[i]) {
                        oneFound[i] = true;
                    } else {
                        retval.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "More than one of " + l.getIdentity() + " in " + lexed.getTAC()));
                    }
                }
            }
        }
        return retval;
    }

    protected static List<ConversionIssue> withFoundIssueTime(final LexemeSequence lexed, final Identity[] before, final ConversionHints hints, final Consumer<PartialOrCompleteTimeInstant> consumer) {
        List<ConversionIssue> retval = new ArrayList<>();
        findNext(Identity.ISSUE_TIME, lexed.getFirstLexeme(), (match) -> {
            ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
            } else {
                Integer day = match.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                Integer minute = match.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                Integer hour = match.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                if (day != null && minute != null && hour != null) {
                    consumer.accept(
                            new PartialOrCompleteTimeInstant.Builder()
                                    .setPartialTimePattern(PartialOrCompleteTimeInstant.TimePattern.DayHourMinuteZone)
                                    .setPartialTime(String.format("%02d%02d%02dZ", day, hour, minute))
                                    .build());
                } else {
                    retval.add(
                            new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing at least some of the issue time components in " + lexed.getTAC()));
                }
            }
        }, () -> retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing at least some of the issue time components in " + lexed.getTAC())));
        return retval;
    }

    protected static void withTimeForTranslation(final ConversionHints hints, final Consumer<ZonedDateTime> consumer) {
        if (hints != null && hints.containsKey(ConversionHints.KEY_TRANSLATION_TIME)){
            Object value = hints.get(ConversionHints.KEY_TRANSLATION_TIME);
            if (ConversionHints.VALUE_TRANSLATION_TIME_AUTO.equals(value)) {
                consumer.accept(ZonedDateTime.now());
            } else if (value instanceof ZonedDateTime) {
                consumer.accept((ZonedDateTime)value);
            }
        }
    }



    protected static List<ConversionIssue> appendWeatherCodes(final Lexeme source, List<fi.fmi.avi.model.Weather> target, Lexeme.Identity[] before, final ConversionHints hints) {
        Lexeme l = source;
        final List<ConversionIssue> issues = new ArrayList<>();
        while (l != null) {
            String code = l.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class);
            if (code != null) {
                ConversionIssue issue = checkBeforeAnyOf(l, before);
                if (issue != null) {
                    issues.add(issue);
                } else {
                    WeatherImpl.Builder weather = new WeatherImpl.Builder();
                    weather.setCode(code);
                    if (WEATHER_CODES.containsKey(code)) {
                        weather.setDescription(WEATHER_CODES.get(code));
                    }
                    target.add(weather.build());
                }
            }
            l = findNext(Identity.WEATHER, l);

        }
        return issues;
    }

    protected static fi.fmi.avi.model.CloudLayer getCloudLayer(final Lexeme match) throws IllegalArgumentException {
        CloudLayerImpl.Builder retval = new CloudLayerImpl.Builder();
        Object coverOrMissing = match.getParsedValue(Lexeme.ParsedValueName.COVER, Object.class);
        Object type = match.getParsedValue(Lexeme.ParsedValueName.TYPE, Object.class);
        Object value = match.getParsedValue(Lexeme.ParsedValueName.VALUE, Object.class);
        String unit = match.getParsedValue(Lexeme.ParsedValueName.UNIT, String.class);
        if (coverOrMissing instanceof CloudCover) {
            CloudCover cover = (CloudCover) coverOrMissing;
            if (SKY_OBSCURED == cover) {
                throw new IllegalArgumentException("Cannot create cloud layer with vertical visibility 'VV' token");
            }
            switch (cover) {
                case FEW:
                    retval.setAmount(AviationCodeListUser.CloudAmount.FEW);
                    break;
                case SCATTERED:
                    retval.setAmount(AviationCodeListUser.CloudAmount.SCT);
                    break;
                case BROKEN:
                    retval.setAmount(AviationCodeListUser.CloudAmount.BKN);
                    break;
                case OVERCAST:
                    retval.setAmount(AviationCodeListUser.CloudAmount.OVC);
                    break;
                case SKY_CLEAR:
                    retval.setAmount(AviationCodeListUser.CloudAmount.SKC);
                    break;
                default:
                    //NOOP
                    break;
            }
        } else if (CloudLayer.SpecialValue.CLOUD_AMOUNT_UNOBSERVABLE == coverOrMissing) {
            retval.setAmount(Optional.empty());
        } else {
            throw new IllegalArgumentException("Unknown cloud cover (amount): " + coverOrMissing);
        }
        if (CloudLayer.CloudType.TOWERING_CUMULUS == type) {
            retval.setCloudType(fi.fmi.avi.model.AviationCodeListUser.CloudType.TCU);
        } else if (CloudLayer.CloudType.CUMULONIMBUS == type) {
            retval.setCloudType(fi.fmi.avi.model.AviationCodeListUser.CloudType.CB);
        }
        if (value instanceof Integer) {
            Integer height = (Integer) value;
            if ("hft".equals(unit)) {
                retval.setBase(NumericMeasureImpl.of(height * 100, "[ft_i]"));
            } else {
                retval.setBase(NumericMeasureImpl.of(height, unit));
            }
        } else if (CloudLayer.SpecialValue.CLOUD_BASE_UNOBSERVABLE == value) {
            retval.setBase(Optional.empty());
        }
        return retval.build();
    }

    protected static List<String> getRemarks(final Lexeme remarkStart,
            final ConversionHints hints) {
        List<String> remarks = new ArrayList<>();
        if (Lexeme.Identity.REMARKS_START == remarkStart.getIdentity()) {
            Lexeme remark = findNext(Identity.REMARK, remarkStart);
            while (remark != null) {
                remarks.add(remark.getTACToken());
                remark = findNext(Identity.REMARK, remark);
            }
        }
        return remarks;
    }

    protected static boolean endsInEndToken(final LexemeSequence lexed, final ConversionHints hints) {
        return Identity.END_TOKEN == lexed.getLastLexeme().getIdentityIfAcceptable();
    }

    protected static boolean lexingSuccessful(final LexemeSequence lexed, final ConversionHints hints) {
        if (lexed.getLexemes().stream().anyMatch(l -> !l.isIgnored() && !Lexeme.Status.OK.equals(l.getStatus()))) {
            return false;
        }
        return true;
    }

    protected boolean checkAndReportLexingResult(final LexemeSequence lexed, final ConversionHints hints, final ConversionResult<?> result) {
        if (!lexingSuccessful(lexed, hints)) {
            ConversionIssue.Severity severity = ConversionIssue.Severity.ERROR;
            if (hints != null && hints.containsValue(ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS)) {
                severity = ConversionIssue.Severity.WARNING;
            } else {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Input message lexing was not fully successful: " + lexed));
            }
            List<Lexeme> errors = lexed.getLexemes().stream().filter(l -> !Lexeme.Status.OK.equals(l.getStatus())).collect(Collectors.toList());
            for (Lexeme l : errors) {
                String msg = "Lexing problem with '" + l.getTACToken() + "'";
                if (l.getLexerMessage() != null) {
                    msg = msg + ": " + l.getLexerMessage();
                }
                result.addIssue(new ConversionIssue(severity, ConversionIssue.Type.SYNTAX, msg));
            }
            if (ConversionIssue.Severity.ERROR == severity) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lambda function interface to use with
     * {@link #findNext(Identity, Lexeme, Consumer, LexemeParsingNotifyer)}.
     *
     */
    @FunctionalInterface
    interface LexemeParsingNotifyer {
        void ping();
    }
}
