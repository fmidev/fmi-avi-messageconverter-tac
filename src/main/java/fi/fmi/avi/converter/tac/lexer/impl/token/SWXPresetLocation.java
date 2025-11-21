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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SWXPresetLocation extends RegexMatchingLexemeVisitor {

    public SWXPresetLocation(final OccurrenceFrequency prio) {
        super("^(?<type>EQN|EQS|HSH|HNH|MSH|MNH|DAYLIGHT\\s+SIDE|DAYSIDE|NIGHTSIDE)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
        final String locationCode = match.group("type");
        token.setParsedValue(Lexeme.ParsedValueName.LOCATION_INDICATOR, locationCode);
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(
                final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) throws SerializingException {
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                return getAsLexemes((SpaceWeatherAdvisoryAmd82) msg, ctx);
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                return getAsLexemes((SpaceWeatherAdvisoryAmd79) msg, ctx);
            }
            return Collections.emptyList();
        }

        private <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(
                final SpaceWeatherAdvisoryAmd82 msg, final ReconstructorContext<T> ctx) throws SerializingException {
            final List<fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion> regions = msg.getAnalyses()
                    .get(ctx.getMandatoryParameter("analysisIndex", Integer.class))
                    .getIntensityAndRegions()
                    .get(ctx.getMandatoryParameter("intensityAndRegionIndex", Integer.class))
                    .getRegions();
            return IntStream.range(0, regions.size())
                    .mapToObj(regionIndex -> regions.get(regionIndex)
                            .getLocationIndicator()
                            .map(locationIndicator -> createLexemes(regionIndex, locationIndicator.getCode()))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .flatMap(Function.identity())
                    .collect(Collectors.toList());
        }

        private <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(
                final SpaceWeatherAdvisoryAmd79 msg, final ReconstructorContext<T> ctx) throws SerializingException {
            final List<fi.fmi.avi.model.swx.amd79.SpaceWeatherRegion> regions = msg.getAnalyses()
                    .get(ctx.getMandatoryParameter("analysisIndex", Integer.class))
                    .getRegions();
            return IntStream.range(0, regions.size())
                    .mapToObj(regionIndex -> regions.get(regionIndex)
                            .getLocationIndicator()
                            .map(locationIndicator -> createLexemes(regionIndex, locationIndicator.getCode()))
                            .orElse(null))
                    .filter(Objects::nonNull)
                    .flatMap(Function.identity())
                    .collect(Collectors.toList());
        }

        private Stream<Lexeme> createLexemes(final int regionIndex, final String locationIndicatorCode) {
            final Lexeme locationLexeme = createLexeme(locationIndicatorCode, LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
            if (regionIndex == 0) {
                return Stream.of(locationLexeme);
            }
            return Stream.of(createLexeme(" ", LexemeIdentity.WHITE_SPACE), locationLexeme);
        }
    }

}
