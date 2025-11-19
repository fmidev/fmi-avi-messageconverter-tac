package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class SWXPhenonmenonLongitudeLimit extends RegexMatchingLexemeVisitor {
    private static final String DASH_PATTERN = "[-‐–—‒―−﹣－]";

    public SWXPhenonmenonLongitudeLimit(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^((W|E)(\\d+)\\s?" + DASH_PATTERN + "?\\s?){2}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        final List<String> limits = Arrays.stream(token.getTACToken().split(DASH_PATTERN))
                .map(String::trim)
                .collect(Collectors.toList());

        final Double minLimit = parseLimit(limits.get(0));
        final Double maxLimit = parseLimit(limits.get(1));

        token.setParsedValue(Lexeme.ParsedValueName.MIN_VALUE, minLimit);
        token.setParsedValue(Lexeme.ParsedValueName.MAX_VALUE, maxLimit);
    }

    private Double parseLimit(final String param) {
        Double longitude;

        final int decimalOffset = Math.min(param.length(), 4);

        longitude = parseLongitude(decimalOffset, param);

        if (param.charAt(0) == 'W' && longitude != 0) {
            longitude *= -1;
        }

        return longitude;
    }

    private Double parseLongitude(final int offset, final String value) {
        Double longitude = Double.parseDouble(value.substring(1, offset) + "." + value.substring(offset));
        if (longitude > 180) {
            longitude = parseLongitude(offset - 1, value);
        }
        return longitude;
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        private static String parseLimit(final double limit, final int decimalPlaces) {
            final String prefix = limit < 0 ? "W" : "E";
            final double absLimit = Math.abs(limit);

            final boolean hasFraction = decimalPlaces > 0 && absLimit != Math.rint(absLimit);
            final int decimals = hasFraction ? decimalPlaces : 0;
            final double scale = Math.pow(10, decimals);
            final long scaled = Math.round(absLimit * scale);

            final int totalWidth = 3 + decimals;
            final String body = String.format(Locale.US, "%0" + totalWidth + "d", scaled);

            return prefix + body;
        }

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(
                final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) throws SerializingException {
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                return getAsLexeme((SpaceWeatherAdvisoryAmd82) msg, ctx);
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                return getAsLexeme((SpaceWeatherAdvisoryAmd79) msg, ctx);
            }
            return Optional.empty();
        }

        private <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(
                final SpaceWeatherAdvisoryAmd82 msg, final ReconstructorContext<T> ctx) throws SerializingException {
            return msg.getAnalyses()
                    .get(ctx.getMandatoryParameter("analysisIndex", Integer.class))
                    .getIntensityAndRegions()
                    .get(ctx.getMandatoryParameter("intensityAndRegionIndex", Integer.class))
                    .getRegions()
                    .stream()
                    .findFirst()
                    .map(region -> region.getLongitudeLimitMinimum().isPresent() && region.getLongitudeLimitMaximum().isPresent()
                            ? createLexeme(region.getLongitudeLimitMinimum().get(), region.getLongitudeLimitMaximum().get(), 0)
                            : null);
        }

        private <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(
                final SpaceWeatherAdvisoryAmd79 msg, final ReconstructorContext<T> ctx) throws SerializingException {
            return msg.getAnalyses()
                    .get(ctx.getMandatoryParameter("analysisIndex", Integer.class))
                    .getRegions()
                    .stream()
                    .findFirst()
                    .map(region -> region.getLongitudeLimitMinimum().isPresent() && region.getLongitudeLimitMaximum().isPresent()
                            ? createLexeme(region.getLongitudeLimitMinimum().get(), region.getLongitudeLimitMaximum().get(), 2)
                            : null);
        }

        private Lexeme createLexeme(final double longitudeLimitMinimum, final double longitudeLimitMaximum, final int decimalPlaces) {
            final String content = parseLimit(longitudeLimitMinimum, decimalPlaces) + " - " + parseLimit(longitudeLimitMaximum, decimalPlaces);
            return this.createLexeme(content, LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        }
    }
}