package fi.fmi.avi.converter.tac.lexer.impl.token;

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
        super("^((?<above>ABV)\\s(?<unit>FL)(?<value>\\d*))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT);

        String above = match.group("above");
        if (above != null && above != "") {
            token.setParsedValue(Lexeme.ParsedValueName.RELATIONAL_OPERATOR, AviationCodeListUser.RelationalOperator.ABOVE);
        }
        token.setParsedValue(Lexeme.ParsedValueName.UNIT, match.group("unit"));
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, Integer.parseInt(match.group("value")));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            Optional<Lexeme> retval = Optional.empty();

            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                StringBuilder builder = new StringBuilder();

                Integer index = (Integer) ctx.getHints().get(ConversionHints.KEY_SWX_ANALYSIS_INDEX);
                if (index == null) {
                    throw new SerializingException("Conversion hint KEY_SWX_ANALYSIS_INDEX has not been set");
                }

                SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index);
                if (analysis.getRegion().isPresent()) {
                    SpaceWeatherRegion region = analysis.getRegion().get().get(0);
                    if (region.getAirSpaceVolume().isPresent()) {
                        AirspaceVolume volume = region.getAirSpaceVolume().get();
                        if (volume.getLowerLimitReference().isPresent()) {
                            String ref = volume.getLowerLimitReference().get();
                            if (ref.equals("STD")) {
                                builder.append(" ABV");
                            }
                            builder.append(" ");
                        }
                        if (volume.getLowerLimit().isPresent()) {
                            NumericMeasure nm = volume.getLowerLimit().get();
                            builder.append(nm.getUom());
                            DecimalFormat f = new DecimalFormat("#");
                            builder.append(f.format(nm.getValue()));

                            retval = Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT));
                        }

                    }
                }
            }
            return retval;
        }
    }
}
