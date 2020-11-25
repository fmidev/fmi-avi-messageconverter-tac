package fi.fmi.avi.converter.tac.lexer.impl.token;

import java.util.ArrayList;
import java.util.List;
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
import fi.fmi.avi.model.swx.SpaceWeatherRegion;

public class SWXPresetLocation extends RegexMatchingLexemeVisitor {

    public SWXPresetLocation(final OccurrenceFrequency prio) {
        super("^(?<type>EQN|EQS|HSH|HNH|MSH|MNH|(DAYLIGHT SIDE))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);
        final String locationCode = match.group("type");
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.fromTacCode(locationCode));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            List<Lexeme> lexemes = new ArrayList<>();
            if (SpaceWeatherAdvisory.class.isAssignableFrom(clz)) {
                final Optional<Integer> index = ctx.getParameter("analysisIndex", Integer.class);
                if (index.isPresent()) {
                    SpaceWeatherAdvisoryAnalysis analysis = ((SpaceWeatherAdvisory) msg).getAnalyses().get(index.get());
                    for (int i = 0; i < analysis.getRegions().size(); i++) {
                        SpaceWeatherRegion region = analysis.getRegions().get(i);
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
