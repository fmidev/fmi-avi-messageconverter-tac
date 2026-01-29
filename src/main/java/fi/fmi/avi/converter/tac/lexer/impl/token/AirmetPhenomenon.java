package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.sigmet.AIRMET;
import fi.fmi.avi.model.sigmet.AirmetCloudLevels;
import fi.fmi.avi.model.sigmet.AirmetWind;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;
import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.AIRMET_PHENOMENON;


/**
 * Created by rinne on 10/02/17.
 */
public class AirmetPhenomenon extends RegexMatchingLexemeVisitor {
    private final static String REGEX = "^(MT OBSC)|((ISOL|OCNL|FRQ)\\s+(CB|TCU))|(MOD ICE)|(MOD TURB)|(MOD MTW)" +
            "|((ISOL|OCNL)\\s+(TS(GR)?))" +
            "|((BKN|OVC)\\s+CLD\\s+(((\\d{3,4})|SFC)/(ABV)?((\\d{3,4}M)|(\\d{4,5}FT))))" +
            "|(SFC\\s+VIS\\s+(\\d{2,4})M)\\s+(\\((BR|DS|DU|DZ|FC|FG|FU|GR|GS|HZ|PL|PO|RA|SA|SG|SN|SQ|SS|VA)\\))" +
            "|(SFC\\s+WIND\\s+(\\d{3})/(\\d{2,3})(MPS|KT))" +
            "$";

    public AirmetPhenomenon(final OccurrenceFrequency prio) {
        super(REGEX, prio);
    }

    private static String getUnit(final String unit) {
        switch (unit) {
            case "kt":
                return "KT";
            case "mps":
                return "MPS";
            default:
                return unit;
        }
    }

    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        token.identify(AIRMET_PHENOMENON);
        final String m = match.group(0);
        if (m.equals("MT OBSC")) {
            token.setParsedValue(PHENOMENON, "MT_OBSC");
        } else if (m.equals("MOD ICE")) {
            token.setParsedValue(PHENOMENON, "MOD_ICE");
        } else if (m.equals("MOD TURB")) {
            token.setParsedValue(PHENOMENON, "MOD_TURB");
        } else if (m.equals("MOD MTW")) {
            token.setParsedValue(PHENOMENON, "MOD_MTW");
        } else if (m.equals("ISOL TS")) {
            token.setParsedValue(PHENOMENON, "ISOL_TS");
        } else if (m.equals("ISOL TSGR")) {
            token.setParsedValue(PHENOMENON, "ISOL_TSGR");
        } else if (m.equals("OCNL TS")) {
            token.setParsedValue(PHENOMENON, "OCNL_TS");
        } else if (m.equals("OCNL TSGR")) {
            token.setParsedValue(PHENOMENON, "OCNL_TSGR");
        } else if (m.startsWith("BKN CLD") || m.startsWith("OVC CLD")) {
            final String regex = "^((BKN CLD)|(OVC CLD)) ((\\d{3,4})|(SFC))/(ABV)?((\\d{3,4})(M)|(\\d{4,5})(FT))$";
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(m);
            if (matcher.matches()) {
                token.setParsedValue(PHENOMENON, matcher.group(1).replace(" ", "_"));
                token.setParsedValue(CLD_LOWLEVEL, matcher.group(4));
                if (matcher.group(7) != null) {
                    token.setParsedValue(CLD_ABOVE_LEVEL, true);
                }
                if (matcher.group(10) != null) {
                    token.setParsedValue(CLD_LEVELUNIT, "M");
                    token.setParsedValue(CLD_HIGHLEVEL, matcher.group(9));
                }
                if (matcher.group(12) != null) {
                    token.setParsedValue(CLD_LEVELUNIT, "FT");
                    token.setParsedValue(CLD_HIGHLEVEL, matcher.group(11));
                }
            }
        } else if (m.startsWith("SFC VIS")) {
            final String regex = "^SFC VIS (\\d{2,4})M (\\((BR|DS|DU|DZ|FC|FG|FU|GR|GS|HZ|PL|PO|RA|SA|SG|SN|SQ|SS|VA)\\))$";
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(m);
            if (matcher.matches()) {
                token.setParsedValue(PHENOMENON, "SFC_VIS");
                token.setParsedValue(SURFACE_VISIBILITY, matcher.group(1));
                token.setParsedValue(SURFACE_VISIBILITY_CAUSE, matcher.group(3));
            }
        } else if (m.startsWith("SFC WIND")) {
            final String regex = "^SFC WIND (\\d{3})/(\\d{2,3})(MPS|KT)$";
            final Pattern pattern = Pattern.compile(regex);
            final Matcher matcher = pattern.matcher(m);
            if (matcher.matches()) {
                token.setParsedValue(PHENOMENON, "SFC_WIND");
                token.setParsedValue(SURFACE_WIND_DIRECTION, matcher.group(1));
                token.setParsedValue(SURFACE_WIND_SPEED, matcher.group(2));
                token.setParsedValue(SURFACE_WIND_SPEED_UNIT, matcher.group(3));
            }
        } else if (m.equals("ISOL CB")) {
            token.setParsedValue(PHENOMENON, "ISOL_CB");
        } else if (m.equals("ISOL TCU")) {
            token.setParsedValue(PHENOMENON, "ISOL_TCU");
        } else if (m.equals("OCNL CB")) {
            token.setParsedValue(PHENOMENON, "OCNL_CB");
        } else if (m.equals("FRQ CB")) {
            token.setParsedValue(PHENOMENON, "FRQ_CB");
        } else if (m.equals("FRQ TCU")) {
            token.setParsedValue(PHENOMENON, "FRQ_TCU");
        }
    }

    public static class Reconstructor extends FactoryBasedReconstructor {

        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx) {
            if (AIRMET.class.isAssignableFrom(clz)) {
                final AIRMET airmet = (AIRMET) msg;
                if (airmet.getPhenomenon().isPresent()) {
                    final AviationCodeListUser.AeronauticalAirmetWeatherPhenomenon phen = airmet.getPhenomenon().get();
                    final String text;
                    text = phen.getText().replaceAll("_", " ");
                    final StringBuilder sb = new StringBuilder(text);
                    switch (phen) {
                        case SFC_VIS:
                            if (airmet.getVisibility().isPresent()) {
                                sb.append(" ");
                                final NumericMeasure vis = airmet.getVisibility().get();
                                final Double val = vis.getValue();
                                sb.append(String.format(Locale.US, "%04.0f", val));
                                sb.append("M");

                                if (airmet.getObscuration().isPresent()) {
                                    sb.append(" (");
                                    sb.append(airmet.getObscuration().get().get(0).getText());
                                    sb.append(")");
                                }
                            }
                            break;
                        case BKN_CLD:
                        case OVC_CLD:
                            if (airmet.getCloudLevels().isPresent()) {
                                sb.append(" ");
                                final AirmetCloudLevels levels = airmet.getCloudLevels().get();
                                final NumericMeasure base = levels.getCloudBase();
                                if ("M".equals(base.getUom())) {
                                    sb.append(String.format(Locale.US, "%03.0f", base.getValue()));
                                } else if ("FT".equals(base.getUom())) {
                                    if (base.getValue() == 0) {
                                        sb.append("SFC");
                                    } else if (base.getValue() < 1000) {
                                        sb.append(String.format(Locale.US, "%03.0f", base.getValue()));
                                    } else if (base.getValue() < 10000) {
                                        sb.append(String.format(Locale.US, "%04.0f", base.getValue()));
                                    }
                                }
                                sb.append("/");
                                if (levels.getTopAbove().isPresent() && levels.getTopAbove().get()) {
                                    sb.append("ABV");
                                }
                                final NumericMeasure top = levels.getCloudTop();
                                if ("M".equals(top.getUom())) {
                                    if (top.getValue() < 1000) {
                                        sb.append(String.format(Locale.US, "%03.0f", top.getValue()));
                                    } else if (top.getValue() < 10000) {
                                        sb.append(String.format(Locale.US, "%04.0f", top.getValue()));
                                    }
                                } else if ("FT".equals(top.getUom())) {
                                    if (top.getValue() == 0) {
                                        sb.append("SFC");
                                    } else if (top.getValue() < 10000) {
                                        sb.append(String.format(Locale.US, "%04.0f", top.getValue()));
                                    } else if (top.getValue() < 100000) {
                                        sb.append(String.format(Locale.US, "%05.0f", top.getValue()));
                                    }
                                }
                                sb.append(getUnit(top.getUom()));
                            }
                            break;
                        case SFC_WIND:
                            if (airmet.getWind().isPresent()) {
                                sb.append(" ");
                                final AirmetWind airmetWind = airmet.getWind().get();
                                sb.append(String.format(Locale.US, "%03.0f/", airmetWind.getDirection().getValue()));
                                final NumericMeasure wind = airmetWind.getSpeed();
                                if (wind.getValue() < 100) {
                                    sb.append(String.format(Locale.US, "%02.0f", wind.getValue()));
                                } else if (wind.getValue() < 1000) {
                                    sb.append(String.format(Locale.US, "%03.0f", wind.getValue()));
                                }
                                sb.append(getUnit(wind.getUom()));
                            }
                            break;
                        default:
                    }
                    return Optional.of(this.createLexeme(sb.toString(), LexemeIdentity.AIRMET_PHENOMENON));
                }
            }
            return Optional.empty();
        }
    }
}


