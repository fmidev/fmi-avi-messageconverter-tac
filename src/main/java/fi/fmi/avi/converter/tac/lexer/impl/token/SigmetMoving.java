package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.SIGMETAIRMET;
import fi.fmi.avi.model.bulletin.MeteorologicalBulletinSpecialCharacter;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_MOVING;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetMoving extends RegexMatchingLexemeVisitor {

    private static final String[] WIND_DIRECTIONS = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW",
            "WSW", "W", "WNW", "NW", "NNW"};

    public SigmetMoving(final OccurrenceFrequency prio) {
        super("^STNR|(MOV)\\s(N|NNE|NE|ENE|E|ESE|SE|SSE|S|SSW|SW|WSW|W|WNW|NW|NNW)\\s([0-9]{2})(KT|KMH)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        if ("STNR".equals(match.group(0))) {
            token.identify(SIGMET_MOVING);
            token.setParsedValue(STATIONARY, true);
        } else {
            token.identify(SIGMET_MOVING);
            token.setParsedValue(STATIONARY, false);
            token.setParsedValue(DIRECTION, match.group(2));
            token.setParsedValue(VALUE, match.group(3));
            token.setParsedValue(UNIT, match.group(4));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMETAIRMET.class.isAssignableFrom(clz)) {
                final SIGMETAIRMET message = (SIGMETAIRMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    if (SIGMET.class.isAssignableFrom(clz)) {
                        final SIGMET sigmet = (SIGMET) msg;
                        if (sigmet.getForecastGeometries().isPresent() && sigmet.getForecastGeometries().get().size() > 0) {
                            return Optional.empty();
                        }
                    }
                    if (!message.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingDirection().isPresent()) {
                        return Optional.of(createLexeme("STNR", SIGMET_MOVING));
                    } else {
                        StringBuilder sb = new StringBuilder();
                        sb.append("MOV");
                        sb.append(MeteorologicalBulletinSpecialCharacter.SPACE.getContent());
                        int index = (int) (message.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingDirection().get().getValue() / 22.5);
                        if ((index >= 0) && (index < 16)) {
                            sb.append(WIND_DIRECTIONS[index]);
                        }
                        sb.append(MeteorologicalBulletinSpecialCharacter.SPACE.getContent());
                        NumericMeasure spd = message.getAnalysisGeometries().get().get(analysisIndex.get()).getMovingSpeed().get();
                        sb.append(String.format(Locale.US, "%02.0f", spd.getValue()));
                        sb.append(spd.getUom());
                        return Optional.of(createLexeme(sb.toString(), SIGMET_MOVING));
                    }
                }
            }
            return Optional.empty();
        }
    }
}
