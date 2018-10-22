package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.List;
import java.util.Optional;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;

class LexemeSequenceBuilderImpl implements LexemeSequenceBuilder {
    private final LexemeSequenceImpl seq;

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
    public LexemeSequenceBuilder appendAll(final List<Lexeme> lexemes) {
        if (lexemes != null) {
            for (final Lexeme l : lexemes) {
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

    @Override
    public Optional<Lexeme> getLast() {
        return Optional.ofNullable(this.seq.getLastLexeme());
    }
}
