package fi.fmi.avi.converter.tac.lexer.impl.token;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

import java.util.regex.Matcher;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.model.AviationWeatherMessage;
import fi.fmi.avi.model.metar.METAR;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;

/**
 * Created by rinne on 10/02/17.
 */
public class TrendTimeGroup extends TimeHandlingRegex {
    public enum TrendTimePeriodType {
        FROM("FM"),
        AT("AT"),
        UNTIL("TL");
      
        private String code;

        TrendTimePeriodType(final String code) {
            this.code = code;
        }

        public static TrendTimePeriodType forCode(final String code) {
            for (TrendTimePeriodType w : values()) {
                if (w.code.equals(code)) {
                    return w;
                }
            }
            return null;
        }

    }
    public TrendTimeGroup(final Priority prio) {
        super("^(FM|TL|AT)([0-9]{2})([0-9]{2})$", prio);
    }

    @Override
	public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        TrendTimePeriodType type = TrendTimePeriodType.forCode(match.group(1));
       
        int hour = Integer.parseInt(match.group(2));
        int minute = Integer.parseInt(match.group(3));
        if (timeOkHourMinute(hour, minute)) {
            token.identify(Identity.TREND_TIME_GROUP);
            token.setParsedValue(HOUR1, hour);
            token.setParsedValue(MINUTE1, minute);
            token.setParsedValue(TYPE, type);
        } else {
            token.identify(Identity.TREND_TIME_GROUP, Lexeme.Status.SYNTAX_ERROR, "Invalid time");
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

		@Override
		public <T extends AviationWeatherMessage> Lexeme getAsLexeme(T msg, Class<T> clz, ConversionHints hints, Object... specifier)
				throws SerializingException {
			Lexeme retval = null;
			if (METAR.class.isAssignableFrom(clz)) {
				//TODO
			}
			return retval;
		}
    	
    }
   
}
