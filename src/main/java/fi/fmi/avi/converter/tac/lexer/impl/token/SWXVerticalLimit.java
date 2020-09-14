package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.PrioritizedLexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.swx.AirspaceVolume;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisory;
import fi.fmi.avi.model.swx.SpaceWeatherAdvisoryAnalysis;
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

import java.text.DecimalFormat;
import java.util.Optional;
import java.util.regex.Matcher;

public class SWXVerticalLimit extends RegexMatchingLexemeVisitor {
    public SWXVerticalLimit(final PrioritizedLexemeVisitor.OccurrenceFrequency prio) {
        super("^((?<above>ABV)\\s)?(?<unit>FL)(?<lowervalue>\\d*)(-(?<uppervalue>\\d*))?$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);

        token.setParsedValue(Lexeme.ParsedValueName.UNIT, match.group("unit"));
        token.setParsedValue(Lexeme.ParsedValueName.MIN_VALUE, Integer.parseInt(match.group("lowervalue")));

        if (match.group("above") != null) {
            token.setParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.ABOVE);
        }

        final String upperVal = match.group("uppervalue");
        if (upperVal != null) {
            token.setParsedValue(Lexeme.ParsedValueName.MAX_VALUE, Integer.parseInt(upperVal));
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {

                final Optional<Integer> index = ctx.getParameter("analysisIndex", Integer.class);
                if (index.isPresent()) {
                    final SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index.get());
                    if (analysis.getRegion().isPresent() && analysis.getRegion().get().size() > 0) {
                        final SpaceWeatherRegion region = analysis.getRegion().get().get(0);
                        if (region.getAirSpaceVolume().isPresent()) {
                            final StringBuilder builder = new StringBuilder();
                            final AirspaceVolume volume = region.getAirSpaceVolume().get();
                            if (volume.getLowerLimit().isPresent()) {
                                if (!volume.getUpperLimit().isPresent()) {
                                    builder.append("ABV");
                                    builder.append(" ");
                                }
                                NumericMeasure nm = volume.getLowerLimit().get();
                                builder.append(nm.getUom());
                                final DecimalFormat f = new DecimalFormat("#");
                                builder.append(f.format(nm.getValue()));
                                if (volume.getUpperLimit().isPresent()) {
                                    builder.append('-');
                                    nm = volume.getUpperLimit().get();
                                    builder.append(nm.getUom());
                                    builder.append(f.format(nm.getValue()));
                                }
                                retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT));
                            }
                        }
                    }

                }
            }
            return retval;
        }
    }
}
