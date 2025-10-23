package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAmd79;
import fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAmd82;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

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
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            final List<Lexeme> lexemes = new ArrayList<>();
            if (SpaceWeatherAdvisoryAmd82.class.isAssignableFrom(clz)) {
                final int analysisIndex = ctx.getParameter("analysisIndex", Integer.class).orElse(-1);
                if (analysisIndex >= 0) {
                    final fi.fmi.avi.model.swx.amd82.SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisoryAmd82) msg).getAnalyses().get(analysisIndex);
                    for (int i = 0; i < analysis.getRegions().size(); i++) {
                        final int regionIndex = i;
                        analysis.getRegions().get(i).getLocationIndicator()
                                .map(fi.fmi.avi.model.swx.amd82.SpaceWeatherRegion.SpaceWeatherLocation::getCode)
                                .ifPresent(locationIndicatorCode -> addLexemes(lexemes, regionIndex, locationIndicatorCode));
                    }
                }
            } else if (SpaceWeatherAdvisoryAmd79.class.isAssignableFrom(clz)) {
                final int analysisIndex = ctx.getParameter("analysisIndex", Integer.class).orElse(-1);
                if (analysisIndex >= 0) {
                    final fi.fmi.avi.model.swx.amd79.SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisoryAmd79) msg).getAnalyses().get(analysisIndex);
                    for (int i = 0; i < analysis.getRegions().size(); i++) {
                        final int regionIndex = i;
                        analysis.getRegions().get(i).getLocationIndicator()
                                .map(fi.fmi.avi.model.swx.amd79.SpaceWeatherRegion.SpaceWeatherLocation::getCode)
                                .ifPresent(locationIndicatorCode -> addLexemes(lexemes, regionIndex, locationIndicatorCode));
                    }
                }
            }
            return lexemes;
        }

        private void addLexemes(final List<Lexeme> lexemes, final int regionIndex, final String locationIndicatorCode) {
            if (regionIndex > 0) {
                lexemes.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
            }
            lexemes.add(this.createLexeme(locationIndicatorCode, LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION));
        }
    }

}
