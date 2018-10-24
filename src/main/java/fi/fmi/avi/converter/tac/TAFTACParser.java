package fi.fmi.avi.converter.tac;

import fi.fmi.avi.model.taf.TAF;

/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class TAFTACParser extends TAFTACParserBase<TAF> {

    private LexemeSequenceParser<TAF> lexemeSequenceParser;

    @Override
    public LexemeSequenceParser<TAF> getLexemeSequenceParser() {
        return lexemeSequenceParser;
    }

    public void setLexemeSequenceParser(final LexemeSequenceParser<TAF> lexemeSequenceParser) {
        this.lexemeSequenceParser = lexemeSequenceParser;
    }



}
