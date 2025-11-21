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
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import javax.annotation.Nullable;
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
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) throws SerializingException {
            if (msg instanceof SpaceWeatherAdvisoryAmd82) {
                return getAsLexeme((SpaceWeatherAdvisoryAmd82) msg, ctx);
            } else if (msg instanceof SpaceWeatherAdvisoryAmd79) {
                return getAsLexeme((SpaceWeatherAdvisoryAmd79) msg, ctx);
            }
            return Optional.empty();
        }

        private <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final SpaceWeatherAdvisoryAmd82 msg, final ReconstructorContext<T> ctx) throws SerializingException {
            return msg.getAnalyses()
                    .get(ctx.getMandatoryParameter("analysisIndex", Integer.class))
                    .getIntensityAndRegions()
                    .get(ctx.getMandatoryParameter("intensityAndRegionIndex", Integer.class))
                    .getRegions()
                    .stream()
                    .findFirst()
                    .flatMap(fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion::getAirSpaceVolume)
                    .flatMap(airspaceVolume -> createLexeme(
                            airspaceVolume.getLowerLimit().orElse(null),
                            airspaceVolume.getUpperLimit().orElse(null)));
        }

        private <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final SpaceWeatherAdvisoryAmd79 msg, final ReconstructorContext<T> ctx) throws SerializingException {
            return msg.getAnalyses()
                    .get(ctx.getMandatoryParameter("analysisIndex", Integer.class))
                    .getRegions()
                    .stream()
                    .findFirst()
                    .flatMap(fi.fmi.avi.model.swx.amd79.SpaceWeatherRegion::getAirSpaceVolume)
                    .flatMap(airspaceVolume -> createLexeme(
                            airspaceVolume.getLowerLimit().orElse(null),
                            airspaceVolume.getUpperLimit().orElse(null)));
        }

        private Optional<Lexeme> createLexeme(final @Nullable NumericMeasure lowerLimit, final @Nullable NumericMeasure upperLimit) {
            if (lowerLimit == null) {
                return Optional.empty();
            }
            final StringBuilder builder = new StringBuilder();
            if (upperLimit == null) {
                builder.append("ABV");
                builder.append(" ");
            }
            builder.append(lowerLimit.getUom());
            final DecimalFormat f = new DecimalFormat("#");
            builder.append(f.format(lowerLimit.getValue()));
            if (upperLimit != null) {
                builder.append('-');
                builder.append(upperLimit.getUom());
                builder.append(f.format(upperLimit.getValue()));
            }
            return Optional.of(this.createLexeme(builder.toString(), LexemeIdentity.SWX_PHENOMENON_VERTICAL_LIMIT));
        }
    }
}
