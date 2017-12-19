package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.HashMap;
import java.util.Map;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeVisitor;

class LexemeImpl implements Lexeme {
    private Identity id;
    private String tacToken;
    private Status status;
    private String lexerMessage;
    private boolean isSynthetic;
    private Map<ParsedValueName, Object> parsedValues;
    private int startIndex = -1;
    private int endIndex = -1;
    private double certainty = 0.0d;

    //Lexing navigation:
    private Lexeme first;
    private Lexeme next;
    private Lexeme prev;

    LexemeImpl(final Lexeme lexeme) {
        this.tacToken = lexeme.getTACToken();
        this.id = lexeme.getIdentity();
        this.status = lexeme.getStatus();
        this.lexerMessage = lexeme.getLexerMessage();
        this.isSynthetic = lexeme.isSynthetic();
        this.parsedValues = new HashMap<>(lexeme.getParsedValues());
        this.startIndex = lexeme.getStartIndex();
        this.endIndex = lexeme.getEndIndex();
        this.certainty = lexeme.getIdentificationCertainty();
    }

    LexemeImpl(final String token) {
        this(token, null, Status.UNRECOGNIZED);
    }

    LexemeImpl(final String token, final Identity identity) {
        this(token, identity, Status.OK);
    }

    LexemeImpl(final String token, final Identity identity, final Status status) {
        this.tacToken = token;
        this.id = identity;
        this.status = status;
        this.isSynthetic = false;
        this.parsedValues = new HashMap<>();
    }

    @Override
    public Identity getIdentity() {
        return this.id;
    }

    @Override
    public Identity getIdentityIfAcceptable() throws IllegalStateException {
        if (Status.OK == this.status || Status.WARNING == this.status) {
            return this.id;
        } else {
            return null;
        }
    }

    @Override
    public Status getStatus() {
        return this.status;
    }

    @Override
    public String getLexerMessage() {
        return this.lexerMessage;
    }

    @Override
    public int getStartIndex() {
        return this.startIndex;
    }

    @Override
    public int getEndIndex() {
        return this.endIndex;
    }

    @SuppressWarnings("unchecked")
    public <T> T getParsedValue(ParsedValueName name, Class<T> clz) {
        if (this.id == null) {
            return null;
        } else {
            if (!this.id.canStore(name)) {
                throw new IllegalArgumentException("Lexeme of identity " + this.id + " can never contain parsed value " + name + ", you should fix your code");
            }
        }
        Object val = this.parsedValues.get(name);
        if (val != null) {
            if (clz.isAssignableFrom(val.getClass())) {
                return (T) val;
            } else {
                throw new ClassCastException("Cannot return value of type " + val.getClass() + " as " + clz);
            }
        } else {
            return null;
        }
    }

    @Override
    public Map<ParsedValueName, Object> getParsedValues() {
        return this.parsedValues;
    }

    @Override
    public String getTACToken() {
        return this.tacToken;
    }

    @Override
    public Lexeme getFirst() {
        return this.first;
    }

    @Override
    public Lexeme getPrevious(boolean acceptWhitespace) {
        return this.prev;
    }

    @Override
    public Lexeme getPrevious() {
        Lexeme retval = this.prev;
        while (retval != null && Identity.WHITE_SPACE == retval.getIdentity()) {
            retval = retval.getPrevious();
        }
        return retval;
    }

    @Override
    public Lexeme getNext(boolean acceptWhitespace) {
        return this.next;
    }

    @Override
    public Lexeme getNext() {
        Lexeme retval = this.next;
        while (retval != null && Identity.WHITE_SPACE == retval.getIdentity()) {
            retval = retval.getNext();
        }
        return retval;
    }

    @Override
    public boolean hasPrevious() {
        return this.getPrevious() != null;
    }

    @Override
    public boolean hasPrevious(boolean acceptWhitespace) {
        return this.prev != null;
    }

    @Override
    public boolean hasNext() {
        return this.getNext() != null;
    }

    @Override
    public boolean hasNext(boolean acceptWhitespace) {
        return this.next != null;
    }

    @Override
    public boolean isSynthetic() {
        return isSynthetic;
    }

    @Override
    public boolean isRecognized() {
        return !Status.UNRECOGNIZED.equals(this.status);
    }

    @Override
    public double getIdentificationCertainty() {
        return this.certainty;
    }

    @Override
    public void identify(final Identity id, final double certainty) {
        identify(id, Status.OK, null, certainty);
    }

    @Override
    public void identify(final Identity id, final Status status, final double certainty) {
        identify(id, status, null, certainty);
    }

    @Override
    public void identify(final Identity id, final Status status, final String note, final double certainty) {
        this.id = id;
        this.status = status;
        this.lexerMessage = note;
        this.setIdentificationCertainty(certainty);
    }

    @Override
    public void identify(final Identity id) {
        identify(id, Status.OK, null, 1.0);
    }

    @Override
    public void identify(final Identity id, final Status status) {
        identify(id, status, null, 1.0);
    }

    @Override
    public void identify(final Identity id, final Status status, final String note) {
        identify(id, status, note, 1.0);
    }

    @Override
    public void setStatus(final Status status) {
        this.status = status;
    }

    @Override
    public void setSynthetic(final boolean synthetic) {
        isSynthetic = synthetic;
    }

    @Override
    public void setParsedValue(ParsedValueName name, Object value) {
        if (this.id != null) {
            if (!this.id.canStore(name)) {
                throw new IllegalArgumentException(this.id + " can only store " + id.getPossibleNames());
            }
            this.parsedValues.put(name, value);
        } else {
            throw new IllegalStateException("Cannot set parsed value before identifying Lexeme");
        }
    }

    @Override
    public void setLexerMessage(final String msg) {
        this.lexerMessage = msg;
    }

    @Override
    public void setIdentificationCertainty(final double percentage) {
        if (percentage < 0.0 || percentage > 1.0) {
            throw new IllegalArgumentException("Certainty must be between 0.0 and 1.0");
        }
        this.certainty = percentage;
    }

    void setStartIndex(final int index) {
        this.startIndex = index;
    }

    void setEndIndex(final int index) {
        this.endIndex = index;
    }

    @Override
    public void accept(final LexemeVisitor visitor, final ConversionHints hints) {
        //Always acccept:
        if (visitor != null) {
            visitor.visit(this, hints);
        }
    }

    void setFirst(final Lexeme token) {
        this.first = token;
    }

    void setNext(final Lexeme token) {
        this.next = token;
    }

    void setPrevious(final Lexeme token) {
        this.prev = token;
    }

    public String toString() {
        return "\'" + this.tacToken + "\'(" + this.id + "," + this.status + ")";
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final LexemeImpl lexeme = (LexemeImpl) o;

        if (isSynthetic != lexeme.isSynthetic) {
            return false;
        }
        if (id != lexeme.id) {
            return false;
        }
        if (status != lexeme.status) {
            return false;
        }
        return lexerMessage != null ? lexerMessage.equals(lexeme.lexerMessage) : lexeme.lexerMessage == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (status != null ? status.hashCode() : 0);
        result = 31 * result + (lexerMessage != null ? lexerMessage.hashCode() : 0);
        result = 31 * result + (isSynthetic ? 1 : 0);
        return result;
    }
}