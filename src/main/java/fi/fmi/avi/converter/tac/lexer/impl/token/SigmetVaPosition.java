package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.util.GeometryHelper;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_VA_POSITION;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetVaPosition extends RegexMatchingLexemeVisitor {
    public SigmetVaPosition(final OccurrenceFrequency prio) {
        super("^(PSN)\\s+(N\\d{2,4}|S\\d{2,4})\\s+(E\\d{3,5}|W\\d{3,5})$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(SIGMET_VA_POSITION);
        token.setParsedValue(ParsedValueName.VOLCANO_LATITUDE, match.group(2));
        token.setParsedValue(ParsedValueName.VOLCANO_LONGITUDE, match.group(3));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            final ConversionHints hints = ctx.getHints();
            final boolean specifyZeros = hints.containsKey(ConversionHints.KEY_COORDINATE_MINUTES) &&
                    ConversionHints.VALUE_COORDINATE_MINUTES_INCLUDE_ZERO.equals(hints.get(ConversionHints.KEY_COORDINATE_MINUTES));
            if (SIGMET.class.isAssignableFrom(clz)) {
                final SIGMET sigmet = (SIGMET) msg;
                if (AviationCodeListUser.AeronauticalSignificantWeatherPhenomenon.VA.equals(sigmet.getPhenomenon().orElse(null))) {
                    if (sigmet.getVAInfo().isPresent()) {
                        if (sigmet.getVAInfo().get().getVolcano().isPresent() &&
                                sigmet.getVAInfo().get().getVolcano().get().getVolcanoPosition().isPresent()) {
                            final List<Double> coords = sigmet.getVAInfo().get().getVolcano().get().getVolcanoPosition().get().getCoordinates();
                            final List<Lexeme> lexemes = GeometryHelper.createCoordinatePairLexemes(BigDecimal.valueOf(coords.get(0)),
                                    BigDecimal.valueOf(coords.get(1)), false, this::createLexeme, specifyZeros);
                            return Optional.of(this.createLexeme("PSN " + lexemes.get(0).getTACToken(), LexemeIdentity.SIGMET_VA_POSITION));
                        }
                    }
                    return Optional.empty();
                }
            }
            return Optional.empty();
        }
    }
}
