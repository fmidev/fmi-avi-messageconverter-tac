package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_WITHIN;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetWithin extends PrioritizedLexemeVisitor {
    public SigmetWithin(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("WI".equalsIgnoreCase(token.getTACToken())) {
            token.identify(SIGMET_WITHIN);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            List<Lexeme> lexemes = new ArrayList<>();
            if (SIGMET.class.isAssignableFrom(clz)) {
                System.err.println("SIGMET_WITHIN");
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);

                if (analysisIndex.isPresent()) {
                    final TacOrGeoGeometry tacOrGeoGeometry = ((SIGMET) msg).getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().get();
                    if (tacOrGeoGeometry.getGeoGeometry().isPresent()&&!tacOrGeoGeometry.getTacGeometry().isPresent()) {
                        System.err.println("SIGMET_WITHIN recon has only GeoGeometry");
                        final Geometry geometry = tacOrGeoGeometry.getGeoGeometry().get();
                        System.err.println("geom: "+geometry);
                        lexemes.addAll(GeometryHelper.getGeoLexemes(geometry, (s, l)-> this.createLexeme(s, l)));
                    } else {
                        System.err.println("SIGMET_WITHIN recon has TACGeometry");
                    }
                }

            }
            return lexemes;
        }
    }

}
