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

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_TAC_ELEMENT;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetTacElement extends RegexMatchingLexemeVisitor {

    public SigmetTacElement(final OccurrenceFrequency prio) {
        super("^WHATEVER$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            StringBuilder sb = new StringBuilder();
            if (SIGMETAIRMET.class.isAssignableFrom(clz)) {
                final SIGMETAIRMET message = (SIGMETAIRMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    message.getAnalysisGeometries()
                            .flatMap(geoms -> geoms.get(analysisIndex.get()).getGeometry())
                            .flatMap(TacOrGeoGeometry::getTacGeometry).ifPresent(t -> sb.append(t.getTacContent()));
                } else if (SIGMET.class.isAssignableFrom(clz)) {
                    final SIGMET sigmet = (SIGMET) message;
                    final Optional<Integer> forecastIndex = ctx.getParameter("forecastIndex", Integer.class);
                    forecastIndex.flatMap(integer -> sigmet.getForecastGeometries()
                            .flatMap(geoms -> geoms.get(integer).getGeometry())
                            .flatMap(TacOrGeoGeometry::getTacGeometry)).ifPresent(t -> sb.append(t.getTacContent()));
                }
                if (sb.length() > 0) {
                    return Optional.of(createLexeme(sb.toString(), SIGMET_TAC_ELEMENT));
                }
            }
            return Optional.empty();
        }
    }
}
