package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.AviationWeatherMessageOrCollection;
import fi.fmi.avi.model.NumericMeasure;
import fi.fmi.avi.model.sigmet.SIGMET;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static fi.fmi.avi.converter.tac.lexer.LexemeIdentity.SIGMET_LEVEL;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.ParsedValueName.*;

/**
 * Created by rinne on 10/02/17.
 */
public class SigmetLevel extends RegexMatchingLexemeVisitor {

    static String regex1="^(([0-9]{4}/)?($?[0-9]{4}M))|(([0-9]{4,5}/)?([0-9]{4,5}FT))|(([0-9]{4}M/)?(FL[0-9]{3}))|(([0-9]{4,5}FT/)?(FL[0-9]{3}))|(((TOP\\s)?(ABV|BLW)\\s)?((FL[0-9]{3})|([0-9]{4,5}FT)))|((SFC/)?(FL[0-9]{3}))|((SFC/)?([0-9]{4,5}FT))|((SFC/)?([0-9]{4}M))|(FL[0-9]{3}(/[0-9]{3})?)$";
    static String regex="^(?<meters>(([0-9]{4}/)?($?[0-9]{4}M))||(?<feet>([0-9]{4,5}/)?([0-9]{4,5}FT)))$";
    public SigmetLevel(final OccurrenceFrequency prio) {
        super(regex1, prio);
    }
    @Override
    public void visitIfMatched(final Lexeme token, final Matcher match, final ConversionHints hints) {
        String toMatch=match.group(0);
        String regex="^(?<level>[0-9]{4})?/?(?<level2>[0-9]{4})(?<unit2>M)$";
        Matcher m = Pattern.compile(regex).matcher(match.group(0));
         if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(VALUE, m.group("level"));
            token.setParsedValue(UNIT, m.group("unit2"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
            return;
        }
        regex="^(?<level>[0-9]{4,5})?/?(?<level2>[0-9]{4,5})(?<unit2>FT)$";
        m = Pattern.compile(regex).matcher(match.group(0));
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("level"));
            token.setParsedValue(UNIT, m.group("unit2"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
            return;
        }
        regex="^(?<level>[0-9]{4})(?<unit>M)/(?<unit2>FL)(?<level2>[0-9]{3})$";
        m = Pattern.compile(regex).matcher(match.group(0));
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("level"));
            token.setParsedValue(UNIT, m.group("unit"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
            return;
        }
        regex="^(?<level>[0-9]{4})(?<unit>M)/(?<level2>[0-9]{4,5})(?<unit2>FT)$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("level"));
            token.setParsedValue(UNIT, m.group("unit"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
            return;
        }
        regex="^(?<level>[0-9]{4,5})(?<unit>FT)/(?<unit2>FL)(?<level2>[0-9]{3})$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("level"));
            token.setParsedValue(UNIT, m.group("unit"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
            return;
        }
        regex="^(?<top>TOP ABV|ABV|BLW)\\s(?<unit2>FL)(?<level2>[0-9]{3})$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, m.group("top"));
            token.setParsedValue(VALUE, null);
            token.setParsedValue(UNIT, null);
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
        }
        regex="^(?<top>TOP ABV|ABV|BLW|TOP)\\s(?<level2>[0-9]{4,5})(?<unit2>FT)$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, m.group("top"));
            token.setParsedValue(VALUE, null);
            token.setParsedValue(UNIT, null);
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));

        }
        regex="^(?<sfc>SFC)?/?(?<unit2>FL)(?<level2>[0-9]{3})$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("sfc"));
            token.setParsedValue(UNIT, m.group("sfc"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
        }
        regex="^(?<sfc>SFC)?/?(?<level2>[0-9]{4})(?<unit2>M)$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("sfc"));
            token.setParsedValue(UNIT, m.group("sfc"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
        }
        regex="^(?<sfc>SFC)?/?(?<level2>[0-9]{4,5})(?<unit2>FT)$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("sfc"));
            token.setParsedValue(UNIT, m.group("sfc"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit2"));
        }
        regex="^(?<unit>FL)(?<level>[0-9]{3})/(?<level2>[0-9]{3})$";
        m = Pattern.compile(regex).matcher(toMatch);
        if (m.matches()){
            token.identify(SIGMET_LEVEL);
            token.setParsedValue(LEVEL_MODIFIER, null);
            token.setParsedValue(VALUE, m.group("level"));
            token.setParsedValue(UNIT, m.group("unit"));
            token.setParsedValue(VALUE2, m.group("level2"));
            token.setParsedValue(UNIT2, m.group("unit"));
        }
    }

    private static String stringifyHeight(NumericMeasure measure, boolean addUnit) {
        StringBuilder sb = new StringBuilder();
        if(addUnit && "FL".equals(measure.getUom())) {
            sb.append("FL");
        }
        if ((measure.getValue()<1000)&&("FL".equals(measure.getUom()))) {
            sb.append(String.format("%03.0f", measure.getValue()));
        } else if ((measure.getValue()<1000)&&("M".equals(measure.getUom()))) {
            sb.append(String.format("%03.0f", measure.getValue()));
        } else if (measure.getValue()<10000) {
            sb.append(String.format("%04.0f", measure.getValue()));
        } else {
            sb.append(String.format("%05.0f", measure.getValue()));
        }
        if (addUnit && ! "FL".equals(measure.getUom())) {
            sb.append(measure.getUom());
        }
        return sb.toString();
    }

	public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    StringBuilder sb=new StringBuilder();
                    String unitSlash=null;
                    NumericMeasure lowerLevel = null;
                    NumericMeasure upperLevel = null;
                    AviationCodeListUser.RelationalOperator lowerLimitOperator = null;
                    AviationCodeListUser.RelationalOperator upperLimitOperator = null;;

                    if (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getLowerLimit().isPresent()) {
                        lowerLevel = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getLowerLimit().get();
                    }
                    if (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getUpperLimit().isPresent()) {
                        upperLevel = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getUpperLimit().get();
                    }
                    if (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getLowerLimitOperator().isPresent()) {
                        lowerLimitOperator = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getLowerLimitOperator().get();
                    }
                    if (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getUpperLimitOperator().isPresent()) {
                        upperLimitOperator = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getUpperLimitOperator().get();
                    }
                    if (lowerLevel!=null) {
                        if (upperLevel!=null) {
                            // BTW and BTW_SFC
                            if (lowerLevel.getValue()==0.0){
                                sb.append("SFC/");
                                sb.append(stringifyHeight(upperLevel, true));
                            } else {
                                sb.append(stringifyHeight(lowerLevel, false));
                                sb.append("/");
                                sb.append(stringifyHeight(upperLevel, true));
                            }
                        } else if (lowerLimitOperator==null) {
                            // AT
                            sb.append(stringifyHeight(lowerLevel, true));
                        } if (AviationCodeListUser.RelationalOperator.ABOVE.equals(lowerLimitOperator)) {
                            // ABV
                            sb.append("ABV ");
                            sb.append(stringifyHeight(lowerLevel, true));
                        }
                    } else {
                        if (upperLevel==null) {
                            sb = new StringBuilder();
                        } else {
                            System.err.println(upperLevel+" "+ upperLimitOperator);
                            if (upperLimitOperator == null) {
                                //TOP
                                sb.append("TOP ");
                                sb.append(stringifyHeight(upperLevel, true));
                            } else if (AviationCodeListUser.RelationalOperator.ABOVE.equals(upperLimitOperator)) {
                                // TOP ABV
                                sb.append("TOP ABV");
                                sb.append(stringifyHeight(upperLevel, true));
                            } else if (AviationCodeListUser.RelationalOperator.BELOW.equals(upperLimitOperator)) {
                                // TOP BLW
                                sb.append("TOP BLW");
                                sb.append(stringifyHeight(upperLevel, true));
                            }
                        }
                    }
                    // The level string can be empty, so check for length>0
                    if (sb.length()>0) {
                        return Optional.of(this.createLexeme(
                        sb.toString(), LexemeIdentity.SIGMET_LEVEL));
                    }
                }
            }
            return Optional.empty();
        }
    }
}
