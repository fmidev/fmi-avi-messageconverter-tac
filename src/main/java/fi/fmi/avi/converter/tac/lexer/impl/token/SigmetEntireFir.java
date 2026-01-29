package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.SIGMETAIRMET;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.AREA_TYPE;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_ENTIRE_AREA;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetEntireFir extends RegexMatchingLexemeVisitor {

    public SigmetEntireFir(final OccurrenceFrequency prio) {
        super("^ENTIRE\\s+(FIR|UIR|FIR/UIR|CTA)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_ENTIRE_AREA);
        token.setParsedValue(AREA_TYPE, match.group(1));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            final String firType = "FIR"; //TODO Adapt for ENTIRE FIR/UIR etc.
            if (SIGMETAIRMET.class.isAssignableFrom(clz)) {
                final SIGMETAIRMET message = (SIGMETAIRMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    if (message.getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().isPresent()) {
                        final TacOrGeoGeometry geom = message.getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().get();
                        if (geom.getEntireArea()) {
                            return Optional.of(createLexeme("ENTIRE " + firType, SIGMET_ENTIRE_AREA));
                        }
                    }
                }

                if (SIGMET.class.isAssignableFrom(clz)) {
                    final SIGMET sigmet = (SIGMET) message;
                    final Optional<Integer> forecastIndex = ctx.getParameter("forecastIndex", Integer.class);
                    if (forecastIndex.isPresent()) {
                        if (sigmet.getForecastGeometries().get().get(forecastIndex.get()).getGeometry().isPresent()) {
                            final TacOrGeoGeometry geom = sigmet.getForecastGeometries().get().get(forecastIndex.get()).getGeometry().get();
                            if (geom.getEntireArea()) {
                                return Optional.of(createLexeme("ENTIRE " + firType, SIGMET_ENTIRE_AREA));
                            }
                        }
                    }
                }
            }
            return Optional.empty();
        }
    }
}
