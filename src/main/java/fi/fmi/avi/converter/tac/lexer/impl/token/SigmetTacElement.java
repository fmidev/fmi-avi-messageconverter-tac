package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
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
            StringBuilder sb=new StringBuilder();
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET)msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    sigmet.getAnalysisGeometries().ifPresent(geoms -> {
                        geoms.get(analysisIndex.get()).getGeometry().ifPresent(geom -> {
                          geom.getTacGeometry().ifPresent(t -> {
                              sb.append(t.getData());
                          });
                        });
                    });
                } else {
                    final Optional<Integer> forecastIndex = ctx.getParameter("analysisIndex", Integer.class);
                    if (forecastIndex.isPresent()) {
                        sigmet.getAnalysisGeometries().ifPresent(geoms -> {
                            geoms.get(forecastIndex.get()).getGeometry().ifPresent(geom -> {
                                geom.getTacGeometry().ifPresent(t -> {
                                    sb.append(t.getData());
                            });
                            });
                        });
                    }
                }
                if (sb.length()>0) {
                    return Optional.of(createLexeme(sb.toString(), SIGMET_TAC_ELEMENT));
                }
            }
            return Optional.empty();
        }
    }
}
