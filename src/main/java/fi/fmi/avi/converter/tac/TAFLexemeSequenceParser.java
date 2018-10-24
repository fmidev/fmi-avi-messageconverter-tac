package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.model.taf.TAF;

public class TAFLexemeSequenceParser extends TAFLexemeSequenceParserBase<TAF> {
    /**
     * Converts a single message.
     *
     * @param input
     *         input message
     * @param hints
     *         parsing hints
     *
     * @return the {@link ConversionResult} with the converter message and the possible conversion issues
     */
    @Override
    public ConversionResult<TAF> convertMessage(final LexemeSequence input, final ConversionHints hints) {
        return new ConversionResult<>(convertMessageInternal(input, hints));
    }
}
