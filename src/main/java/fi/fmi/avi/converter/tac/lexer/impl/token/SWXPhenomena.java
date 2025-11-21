package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.Optional;
import java.util.regex.Matcher;

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

    public enum Type {OBS, FCST}

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final SpaceWeatherAdvisoryAmd82 advisory = (SpaceWeatherAdvisoryAmd82) msg;
                final int analysisIndex = ctx.getParameter("analysisIndex", Integer.class).orElse(-1);
                if (analysisIndex >= 0) {
                    final fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis analysis = advisory.getAnalyses().get(analysisIndex);
                    final fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis.Type analysisType = analysis.getAnalysisType();
                    retval = Optional.of(createLexeme(analysisIndex, analysisType,
                            fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION,
                            fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis.Type.FORECAST));
                }
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final SpaceWeatherAdvisoryAmd79 advisory = (SpaceWeatherAdvisoryAmd79) msg;
                final int analysisIndex = ctx.getParameter("analysisIndex", Integer.class).orElse(-1);
                if (analysisIndex >= 0) {
                    final fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis analysis = advisory.getAnalyses().get(analysisIndex);
                    final fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis.Type analysisType = analysis.getAnalysisType();
                    retval = Optional.of(createLexeme(analysisIndex, analysisType,
                            fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis.Type.OBSERVATION,
                            fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis.Type.FORECAST));
                }
            }
            return retval;
        }

        private <T extends Enum<T>> Lexeme createLexeme(
                final int analysisIndex, final T analysisType, final T observationValue, final T forecastValue) throws SerializingException {
            final StringBuilder builder = new StringBuilder();
            if (analysisType.equals(observationValue)) {
                builder.append("OBS ");
            } else if (analysisType.equals(forecastValue)) {
                builder.append("FCST ");
            } else {
                throw new SerializingException("Unknown analysisType '" + analysisType + "'");
            }

            builder.append("SWX");

            if (analysisIndex > 0) {
                builder.append(" +");
                builder.append(analysisIndex * 6);
                builder.append(" HR");
            }
            builder.append(":");

            return createLexeme(builder.toString(), LexemeIdentity.ADVISORY_PHENOMENA_LABEL);
        }
    }
}
