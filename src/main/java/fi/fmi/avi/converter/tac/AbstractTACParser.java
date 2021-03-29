package fi.fmi.avi.converter.tac;

import static fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover.SKY_OBSCURED;
import static fi.fmi.avi.model.immutable.WeatherImpl.WEATHER_CODES;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer;
import fi.fmi.avi.converter.tac.lexer.impl.token.CloudLayer.CloudCover;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PartialDateTime;
import fi.fmi.avi.model.PartialOrCompleteTimeInstant;
import fi.fmi.avi.model.immutable.CloudLayerImpl;
import fi.fmi.avi.model.immutable.NumericMeasureImpl;
import fi.fmi.avi.model.immutable.WeatherImpl;

/**
 * Common parent class for AviMessageConverter implementations.
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public abstract class AbstractTACParser<T extends AviationWeatherMessageOrCollection> implements TACParser<T> {

    /**
     * Finds the next {@link Lexeme} identified as <code>needle</code> in the sequence of Lexemes starting from <code>from</code>.
     *
     * @param needle
     *         the identity of the Lexeme to find
     * @param from
     *         the starting point
     *
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     * @deprecated use {@link Lexeme#findNext(LexemeIdentity)} instead
     */
    protected static Lexeme findNext(final LexemeIdentity needle, final Lexeme from) {
        return from.findNext(needle);
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
     * @param needle
     *         the identity of the Lexeme to find
     * @param from
     *         the starting point
     * @param found
     *         the function to execute with the match
     *
     * @return the found Lexeme, or null if match was not found by the last Lexeme
     * @deprecated use {@link Lexeme#findNext(LexemeIdentity, Consumer)} instead
     */
    protected static Lexeme findNext(final LexemeIdentity needle, final Lexeme from, final Consumer<Lexeme> found) {
        return from.findNext(needle, found);
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
     * @deprecated use {@link Lexeme#findNext(LexemeIdentity, Consumer, Lexeme.LexemeParsingNotifyer)} instead
     */
    protected static Lexeme findNext(final LexemeIdentity needle, final Lexeme from, final Consumer<Lexeme> found, final Lexeme.LexemeParsingNotifyer notFound) {
        return from.findNext(needle, found, notFound);
    }

    protected static ConversionIssue checkBeforeAnyOf(final Lexeme lexeme, final LexemeIdentity... toMatch) {
        return lexeme == null || toMatch == null || toMatch.length == 0 ? null : checkBeforeAnyOf(lexeme, new HashSet<>(Arrays.asList(toMatch)));
    }

    protected static ConversionIssue checkBeforeAnyOf(final Lexeme lexeme, final Set<LexemeIdentity> toMatch) {
        ConversionIssue retval = null;
        if (lexeme != null && toMatch != null && !toMatch.isEmpty()) {
            Lexeme toCheck = lexeme;
            while (toCheck.hasPrevious()) {
                toCheck = toCheck.getPrevious();
                final LexemeIdentity identity = toCheck.getIdentity();
                if (toMatch.contains(identity)) {
                    retval = new ConversionIssue(ConversionIssue.Type.SYNTAX, "Invalid token order: '" + lexeme + "' was found after one of type " + identity);
                }
            }
        }
        return retval;
    }

    /**
     * Convenience method for verifying that the {@link LexemeSequence} given only contains maximum of one
     * any of the {@link Lexeme}s identified as one of <code>ids</code>.
     *
     * @param lexed
     *         sequence to check
     * @param ids
     *         the identities to verify
     *
     * @return list the ParsingIssues to report for found extra Lexemes
     */
    protected static List<ConversionIssue> checkZeroOrOne(final LexemeSequence lexed, final LexemeIdentity[] ids) {
        final List<ConversionIssue> retval = new ArrayList<>();
        checkZeroOrOne(lexed, ids, retval, new boolean[ids.length]);
        return retval;
    }

    private static void checkZeroOrOne(final LexemeSequence lexed, final LexemeIdentity[] ids, List<ConversionIssue> issues, boolean[] oneFound) {
        final List<Lexeme> recognizedLexemes = lexed.getLexemes()
                .stream()
                .filter((lexeme) -> Lexeme.Status.UNRECOGNIZED != lexeme.getStatus())
                .collect(Collectors.toList());
        for (final Lexeme l : recognizedLexemes) {
            for (int i = 0; i < ids.length; i++) {
                if (ids[i].equals(l.getIdentity())) {
                    if (!oneFound[i]) {
                        oneFound[i] = true;
                    } else {
                        issues.add(new ConversionIssue(ConversionIssue.Type.SYNTAX, "More than one of " + l.getIdentity() + " in " + lexed.getTAC()));
                    }
                }
            }
        }
    }

    protected static List<ConversionIssue> checkExactlyOne(final LexemeSequence lexed, final LexemeIdentity[] ids) {
        final List<ConversionIssue> retval = new ArrayList<>();
        final boolean[] oneFound = new boolean[ids.length];
        checkZeroOrOne(lexed, ids, retval, oneFound);
        for(int i = 0; i < oneFound.length; i++) {
            if(!oneFound[i]) {
                retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "One of " + ids[i] + " required in message " + lexed.getTAC()));
            }
        }
        return retval;
    }

    protected static List<ConversionIssue> withFoundIssueTime(final LexemeSequence lexed, final LexemeIdentity[] before, final ConversionHints hints,
            final Consumer<PartialOrCompleteTimeInstant> consumer) {
        final List<ConversionIssue> retval = new ArrayList<>();
        lexed.getFirstLexeme().findNext(LexemeIdentity.ISSUE_TIME, (match) -> {
            final ConversionIssue issue = checkBeforeAnyOf(match, before);
            if (issue != null) {
                retval.add(issue);
            } else {
                final Integer day = match.getParsedValue(Lexeme.ParsedValueName.DAY1, Integer.class);
                final Integer minute = match.getParsedValue(Lexeme.ParsedValueName.MINUTE1, Integer.class);
                final Integer hour = match.getParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.class);
                final Integer month = match.getParsedValue(Lexeme.ParsedValueName.MONTH, Integer.class);
                final Integer year = match.getParsedValue(Lexeme.ParsedValueName.YEAR, Integer.class);
                if (year != null && month != null && day != null && minute != null && hour != null) {
                    consumer.accept(PartialOrCompleteTimeInstant.of(ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Z"))));
                } else if (day != null && minute != null && hour != null) {
                    consumer.accept(PartialOrCompleteTimeInstant.of(PartialDateTime.ofDayHourMinuteZone(day, hour, minute, ZoneId.of("Z"))));
                } else {
                    retval.add(
                            new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing at least some of the issue time components in " + lexed.getTAC()));
                }
            }
        }, () -> retval.add(new ConversionIssue(ConversionIssue.Type.MISSING_DATA, "Missing at least some of the issue time components in " + lexed.getTAC())));
        return retval;
    }

    protected static void withTimeForTranslation(final ConversionHints hints, final Consumer<ZonedDateTime> consumer) {
        if (hints != null && hints.containsKey(ConversionHints.KEY_TRANSLATION_TIME)) {
            final Object value = hints.get(ConversionHints.KEY_TRANSLATION_TIME);
            if (ConversionHints.VALUE_TRANSLATION_TIME_AUTO.equals(value)) {
                consumer.accept(ZonedDateTime.now());
            } else if (value instanceof ZonedDateTime) {
                consumer.accept((ZonedDateTime) value);
            }
        }
    }

    protected static List<ConversionIssue> appendWeatherCodes(final Lexeme source, final List<fi.fmi.avi.model.Weather> target, final LexemeIdentity[] before,
            final ConversionHints hints) {
        Lexeme l = source;
        final List<ConversionIssue> issues = new ArrayList<>();
        while (l != null) {
            final String code = l.getParsedValue(Lexeme.ParsedValueName.VALUE, String.class);
            if (code != null) {
                final ConversionIssue issue = checkBeforeAnyOf(l, before);
                if (issue != null) {
                    issues.add(issue);
                } else {
                    final WeatherImpl.Builder weather = WeatherImpl.builder();
                    weather.setCode(code);
                    if (WEATHER_CODES.containsKey(code)) {
                        weather.setDescription(WEATHER_CODES.get(code));
                    }
                    target.add(weather.build());
                }
            }
            l = l.findNext(LexemeIdentity.WEATHER);

        }
        return issues;
    }

    protected static fi.fmi.avi.model.CloudLayer getCloudLayer(final Lexeme match) throws IllegalArgumentException {
        CloudLayerImpl.Builder retval = CloudLayerImpl.builder();
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

    protected static List<String> getRemarks(final Lexeme remarkStart, final ConversionHints hints) {
        final List<String> remarks = new ArrayList<>();
        if (LexemeIdentity.REMARKS_START.equals(remarkStart.getIdentity())) {
            Lexeme remark = remarkStart.findNext(LexemeIdentity.REMARK);
            while (remark != null) {
                remarks.add(remark.getTACToken());
                remark = remark.findNext(LexemeIdentity.REMARK);
            }
        }
        return remarks;
    }

    protected static boolean endsInEndToken(final LexemeSequence lexed, final ConversionHints hints) {
        return LexemeIdentity.END_TOKEN.equals(lexed.getLastLexeme().getIdentityIfAcceptable());
    }

    protected static boolean lexingSuccessful(final LexemeSequence lexed, final ConversionHints hints) {
        return lexed.getLexemes().stream().noneMatch(l -> !l.isIgnored() && !Lexeme.Status.OK.equals(l.getStatus()));
    }

    protected boolean checkAndReportLexingResult(final LexemeSequence lexed, final ConversionHints hints, final ConversionResult<?> result) {
        if (!lexingSuccessful(lexed, hints)) {
            ConversionIssue.Severity severity = ConversionIssue.Severity.ERROR;
            if (hints != null && (hints.containsValue(ConversionHints.VALUE_PARSING_MODE_ALLOW_SYNTAX_ERRORS) //
                    || hints.containsValue(ConversionHints.VALUE_PARSING_MODE_ALLOW_ANY_ERRORS))) {
                severity = ConversionIssue.Severity.WARNING;
            } else {
                result.addIssue(new ConversionIssue(ConversionIssue.Type.SYNTAX, "Input message lexing was not fully successful: " + lexed));
            }
            final List<Lexeme> errors = lexed.getLexemes().stream().filter(l -> !Lexeme.Status.OK.equals(l.getStatus())).collect(Collectors.toList());
            for (final Lexeme l : errors) {
                String msg = "Lexing problem with '" + l.getTACToken() + "'";
                if (l.getLexerMessage() != null) {
                    msg = msg + ": " + l.getLexerMessage();
                }
                result.addIssue(new ConversionIssue(severity, ConversionIssue.Type.SYNTAX, msg));
            }
            return ConversionIssue.Severity.ERROR != severity;
        }
        return true;
    }


}
