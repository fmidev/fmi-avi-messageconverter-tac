package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REMARK;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.REMARKS_START;

import java.util.Optional;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;

/**
 * Created by rinne on 10/02/17.
 */
public class Remark extends PrioritizedLexemeVisitor {
    public Remark(final OccurrenceFrequency prio) {
        super(prio);
    }

    @Override
    public void visit(final Lexeme token, final ConversionHints hints) {
        if (token.getPrevious() != null) {
            final Lexeme prev = token.getPrevious();
            if ((//
                    REMARK.equals(prev.getIdentityIfAcceptable()) && !token.getTACToken().startsWith("NXT ADVISORY:") //
                            || REMARKS_START.equals(prev.getIdentityIfAcceptable())) //
                    && !LexemeIdentity.END_TOKEN.equals(token.getIdentityIfAcceptable()) //
                    && !LexemeIdentity.WHITE_SPACE.equals(token.getIdentityIfAcceptable()) //
                    && !LexemeIdentity.NEXT_ADVISORY.equals(token.getIdentityIfAcceptable())) {
                token.identify(REMARK);
                token.setParsedValue(ParsedValueName.VALUE, token.getTACToken());
            }
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            return ctx.getParameter("remark", String.class).map(rmk -> this.createLexeme(rmk, REMARK));
        }
    }
}
