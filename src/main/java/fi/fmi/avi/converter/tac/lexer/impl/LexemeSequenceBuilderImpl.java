package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.List;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;

class LexemeSequenceBuilderImpl implements LexemeSequenceBuilder {
    private LexemeSequenceImpl seq;

    LexemeSequenceBuilderImpl() {
        seq = new LexemeSequenceImpl();
    }

    @Override
    public LexemeSequenceBuilder append(final Lexeme lexeme) {
        this.seq.addAsLast(new LexemeImpl(lexeme));
        return this;
    }

    @Override
    public LexemeSequence build() {
        return seq;
    }

    @Override
    public LexemeSequenceBuilder appendAll(List<Lexeme> lexemes) {
        if (lexemes != null) {
            for (Lexeme l : lexemes) {
                this.seq.addAsLast(new LexemeImpl(l));
            }
        }
        return this;
    }

    @Override
    public LexemeSequenceBuilder removeLast() {
        if (this.seq.lexemes.size() > 0) {
            this.seq.removeLast();
        }
        return this;
    }
}