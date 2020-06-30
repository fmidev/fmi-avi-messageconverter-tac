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

public class SpaceWeatherPresetLocation extends RegexMatchingLexemeVisitor {

    public SpaceWeatherPresetLocation(final Priority prio) {
        super("^(?<type>EQN|EQS|HSH|HNH|MSH|MNH|DAYLIGHT_SIDE)$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION);

        token.setParsedValue(Lexeme.ParsedValueName.VALUE, SpaceWeatherRegion.SpaceWeatherLocation.fromCode(match.group("type")));
    }

    public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> List<Lexeme> getAsLexemes(T msg, Class<T> clz, ReconstructorContext<T> ctx) throws SerializingException {
            List<Lexeme> lexemes = new ArrayList<>();
            if(SpaceWeatherAdvisoryAnalysis.class.isAssignableFrom(clz)) {
                SpaceWeatherAdvisoryAnalysis analysis = (SpaceWeatherAdvisoryAnalysis) msg;
                for(SpaceWeatherRegion region : analysis.getRegion().get()) {
                    lexemes.add(this.createLexeme(region.getLocationIndicator().get().getCode() , LexemeIdentity.SWX_PHENOMENON_PRESET_LOCATION));
                }
            }

            return lexemes;
        }
    }

}
