package fi.fmi.avi.converter.tac;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.ConversionResult;
import fi.fmi.avi.converter.tac.lexer.AviMessageLexer;
import fi.fmi.avi.model.taf.TAF;


public abstract class TAFTACParserBase<T extends TAF> extends AbstractTACParser<T> {
    protected AviMessageLexer lexer;

    protected abstract LexemeSequenceParser<T> getLexemeSequenceParser();

    @Override
    public void setTACLexer(final AviMessageLexer lexer) {
        this.lexer = lexer;
    }

    public ConversionResult<T> convertMessage(final String input, final ConversionHints hints) {
        if (this.lexer == null) {
            throw new IllegalStateException("TAC lexer not set");
        }
        final LexemeSequenceParser<T> parser = getLexemeSequenceParser();
        if (parser == null) {
            throw new IllegalStateException("TAC LexemeSequenceParser not set");
        }
        return parser.convertMessage(this.lexer.lexMessage(input, hints), hints);

    }
}
