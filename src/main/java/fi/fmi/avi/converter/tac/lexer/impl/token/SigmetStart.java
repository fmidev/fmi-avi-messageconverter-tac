package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REAL_SIGMET_START;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.sigmet.SIGMET;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.LOCATION_INDICATOR;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetStart extends PrioritizedLexemeVisitor {

    public SigmetStart(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        String[] words=token.getTACToken().split(" ");
        if ((words.length==2)&&"SIGMET".equals(words[1])){
            token.identify(REAL_SIGMET_START);
            token.setParsedValue(LOCATION_INDICATOR, words[0]);
        }
    }
    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET)msg;
                StringBuilder sb=new StringBuilder();
                sb.append(sigmet.getMeteorologicalWatchOffice());
                sb.append(" ");
                sb.append("SIGMET");
                return Optional.of(createLexeme(sb.toString(), REAL_SIGMET_START));
            }
            return Optional.empty();
        }
    }

}
