package fi.fmi.avi.converter.tac.lexer;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COUNTRY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.COVER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DAY2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.HOUR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MAX_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MEAN_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE1;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MINUTE2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MIN_DIRECTION;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MIN_VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.MONTH;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RELATIONAL_OPERATOR2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.RUNWAY;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.SEQUENCE_NUMBER;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TENDENCY_OPERATOR;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.TYPE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.UNIT2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.VALUE2;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.YEAR;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

@JsonDeserialize(using = LexemeIdentity.Deserializer.class)
@JsonSerialize(using = LexemeIdentity.Serializer.class)
public class LexemeIdentity {
    public static final LexemeIdentity METAR_START = new LexemeIdentity("METAR_START");
    public static final LexemeIdentity SPECI_START = new LexemeIdentity("SPECI_START");
    public static final LexemeIdentity TAF_START = new LexemeIdentity("TAF_START");
    public static final LexemeIdentity ARS_START = new LexemeIdentity("ARS_START");
    public static final LexemeIdentity AIREP_START = new LexemeIdentity("AIREP_START");
    public static final LexemeIdentity SIGMET_START = new LexemeIdentity("SIGMET_START");
    public static final LexemeIdentity US_SIGMET_START = new LexemeIdentity("US_SIGMET_START");
    public static final LexemeIdentity REP = new LexemeIdentity("REP");
    public static final LexemeIdentity SPACE_WEATHER_ADVISORY_START = new LexemeIdentity("SPACE_WEATHER_ADVISORY_START");
    public static final LexemeIdentity ADVISORY_PHENOMENA_LABEL = new LexemeIdentity("ADVISORY_PHENOMENA_LABEL", TYPE);
    public static final LexemeIdentity ADVISORY_PHENOMENA_TIME_GROUP = new LexemeIdentity("ADVISORY_PHENOMENA_TIME_GROUP", DAY1, HOUR1, MINUTE1);
    public static final LexemeIdentity VOLCANIC_ASH_ADVISORY_START = new LexemeIdentity("VOLCANIC_ASH_ADVISORY_START");
    public static final LexemeIdentity CORRECTION = new LexemeIdentity("CORRECTION");
    public static final LexemeIdentity AMENDMENT = new LexemeIdentity("AMENDMENT");
    public static final LexemeIdentity CANCELLATION = new LexemeIdentity("CANCELLATION");
    public static final LexemeIdentity NIL = new LexemeIdentity("NIL");
    public static final LexemeIdentity ROUTINE_DELAYED_OBSERVATION = new LexemeIdentity("ROUTINE_DELAYED_OBSERVATION");
    public static final LexemeIdentity ISSUE_TIME = new LexemeIdentity("ISSUE_TIME", YEAR, MONTH, DAY1, HOUR1, MINUTE1);
    public static final LexemeIdentity AERODROME_DESIGNATOR = new LexemeIdentity("AERODROME_DESIGNATOR", VALUE, COUNTRY);
    public static final LexemeIdentity CAVOK = new LexemeIdentity("CAVOK");
    public static final LexemeIdentity AIR_DEWPOINT_TEMPERATURE = new LexemeIdentity("AIR_DEWPOINT_TEMPERATURE", VALUE, UNIT);
    public static final LexemeIdentity AIR_PRESSURE_QNH = new LexemeIdentity("AIR_PRESSURE_QNH", VALUE, UNIT);
    public static final LexemeIdentity SURFACE_WIND = new LexemeIdentity("SURFACE_WIND", DIRECTION, MAX_VALUE, MEAN_VALUE, UNIT, RELATIONAL_OPERATOR,
            RELATIONAL_OPERATOR2);
    public static final LexemeIdentity VARIABLE_WIND_DIRECTION = new LexemeIdentity("VARIABLE_WIND_DIRECTION", MIN_DIRECTION, MAX_DIRECTION, UNIT);
    public static final LexemeIdentity HORIZONTAL_VISIBILITY = new LexemeIdentity("HORIZONTAL_VISIBILITY", RELATIONAL_OPERATOR, VALUE, UNIT, DIRECTION);
    public static final LexemeIdentity CLOUD = new LexemeIdentity("CLOUD", VALUE, COVER, TYPE, UNIT);
    public static final LexemeIdentity TAF_FORECAST_CHANGE_INDICATOR = new LexemeIdentity("TAF_FORECAST_CHANGE_INDICATOR", DAY1, HOUR1, MINUTE1, TYPE);
    public static final LexemeIdentity TAF_CHANGE_FORECAST_TIME_GROUP = new LexemeIdentity("TAF_CHANGE_FORECAST_TIME_GROUP", DAY1, DAY2, HOUR1, HOUR2, MINUTE1);
    public static final LexemeIdentity TREND_CHANGE_INDICATOR = new LexemeIdentity("TREND_CHANGE_INDICATOR", TYPE);
    public static final LexemeIdentity NO_SIGNIFICANT_CHANGES = new LexemeIdentity("NO_SIGNIFICANT_CHANGES");
    public static final LexemeIdentity TREND_TIME_GROUP = new LexemeIdentity("TREND_TIME_GROUP", TYPE, HOUR1, MINUTE1);
    public static final LexemeIdentity NO_SIGNIFICANT_WEATHER = new LexemeIdentity("NO_SIGNIFICANT_WEATHER");
    public static final LexemeIdentity AUTOMATED = new LexemeIdentity("AUTOMATED");
    public static final LexemeIdentity RUNWAY_VISUAL_RANGE = new LexemeIdentity("RUNWAY_VISUAL_RANGE", RUNWAY, MIN_VALUE, MAX_VALUE, RELATIONAL_OPERATOR,
            RELATIONAL_OPERATOR2, TENDENCY_OPERATOR, UNIT);
    public static final LexemeIdentity WEATHER = new LexemeIdentity("WEATHER", VALUE);
    public static final LexemeIdentity RECENT_WEATHER = new LexemeIdentity("RECENT_WEATHER", VALUE);
    public static final LexemeIdentity WIND_SHEAR = new LexemeIdentity("WIND_SHEAR", RUNWAY);
    public static final LexemeIdentity SEA_STATE = new LexemeIdentity("SEA_STATE", UNIT, UNIT2, VALUE);
    public static final LexemeIdentity RUNWAY_STATE = new LexemeIdentity("RUNWAY_STATE", RUNWAY, VALUE);
    public static final LexemeIdentity SNOW_CLOSURE = new LexemeIdentity("SNOW_CLOSURE");
    public static final LexemeIdentity VALID_TIME = new LexemeIdentity("VALID_TIME", DAY1, DAY2, HOUR1, HOUR2, MINUTE1, MINUTE2);
    public static final LexemeIdentity MIN_TEMPERATURE = new LexemeIdentity("MIN_TEMPERATURE", DAY1, HOUR1, VALUE);
    public static final LexemeIdentity MAX_TEMPERATURE = new LexemeIdentity(" MAX_TEMPERATURE", DAY1, HOUR1, VALUE);
    public static final LexemeIdentity REMARKS_START = new LexemeIdentity("REMARKS_START");
    public static final LexemeIdentity REMARK = new LexemeIdentity("REMARK", VALUE);
    public static final LexemeIdentity COLOR_CODE = new LexemeIdentity("COLOR_CODE", VALUE);
    public static final LexemeIdentity WHITE_SPACE = new LexemeIdentity("WHITE_SPACE", TYPE, VALUE);
    public static final LexemeIdentity END_TOKEN = new LexemeIdentity("END_TOKEN");
    public static final LexemeIdentity BULLETIN_HEADING_DATA_DESIGNATORS = new LexemeIdentity("BULLETIN_HEADING_DATA_DESIGNATORS", VALUE);
    public static final LexemeIdentity BULLETIN_HEADING_LOCATION_INDICATOR = new LexemeIdentity("BULLETIN_HEADING_LOCATION_INDICATOR", VALUE);
    public static final LexemeIdentity BULLETIN_HEADING_BBB_INDICATOR = new LexemeIdentity("BULLETIN_HEADING_BBB_INDICATOR", VALUE, SEQUENCE_NUMBER);
    public static final LexemeIdentity ADVISORY_STATUS = new LexemeIdentity("ADVISORY_STATUS", VALUE);
    public static final LexemeIdentity ADVISORY_STATUS_LABEL = new LexemeIdentity("ADVISORY_STATUS_LABEL", VALUE);
    public static final LexemeIdentity ADVISORY_NUMBER = new LexemeIdentity("ADVISORY_NUMBER", VALUE);
    public static final LexemeIdentity ADVISORY_NUMBER_LABEL = new LexemeIdentity("ADVISORY_NUMBER_LABEL", VALUE);
    public static final LexemeIdentity REPLACE_ADVISORY_NUMBER = new LexemeIdentity("REPLACE_ADVISORY_NUMBER", VALUE);
    public static final LexemeIdentity REPLACE_ADVISORY_NUMBER_LABEL = new LexemeIdentity("REPLACE_ADVISORY_NUMBER_LABEL");
    public static final LexemeIdentity SWX_CENTRE = new LexemeIdentity("SWX_CENTRE", VALUE);
    public static final LexemeIdentity SWX_CENTRE_LABEL = new LexemeIdentity("SWX_CENTRE_LABEL", VALUE);
    public static final LexemeIdentity SWX_EFFECT_LABEL = new LexemeIdentity("SWX_EFFECT_LABEL", VALUE);
    public static final LexemeIdentity SWX_EFFECT = new LexemeIdentity("SWX_EFFECT", VALUE);
    public static final LexemeIdentity SWX_EFFECT_CONJUCTION = new LexemeIdentity("SWX_EFFECT_CONJUCTION", VALUE);
    public static final LexemeIdentity SWX_EFFECT_CATENATION = new LexemeIdentity("SWX_EFFECT_CATENATION");
    public static final LexemeIdentity SWX_PHENOMENON_PRESET_LOCATION = new LexemeIdentity("SWX_PHENOMENON_PRESET_LOCATION", VALUE);
    public static final LexemeIdentity SWX_NOT_EXPECTED = new LexemeIdentity("SWX_NOT_EXPECTED");
    public static final LexemeIdentity SWX_NOT_AVAILABLE = new LexemeIdentity("SWX_NOT_AVAILABLE");
    public static final LexemeIdentity SWX_PHENOMENON_LONGITUDE_LIMIT = new LexemeIdentity("SWX_PHENOMENON_LONGITUDE_LIMIT", MIN_VALUE, MAX_VALUE);
    public static final LexemeIdentity SWX_PHENOMENON_VERTICAL_LIMIT = new LexemeIdentity("SWX_PHENOMENON_VERTICAL_LIMIT",MIN_VALUE, MAX_VALUE, UNIT,
            RELATIONAL_OPERATOR);
    public static final LexemeIdentity SWX_PHENOMENON_POLYGON_LIMIT = new LexemeIdentity("SWX_PHENOMENON_POLYGON_LIMIT", VALUE);
    public static final LexemeIdentity POLYGON_COORDINATE_PAIR_SEPARATOR = new LexemeIdentity("POLYGON_COORDINATE_PAIR_SEPARATOR");
    public static final LexemeIdentity POLYGON_COORDINATE_PAIR = new LexemeIdentity("POLYGON_COORDINATE_PAIR", VALUE, VALUE2);
    public static final LexemeIdentity DTG_ISSUE_TIME_LABEL = new LexemeIdentity("DTG_ISSUE_TIME_LABEL", VALUE);
    public static final LexemeIdentity ADVISORY_ISSUE_TIME = new LexemeIdentity("ADVISORY_ISSUE_TIME", YEAR, MONTH, DAY1, HOUR1, MINUTE1);
    public static final LexemeIdentity NEXT_ADVISORY = new LexemeIdentity("NEXT_ADVISORY", TYPE, YEAR, MONTH, DAY1, HOUR1, MINUTE1);
    public static final LexemeIdentity NEXT_ADVISORY_LABEL = new LexemeIdentity("NEXT_ADVISORY_LABEL");

    private final String name;
    private final Set<Lexeme.ParsedValueName> possibleParameters = new HashSet<>();

    public LexemeIdentity(final String name, final Lexeme.ParsedValueName... allowedParameters) {
        this.name = name;
        if (allowedParameters != null) {
            this.possibleParameters.addAll(Arrays.asList(allowedParameters));
        }
    }

    public LexemeIdentity(final String name) {
        this(name, null);
    }

    public String name() {
        return this.name;
    }

    public Set<Lexeme.ParsedValueName> getPossibleNames() {
        return this.possibleParameters;
    }

    public boolean canStore(final Lexeme.ParsedValueName name) {
        return this.possibleParameters.contains(name);
    }

    public boolean equals(final Object other) {
        if (other instanceof LexemeIdentity) {
            return name.equals(((LexemeIdentity) other).name());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    static class Deserializer extends StdDeserializer<LexemeIdentity> {
        public Deserializer() {
            this(null);
        }

        public Deserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public LexemeIdentity deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException {
            final String value = ((JsonNode) jsonParser.getCodec().readTree(jsonParser)).asText();
            return new LexemeIdentity(value);
        }
    }

    static class Serializer extends StdSerializer<LexemeIdentity> {

        public Serializer() {
            this(null);
        }

        public Serializer(Class<LexemeIdentity> vc) {
            super(vc);
        }

        @Override
        public void serialize(final LexemeIdentity identity, final JsonGenerator jsonGenerator, final SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeString(identity.name);
        }
    }
}
