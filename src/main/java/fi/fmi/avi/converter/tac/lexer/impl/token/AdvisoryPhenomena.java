package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;

/**
 * Created by rinne on 10/02/17.
 */
public class AdvisoryPhenomena extends RegexMatchingLexemeVisitor {

    public AdvisoryPhenomena(final OccurrenceFrequency prio) {
        super("^(?<type>OBS|FCST)(?:[a-zA-Z0-9\\+\\s]+)?:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        token.setParsedValue(Lexeme.ParsedValueName.TYPE, Type.valueOf(match.group("type")));
    }

    public enum Type { OBS, FCST }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                Integer index = (Integer) ctx.getHints().get(ConversionHints.KEY_SWX_ANALYSIS_INDEX);
                if (index == null) {
                    throw new SerializingException("Conversion hint KEY_SWX_ANALYSIS_INDEX has not been set");
                }

                StringBuilder builder = new StringBuilder();
                SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index);

                if (analysis.getAnalysisType().get().equals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION)) {
                    builder.append("OBS ");
                } else {
                    builder.append("FCST ");
                }

                builder.append("SWX");

                if (index != null && index > 0) {
                    builder.append(" +");
                    builder.append(index * 6);
                    builder.append(" HR");
                }
                builder.append(":");
                appendWhiteSpaceToString(builder, 21);

                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.ADVISORY_PHENOMENA_LABEL));
            }
            return retval;
        }
    }

}
