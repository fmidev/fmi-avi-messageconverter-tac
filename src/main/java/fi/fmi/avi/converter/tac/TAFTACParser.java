package fi.fmi.avi.converter.tac;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static fi.fmi.avi.converter.tac.lexer.Lexeme.Identity;

import java.util.List;
import java.util.stream.Collectors;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionIssue;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.AviationCodeListUser;
import fi.fmi.avi.model.immutable.AerodromeImpl;
import fi.fmi.avi.model.taf.TAF;
import fi.fmi.avi.model.taf.immutable.TAFImpl;


/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class TAFTACParser extends TAFTACParserBase<TAF> {

    @Override
    public ConversionResult<TAF> convertMessage(final String input, final ConversionHints hints) {
        return new ConversionResult<>(convertMessageInternal(input, hints));
    }

}
