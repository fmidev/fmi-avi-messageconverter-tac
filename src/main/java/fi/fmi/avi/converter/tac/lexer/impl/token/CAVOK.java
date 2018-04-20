package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.CAVOK;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.model.metar.TrendForecast;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.TAFBaseForecast;
import fi.fmi.avi.model.taf.TAFChangeForecast;

/**
 * Created by rinne on 10/02/17.
 */
public class CAVOK extends PrioritizedLexemeVisitor {
    public CAVOK(final Priority prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if ("CAVOK".equalsIgnoreCase(token.getTACToken())) {
            token.identify(CAVOK);
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessage> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ConversionHints hints,
                final Object... specifier) {
            Optional<Lexeme> retval = Optional.empty();
            if (METAR.class.isAssignableFrom(clz)) {
                METAR m = (METAR) msg;
                if (specifier == null || specifier.length == 0) {
                	if (m.isCeilingAndVisibilityOk()) {
                        retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                    }
                } else {
                    Optional<TrendForecast> trendForecast = getAs(specifier, TrendForecast.class);
                    if (trendForecast.isPresent()) {
                        if (trendForecast.get().isCeilingAndVisibilityOk()) {
                            retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                        }
                    }
                }
            
            } else if (TAF.class.isAssignableFrom(clz)) {
                Optional<TAFBaseForecast> base = getAs(specifier, 0, TAFBaseForecast.class);
                if (base.isPresent() && base.get().isCeilingAndVisibilityOk()) {
                    retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                }
                Optional<TAFChangeForecast> change = getAs(specifier, 0, TAFChangeForecast.class);
                if (change.isPresent() && change.get().isCeilingAndVisibilityOk()) {
                    retval = Optional.of(this.createLexeme("CAVOK", CAVOK));
                }
            }
            return retval;
        }
    }
}
