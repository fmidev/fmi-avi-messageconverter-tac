package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

public class SWXPhenonmenonLongitudeLimit extends RegexMatchingLexemeVisitor {
    public SWXPhenonmenonLongitudeLimit(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^((W|E)(\\d+)\\s?\\-?\\s?){2}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        final List<String> limits = Arrays.stream(token.getTACToken().split("-")).map(String::trim).collect(Collectors.toList());

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
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            Optional<Lexeme> lexeme = Optional.empty();
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final int analysisIndex = ctx.getParameter("analysisIndex", Integer.class).orElse(-1);
                if (analysisIndex >= 0) {
                    final fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisoryAmd82) msg).getAnalyses().get(analysisIndex);
                    if (analysis.getRegions() != null && !analysis.getRegions().isEmpty()) {
                        final fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion region = analysis.getRegions().get(0);
                        if (region.getLongitudeLimitMinimum().isPresent() && region.getLongitudeLimitMaximum().isPresent()) {
                            lexeme = Optional.of(createLexeme(region.getLongitudeLimitMinimum().get(), region.getLongitudeLimitMaximum().get()));
                        }
                    }
                }
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final int analysisIndex = ctx.getParameter("analysisIndex", Integer.class).orElse(-1);
                if (analysisIndex >= 0) {
                    final fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisoryAmd79) msg).getAnalyses().get(analysisIndex);
                    if (analysis.getRegions() != null && !analysis.getRegions().isEmpty()) {
                        final fi.fmi.avi.model.swx.amd79.SpaceWeatherRegion region = analysis.getRegions().get(0);
                        if (region.getLongitudeLimitMinimum().isPresent() && region.getLongitudeLimitMaximum().isPresent()) {
                            lexeme = Optional.of(createLexeme(region.getLongitudeLimitMinimum().get(), region.getLongitudeLimitMaximum().get()));
                        }
                    }
                }
            }
            return lexeme;
        }

        private Lexeme createLexeme(final double longitudeLimitMinimum, final double longitudeLimitMaximum) {
            final String content = parseLimit(longitudeLimitMinimum) + " - " + parseLimit(longitudeLimitMaximum);
            return this.createLexeme(content, LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        }

        private String parseLimit(final double limit) {
            final StringBuilder builder = new StringBuilder();
            if (limit < 0) {
                builder.append("W");
            } else {
                builder.append("E");
            }
            final double absLimit = Math.abs(limit);
            final DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
            formatter.applyPattern(absLimit % 1.0 == 0.0 ? "000" : "000.00");
            Arrays.stream(formatter.format(absLimit).split("\\."))//
                    .filter(val -> !val.isEmpty())//
                    .forEach(builder::append);
            return builder.toString();
        }
    }
}
