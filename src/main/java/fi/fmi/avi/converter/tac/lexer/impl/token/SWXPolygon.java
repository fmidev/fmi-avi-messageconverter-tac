package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

public class SWXPolygon extends RegexMatchingLexemeVisitor {
    public SWXPolygon(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^(((N|S)\\d*\\s(W|E)\\d*)(\\s-\\s)?){5}$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_POLYGON_LIMIT);
        //TODO: parse the polygon here and store as parsed value
    }
    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final Optional<Integer> index = ctx.getParameter("analysisIndex", Integer.class);
                if (index.isPresent()) {
                    SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index.get());
                    if (analysis.getRegion().isPresent() && analysis.getRegion().get().size() > 0) {
                        SpaceWeatherRegion region = analysis.getRegion().get().get(0);
                        if (!region.getLocationIndicator().isPresent() && region.getAirSpaceVolume().isPresent()) {
                            AirspaceVolume volume = region.getAirSpaceVolume().get();
                            if (volume.getHorizontalProjection().isPresent()) {
                                List<Double> coord = ((PolygonGeometry) volume.getHorizontalProjection().get()).getExteriorRingPositions();
                                StringBuilder builder = new StringBuilder();
                                for (int i = 0; i < coord.size(); i++) {
                                    Double val = coord.get(i);
                                    if (val < 0) {
                                        builder.append("S");
                                        val = Math.abs(val);
                                    } else {
                                        builder.append("N");
                                    }
                                    builder.append(formatDouble(val));
                                    builder.append(" ");
                                    i++;
                                    val = coord.get(i);
                                    if (val < 0) {
                                        builder.append("W");
                                        val = Math.abs(val);
                                    } else {
                                        builder.append("E");
                                    }
                                    if ((val % 1) == 0) {

                                    }
                                    builder.append(formatDouble(val));
                                    if ((i + 1) != coord.size()) {
                                        builder.append(" - ");
                                    }
                                }
                                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.SWX_PHENOMENON_POLYGON_LIMIT));
                            }
                        }
                    }
                }
            }
            return retval;
        }
        private String formatDouble(final Double number) {
            if ((number % 1) == 0.0) {
                DecimalFormat f = new DecimalFormat("#");
                return f.format(number);
            }
            return number.toString();
        }
    }
}
