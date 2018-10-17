package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity.SEA_STATE;

import java.util.Optional;
import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.metar.MeteorologicalTerminalAirReport;

/**
 * Created by rinne on 10/02/17.
 */
public class SeaState extends RegexMatchingLexemeVisitor {

    public enum SeaSurfaceState {
        CALM_GLASSY('0'),
        CALM_RIPPLED('1'),
        SMOOTH_WAVELETS('2'),
        SLIGHT('3'),
        MODERATE('4'),
        ROUGH('5'),
        VERY_ROUGH('6'),
        HIGH('7'),
        VERY_HIGH('8'),
        PHENOMENAL('9'),
        MISSING('/');

        private char code;

        SeaSurfaceState(final char code) {
            this.code = code;
        }

        public char getCode() {
        	return code;
        }
        
        public static SeaSurfaceState forCode(final char code) {
            for (SeaSurfaceState w : values()) {
                if (w.code == code) {
                    return w;
                }
            }
            return null;
        }
    }

    public SeaState(final Priority prio) {
        super("^W(?<minus>M?)(?<temp>[0-9]{2}|//)/(S(?<state>[0-9]|/)|H(?<height>[0-9]{1,3}))$", prio);
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        Object seaSurfaceTemperature = null;
        int temp = -1;
        if ("//".equals(match.group("temp"))) {
            seaSurfaceTemperature = SpecialValue.UNOBSERVABLE_BY_AUTO_SYSTEM;
        } else {
            temp = Integer.valueOf(match.group("temp"));
            if (match.group("minus") != null) {
                temp *= -1;
            }
            seaSurfaceTemperature = Integer.valueOf(temp);
        }
        SeaSurfaceState state = null;
        if (match.group("state") != null) {
            state = SeaSurfaceState.forCode(match.group("state").charAt(0));
        }
        Integer waveHeight = null;
        if (match.group("height") != null) {
            waveHeight = Integer.valueOf(match.group("height"));
        }
        token.identify(SEA_STATE);
        Object[] values = new Object[3];
        if (seaSurfaceTemperature != null) {
            values[0] = seaSurfaceTemperature;
            token.setParsedValue(Lexeme.ParsedValueName.UNIT, "degC");
        }
        if (state != null) {
            values[1] = state;
        }
        if (waveHeight != null) {
            values[2] = (double)waveHeight.intValue() * 0.1;
            token.setParsedValue(Lexeme.ParsedValueName.UNIT2, "m");
        }
        token.setParsedValue(Lexeme.ParsedValueName.VALUE, values);
    }

    public enum SpecialValue {UNOBSERVABLE_BY_AUTO_SYSTEM}
    
    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (MeteorologicalTerminalAirReport.class.isAssignableFrom(clz)) {
                Optional<fi.fmi.avi.model.metar.SeaState> state = ((MeteorologicalTerminalAirReport) msg).getSeaState();
                if (state.isPresent()) {
                    StringBuilder builder = new StringBuilder("W");

                    Optional<NumericMeasure> temp = state.get().getSeaSurfaceTemperature();
                    if (!temp.isPresent()) {
                        builder.append("//");
                    } else {
                        if ("degC".equals(temp.get().getUom())) {
                            int value = temp.get().getValue().intValue();
                            if (value < 0) {
                                builder.append("M");
                                value *= -1;
                            }
                            builder.append(String.format("%02d", value));
                        } else {
                            throw new SerializingException("Sea state temperature must be in degC, cannot serialize");
                        }
                    }
                    builder.append('/');

                    Optional<NumericMeasure> waveHeight = state.get().getSignificantWaveHeight();

                    if (state.get().getSeaSurfaceState().isPresent() && waveHeight.isPresent()) {
                        throw new SerializingException("Sea state can only contain either surface state or wave height, not both");
                    }

                    if (!state.get().getSeaSurfaceState().isPresent() && !waveHeight.isPresent()) {
                        throw new SerializingException("Sea state has to contain either surface state or wave height");
                    }

                    if (state.get().getSeaSurfaceState().isPresent()) {
                        // Sea surface state
                        //builder.append(String.format("S%c", state.getSeaSurfaceState().getCode()));
                        builder.append("S");
                        builder.append(state.get().getSeaSurfaceState().get().getCode());

                    } else if (waveHeight.isPresent()) {
                        // Significant wave height
                        if (!"m".equals(waveHeight.get().getUom())) {
                            throw new SerializingException("Sea state wave height must be in meters");
                        }

                        int height = (int) Math.round(waveHeight.get().getValue() / 0.1);

                        if (height < 0 || height > 999) {
                            throw new SerializingException("Sea state wave height must be between 0 and 100 meters, it was " + waveHeight.get().getValue());
                        }

                        builder.append(String.format("H%d", height));
                    }

                    return Optional.of(createLexeme(builder.toString(), SEA_STATE));
                }
            }
            return Optional.empty();
        }
    }
}
