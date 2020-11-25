package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

public class SWXPhenonmenonLongitudeLimit extends RegexMatchingLexemeVisitor {
    public SWXPhenonmenonLongitudeLimit(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^((W|E)(\\d+)\\s?\\-?\\s?){2}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT);
        final List<String> limits = Arrays.asList(token.getTACToken().split("-")).stream().map(String::trim).collect(Collectors.toList());

        final Double minLimit = parseLimit(limits.get(0));
        final Double maxLimit = parseLimit(limits.get(1));

        token.setParsedValue(Lexeme.ParsedValueName.MIN_VALUE, minLimit);
        token.setParsedValue(Lexeme.ParsedValueName.MAX_VALUE, maxLimit);
    }

    private Double parseLimit(final String param) {
        Double longitude;

        final int decimalOffset = param.length() > 4 ? 4 : param.length();

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
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> lexeme = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(analysisIndex.get());
                    if (analysis.getRegions() != null && analysis.getRegions().size() > 0) {
                        final SpaceWeatherRegion region = analysis.getRegions().get(0);
                        if (region.getLongitudeLimitMinimum().isPresent() && region.getLongitudeLimitMaximum().isPresent()) {
                            final StringBuilder builder = new StringBuilder();
                            builder.append(parseLimit(region.getLongitudeLimitMinimum().get()));
                            builder.append(" - ");
                            builder.append(parseLimit(region.getLongitudeLimitMaximum().get()));

                            lexeme = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.SWX_PHENOMENON_LONGITUDE_LIMIT));
                        }
                    }
                }
            }
            return lexeme;
        }

        private String parseLimit(final Double limit) {
            final StringBuilder builder = new StringBuilder();
            if (limit < 0) {
                builder.append("W");
            } else {
                builder.append("E");
            }
            final Double absLimit = Math.abs(limit);
            final DecimalFormat formatter = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
            formatter.applyPattern(absLimit % 1.0 == 0.0 ? "000" : "000.00");
            Arrays.asList(formatter.format(absLimit).split("\\.")).stream().filter(val -> !val.isEmpty()).forEach((item) -> {
                builder.append(item);
            });
            return builder.toString();
        }
    }
}
