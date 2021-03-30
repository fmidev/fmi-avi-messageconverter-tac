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
public class SWXPhenomena extends RegexMatchingLexemeVisitor {

    public SWXPhenomena(final OccurrenceFrequency prio) {
        super("^(?<type>OBS|FCST)\\s+SWX(?:\\s+\\+(?:\\s+)?(?<hour>\\d{1,2})\\s+HR)?:$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        token.setParsedValue(Lexeme.ParsedValueName.TYPE, Type.valueOf(match.group("type")));

        if (match.group("hour") != null) {
            token.setParsedValue(Lexeme.ParsedValueName.HOUR1, Integer.parseInt(match.group("hour")));
        }
    }

    public enum Type { OBS, FCST }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final SpaceWeatherAdvisory advisory = (SpaceWeatherAdvisory) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    final SpaceWeatherAdvisoryAnalysis analysis = advisory.getAnalyses().get(analysisIndex.get());
                    final StringBuilder builder = new StringBuilder();
                    if (analysis.getAnalysisType().equals(SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION)) {
                        builder.append("OBS ");
                    } else if (analysis.getAnalysisType().equals(SpaceWeatherAdvisoryAnalysis.Type.FORECAST)) {
                        builder.append("FCST ");
                    } else {
                        throw new SerializingException("Unknown analysisType '" + analysis.getAnalysisType() + "'");
                    }

                    builder.append("SWX");

                    if (analysisIndex.get() > 0) {
                        builder.append(" +");
                        builder.append(analysisIndex.get() * 6);
                        builder.append(" HR");
                    }
                    builder.append(":");

                    retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.ADVISORY_PHENOMENA_LABEL));
                }
            }
            return retval;
        }
    }

}
