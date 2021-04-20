package fi.fmi.avi.converter.tac.lexer.impl.token;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.SerializingException;
import fi.fmi.avi.converter.tac.lexer.impl.FactoryBasedReconstructor;
import fi.fmi.avi.converter.tac.lexer.impl.ReconstructorContext;
import fi.fmi.avi.converter.tac.lexer.impl.RegexMatchingLexemeVisitor;
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

    // private void printMatcher(Matcher m) {
    //     System.err.println(m.group("level")+"/"+m.group("level2")+m.group("unit2"));
    //     StringBuilder sb=new StringBuilder();
    //     sb.append(m.group(0)+"==>");
    //     sb.append((m.group("level")!=null)?m.group("level"):"-");
    //     sb.append(((m.group("unit")!=null)&&(m.group("unit").length()>0))?m.group("unit"):"*");
    //     sb.append((m.group("level2")!=null)?m.group("level2"):"-");
    //     sb.append((m.group("unit2")!=null)?m.group("unit2"):"*");
    //     System.err.println(sb.toString());
    // }

    // private void printMatcherTop(Matcher m) {
    // //    System.err.println(m.group("level")+"/"+m.group("level2")+m.group("unit2"));
    //     StringBuilder sb=new StringBuilder();
    //     sb.append(m.group(0)+"==>");
    //     sb.append(m.group("top"));
    //     sb.append(" ");
    //     sb.append((m.group("level2")!=null)?m.group("level2"):"-");
    //     sb.append((m.group("unit2")!=null)?m.group("unit2"):"*");
    //     System.err.println(sb.toString());
    // }

    // private void printMatcherSfc(Matcher m) {
    // //    System.err.println(m.group("level")+"/"+m.group("level2")+m.group("unit2"));
    //     StringBuilder sb=new StringBuilder();
    //     sb.append(m.group(0)+"==>");
    //     sb.append(m.group("sfc"));
    //     sb.append(" ");
    //     sb.append((m.group("level2")!=null)?m.group("level2"):"-");
    //     sb.append((m.group("unit2")!=null)?m.group("unit2"):"*");
    //     System.err.println(sb.toString());
    // }

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

	public static class Reconstructor extends FactoryBasedReconstructor {
        @Override
        public <T extends AviationWeatherMessageOrCollection> Optional<Lexeme> getAsLexeme(final T msg, final Class<T> clz, final ReconstructorContext<T> ctx)
                throws SerializingException {
            if (SIGMET.class.isAssignableFrom(clz)) {
                SIGMET sigmet = (SIGMET) msg;
                final Optional<Integer> analysisIndex = ctx.getParameter("analysisIndex", Integer.class);
                if (analysisIndex.isPresent()) {
                    StringBuilder sb=new StringBuilder();
                    if (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getLowerLimit().isPresent()) {
                        NumericMeasure lowerLevel = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getLowerLimit().get();
                        if (lowerLevel.getValue()==0.0){
                            sb.append("SFC/");
                        } else {
                            if ("FL".equals(lowerLevel.getUom())){
                                sb.append("FL");
                            }
                            if ((lowerLevel.getValue()<1000)&&("FL".equals(lowerLevel.getUom()))) {
                                sb.append(String.format("%03.0f", lowerLevel.getValue()));
                            } else if ((lowerLevel.getValue()<1000)&&("M".equals(lowerLevel.getUom()))) {
                                sb.append(String.format("%03.0f", lowerLevel.getValue()));
                            } else if (lowerLevel.getValue()<10000) {
                                sb.append(String.format("%04.0f", lowerLevel.getValue()));
                            } else {
                                sb.append(String.format("%05.0f", lowerLevel.getValue()));
                            }
                            if (!"FL".equals(lowerLevel.getUom())){
                                sb.append(lowerLevel.getUom());
                            }
                            sb.append(lowerLevel.getUom());
                            sb.append("/");
                        }
                    }
                    if (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getUpperLimitOperator().isPresent()) {
                    }
                    if (sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getUpperLimit().isPresent()) {
                        NumericMeasure upperLevel = sigmet.getAnalysisGeometries().get().get(analysisIndex.get()).getUpperLimit().get();
                        if ("FL".equals(upperLevel.getUom())){
                            sb.append("FL");
                        }
                        if ((upperLevel.getValue()<1000)&&("FL".equals(upperLevel.getUom()))) {
                            sb.append(String.format("%03.0f", upperLevel.getValue()));
                        } else if ((upperLevel.getValue()<1000)&&("M".equals(upperLevel.getUom()))) {
                            sb.append(String.format("%03.0f", upperLevel.getValue()));
                        } else if (upperLevel.getValue()<10000) {
                            sb.append(String.format("%04.0f", upperLevel.getValue()));
                        } else {
                            sb.append(String.format("%05.0f", upperLevel.getValue()));
                        }
                        if (!"FL".equals(upperLevel.getUom())){
                            sb.append(upperLevel.getUom());
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
