package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReplaceAdvisoryNumber extends AbstractAdvisoryNumber {

    public ReplaceAdvisoryNumber(final OccurrenceFrequency prio) {
        super(prio, LexemeIdentity.REPLACE_ADVISORY_NUMBER, LexemeIdentity.REPLACE_ADVISORY_NUMBER_LABEL);
    }

    @Override
    protected boolean previousLexemeIdentityMatches(final LexemeIdentity previousIdentity) {
        return previousIdentity == this.previousLexemeIdentity || previousIdentity == this.lexemeIdentity;
    }

    public static class Reconstructor extends AbstractAdvisoryNumber.AbstractReconstructor {
        private static final int MAX_ADVISORIES_TO_REPLACE = 4;

        public Reconstructor() {
            super(LexemeIdentity.REPLACE_ADVISORY_NUMBER);
        }

        private Lexeme createAdvisoryLexeme(final fi.fmi.avi.model.swx.amd82.AdvisoryNumber advisoryNumber, final int index) throws SerializingException {
            final String token = toAdvisoryNumberString(advisoryNumber.getYear(), advisoryNumber.getSerialNumber());
            if (index >= MAX_ADVISORIES_TO_REPLACE) {
                final Lexeme lexeme = createLexeme(token, LexemeIdentity.REPLACE_ADVISORY_NUMBER, Lexeme.Status.WARNING);
                lexeme.setLexerMessage("Exceeding maximum number of replace advisory numbers (" + MAX_ADVISORIES_TO_REPLACE + ")");
                return lexeme;
            }
            return createLexeme(token, LexemeIdentity.REPLACE_ADVISORY_NUMBER);
        }

        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(
                final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final fi.fmi.avi.model.swx.amd79.AdvisoryNumber advisoryNumber =
                        ((SpaceWeatherAdvisoryAmd79) msg).getReplaceAdvisoryNumber().orElse(null);
                if (advisoryNumber == null) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(createAdvisoryNumberLexeme(advisoryNumber.getYear(), advisoryNumber.getSerialNumber()));
            }

            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final List<fi.fmi.avi.model.swx.amd82.AdvisoryNumber> advisoryNumbers =
                        ((SpaceWeatherAdvisoryAmd82) msg).getReplaceAdvisoryNumbers();
                if (advisoryNumbers.isEmpty()) {
                    return Collections.emptyList();
                }

                final List<Lexeme> lexemes = new ArrayList<>();
                for (int i = 0; i < advisoryNumbers.size(); i++) {
                    if (i > 0) {
                        lexemes.add(createLexeme(" ", LexemeIdentity.WHITE_SPACE));
                    }
                    lexemes.add(createAdvisoryLexeme(advisoryNumbers.get(i), i));
                }
                return lexemes;
            }
            return Collections.emptyList();
        }
    }

}
