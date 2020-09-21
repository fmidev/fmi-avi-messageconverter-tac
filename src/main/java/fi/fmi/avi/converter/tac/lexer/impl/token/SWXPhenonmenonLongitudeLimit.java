package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Arrays;
import java.util.List;
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
        List<String> limits = Arrays.asList(token.getTACToken().split("-")).stream().map(String::trim).collect(Collectors.toList());

        Double minLimit = parseLimit(limits.get(0));
        Double maxLimit = parseLimit(limits.get(1));

        token.setParsedValue(Lexeme.ParsedValueName.MIN_VALUE, minLimit);
        token.setParsedValue(Lexeme.ParsedValueName.MAX_VALUE, maxLimit);
    }

    private Double parseLimit(final String value) {
        StringBuilder builder = new StringBuilder();
        if (value.startsWith("E")) {
            builder.append("-");
        }
        return Double.parseDouble(addDecimal(builder, value.substring(1)));
    }

    private String addDecimal(final StringBuilder builder, final String value) {
        builder.append(value.substring(0, value.length() - 2));
        builder.append(".");
        builder.append(value.substring(value.length() - 2));
        return builder.toString();
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> lexeme = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(analysisIndex.get());
                    if (analysis.getRegion().isPresent() && analysis.getRegion().get().size() > 0) {
                        SpaceWeatherRegion region = analysis.getRegion().get().get(0);
                        if (region.getLongitudeLimitMinimum().isPresent() && region.getLongitudeLimitMaximum().isPresent()) {
                            StringBuilder builder = new StringBuilder();
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
            StringBuilder builder = new StringBuilder();
            if (limit < 0) {
                builder.append("E");
            } else {
                builder.append("W");
            }
            String[] limtArray = Double.toString(Math.abs(limit)).split("\\.");
            builder.append(limtArray[0]);
            builder.append(limtArray[1]);
            if (limtArray[1].length() < 2) {
                builder.append("0");
            }
            return builder.toString();
        }
            //return retval;
    }
}
