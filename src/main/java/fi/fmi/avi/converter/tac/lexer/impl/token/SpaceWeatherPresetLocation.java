package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.ArrayList;
import java.util.List;
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
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

public class SpaceWeatherPresetLocation extends RegexMatchingLexemeVisitor {

    public SpaceWeatherPresetLocation(final OccurrenceFrequency prio) {
        super("^(?<type>EQN|EQS|HSH|HNH|MSH|MNH|DAYLIGHT_SIDE)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);

        token.setParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.fromCode(match.group("type")));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            List<Lexeme> lexemes = new ArrayList<>();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                Integer index = (Integer) ctx.getHints().get(ConversionHints.KEY_SWX_ANALYSIS_INDEX);
                if (index == null) {
                    throw new SerializingException("Conversion hint KEY_SWX_ANALYSIS_INDEX has not been set");
                }
                SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index);
                if (analysis.getRegion().isPresent()) {
                    for (int i = 0; i < analysis.getRegion().get().size(); i++) {
                        SpaceWeatherRegion region = analysis.getRegion().get().get(i);
                        if (region.getLocationIndicator().isPresent()) {
                            if (i > 0) {
                                lexemes.add(this.createLexeme(" ", LexemeIdentity.WHITE_SPACE));
                            }
                            lexemes.add(this.createLexeme(region.getLocationIndicator().get().getCode(), LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION));
                        }
                    }
                }
            }

            return lexemes;
        }
    }

}
