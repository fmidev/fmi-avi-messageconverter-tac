package fi.fmi.avi.converter.tac;

import fi.fmi.avi.model.taf.immutable.TAFImpl;


/**
 *
 * @author Ilkka Rinne / Spatineo Oy 2017
 */
public class ImmutableTAFTACParser extends TAFTACParserBase<TAFImpl> {

    private LexemeSequenceParser<TAFImpl> lexemeSequenceParser;

    @Override
    public LexemeSequenceParser<TAFImpl> getLexemeSequenceParser() {
        return lexemeSequenceParser;
    }

    public void setLexemeSequenceParser(final LexemeSequenceParser<TAFImpl> lexemeSequenceParser) {
        this.lexemeSequenceParser = lexemeSequenceParser;
    }
}
