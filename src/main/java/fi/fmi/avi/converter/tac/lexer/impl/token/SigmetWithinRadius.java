package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_RADIUS;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_RADIUS_UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_LAT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RDOACT_LON;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.geoinfo.GeoUtilsTac;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.CircleByCenterPoint;
import fi.fmi.avi.model.Geometry;
import fi.fmi.avi.model.PolygonGeometry;
import fi.fmi.avi.model.TacOrGeoGeometry;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetWithinRadius extends RegexMatchingLexemeVisitor {
    public SigmetWithinRadius(final OccurrenceFrequency prio) {
        super("^WI (\\d{2})(KM|NM) OF ((N|S)(\\d{2,4})) ((W|E)(\\d{3,5}))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_WITHIN_RADIUS_OF_POINT);
        token.setParsedValue(RDOACT_RADIUS, Integer.parseInt(match.group(1)));
        token.setParsedValue(RDOACT_RADIUS_UNIT, match.group(2));
        token.setParsedValue(RDOACT_LON, GeoUtilsTac.getLatLon(match.group(3)));
        token.setParsedValue(RDOACT_LAT, GeoUtilsTac.getLatLon(match.group(6)));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, Class<T> clz, final ReconstructorContext<T> ctx) {
            List<Lexeme> lexemes = new ArrayList<>();
            if (SIGMET.class.isAssignableFrom(clz)) {
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final TacOrGeoGeometry tacOrGeoGeometry = ((SIGMET) msg).getAnalysisGeometries().get().get(analysisIndex.get()).getGeometry().get();
                    if (tacOrGeoGeometry.getGeoGeometry().isPresent()
                            &&!tacOrGeoGeometry.getTacGeometry().isPresent()
                            &&!tacOrGeoGeometry.getEntireArea()) {
                        final Geometry geometry = tacOrGeoGeometry.getGeoGeometry().get();
                        if (geometry instanceof CircleByCenterPoint) {
                            double radius = ((CircleByCenterPoint)geometry).getRadius().getValue();
                            String unit = ((CircleByCenterPoint)geometry).getRadius().getUom();
                            List<Double> coords = ((CircleByCenterPoint)geometry).getCenterPointCoordinates();
                            String tac = String.format("WI %02.0f%s OF %s%04d %s%05d", radius, unit,
                                    coords.get(0)<0?"S":"N", Math.round(coords.get(0)*100),
                                    coords.get(1)<0?"W":"E", Math.round(coords.get(1)*100));
                            lexemes.add(this.createLexeme(tac, LexemeIdentity.SIGMET_WITHIN_RADIUS_OF_POINT));
                //            lexemes.add(this.createLexeme(MeteorologicalBulletinSpecialCharacter.SPACE.getContent(), LexemeIdentity.WHITE_SPACE));
                        }
                    } else {
                        // System.err.println("SIGMET_WITHIN recon has TACGeometry");
                        // TODO should this condition be handled as error
                    }
                }
            }
            return lexemes;
        }
    }
}
