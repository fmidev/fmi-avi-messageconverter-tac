package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;

/**
 * Default LexingFactory implementation.
 */

public class LexingFactoryImpl implements LexingFactory {

    @Override
    public LexemeSequence createLexemeSequence(final String input, final ConversionHints hints) {
        LexemeSequenceImpl result = new LexemeSequenceImpl(input);
        appendArtifialStartTokenIfNecessary(input, result, hints);
        return result;
    }

    @Override
    public LexemeSequenceBuilder createLexemeSequenceBuilder() {
        return new LexemeSequenceBuilderImpl();
    }

    @Override
    public Lexeme createLexeme(final String token) {
        return new LexemeImpl(token);
    }

    @Override
    public Lexeme createLexeme(final String token, final Lexeme.Identity identity) {
        return new LexemeImpl(token, identity);
    }

    @Override
    public Lexeme createLexeme(final String token, final Lexeme.Identity identity, final Lexeme.Status status) {
        return new LexemeImpl(token, identity, status);
    }

    private static void appendArtifialStartTokenIfNecessary(final String input, final LexemeSequenceImpl result, final ConversionHints hints) {
        if (hints != null && hints.containsKey(ConversionHints.KEY_MESSAGE_TYPE)) {
            LexemeImpl artificialStartToken = null;
            if (hints.get(ConversionHints.KEY_MESSAGE_TYPE) == ConversionHints.VALUE_MESSAGE_TYPE_METAR && !input.startsWith("METAR ")) {
                artificialStartToken = new LexemeImpl("METAR", Lexeme.Identity.METAR_START);
            } else if (hints.get(ConversionHints.KEY_MESSAGE_TYPE) == ConversionHints.VALUE_MESSAGE_TYPE_SPECI && !input.startsWith("SPECI ")) {
                artificialStartToken = new LexemeImpl("SPECI", Lexeme.Identity.SPECI_START);
            } else if (hints.get(ConversionHints.KEY_MESSAGE_TYPE) == ConversionHints.VALUE_MESSAGE_TYPE_TAF && !input.startsWith("TAF ")) {
                artificialStartToken = new LexemeImpl("TAF", Lexeme.Identity.TAF_START);
            }
            if (artificialStartToken != null) {
                artificialStartToken.setSynthetic(true);
                result.addAsFirst(new LexemeImpl(" ", Lexeme.Identity.WHITE_SPACE));
                result.addAsFirst(artificialStartToken);
            }
        }
    }

    static class LexemeSequenceImpl implements LexemeSequence {

        private String originalTac;
        private LexemeImpl head;
        private LexemeImpl tail;

        LexemeSequenceImpl(final String originalTac) {
            if (originalTac != null) {
                this.constructFromTAC(originalTac);
            }
        }

        LexemeSequenceImpl() {
            this(null);
        }

        @Override
        public String getTAC() {
            if (this.originalTac != null) {
                return this.originalTac;
            } else {
                return this.getAsTAC();
            }
        }

        @Override
        public Lexeme getFirstLexeme() {
            return this.head;
        }

        @Override
        public Lexeme getLastLexeme() {
            return this.tail;
        }

        @Override
        public List<Lexeme> getLexemes() {
            return this.getLexemes(false);
        }

        @Override
        public List<Lexeme> getLexemes(final boolean acceptIgnored) {
            final List<Lexeme> retval = new ArrayList<>();
            LexemeImpl l = this.head;
            while (l != null) {
                retval.add(l);
                l = l.getNextImpl(acceptIgnored, true);
            }
            return Collections.unmodifiableList(retval);
        }

        @Override
        public List<LexemeSequence> splitBy(final Lexeme.Identity... ids) {
            return this.splitBy(true, ids);
        }

        @Override
        public List<LexemeSequence> splitBy(final boolean separatorStartsSequence, final Lexeme.Identity... ids) {
            final List<LexemeSequence> retval = new ArrayList<>();
            LexemeSequenceBuilder builder = new LexemeSequenceBuilderImpl();
            LexemeImpl l = this.head;
            boolean matchFound = false;
            while (l != null) {
                matchFound = false;
                for (final Lexeme.Identity toMatch : ids) {
                    if (toMatch == l.getIdentity()) {
                        matchFound = true;
                        if (!separatorStartsSequence) {
                            builder.append(l);
                        }
                        //Do not produce empty sequences
                        if (!builder.isEmpty()) {
                            retval.add(builder.build());
                            builder = new LexemeSequenceBuilderImpl();
                        }
                        break;
                    }
                }
                if (!matchFound) {
                    builder.append(l);
                } else if (separatorStartsSequence) {
                    builder.append(l);
                }
                l = l.getNextImpl(true, true);
            }
            if (!builder.isEmpty()) {
                retval.add(builder.build());
            }
            return retval;
        }

        /**
         * Trims any white space from the beginning and end of this sequence.
         *
         * @return the same sequence trimmed
         */
        @Override
        public LexemeSequence trimWhiteSpace() {
            final LexemeSequenceBuilder builder = new LexemeSequenceBuilderImpl();
            Lexeme l = this.head;
            while (l != null && Lexeme.Identity.WHITE_SPACE == l.getIdentity()) {
                l = l.getNext();
            }
            if (l != null) {
                builder.append(l)
                        .appendAll(l.getTailSequence().getLexemes());
            }
            Optional<Lexeme> last = builder.getLast();
            while (last.isPresent() && Lexeme.Identity.WHITE_SPACE == last.get().getIdentity()) {
                builder.removeLast();
                last = builder.getLast();
            }
            return builder.build();
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            LexemeImpl l = this.head;
            while (l != null) {
                sb.append('[');
                sb.append(l);
                sb.append(']');
                l = l.getNextImpl(true, true);
            }
            return sb.toString();
        }

        Lexeme replaceFirstWith(final LexemeImpl replacement) {
            if (replacement == null) {
                throw new NullPointerException();
            }
            if (this.head == null) {
                throw new IllegalStateException("No first lexeme to replace");
            }
            final Lexeme oldFirst = this.head;
            this.addAsFirst(replacement);
            if (oldFirst.isSynthetic() && !replacement.isSynthetic()) {
                //add the full length
                this.adjustIndexes(1, replacement.getTACToken().length() + 1);
            } else if (!oldFirst.isSynthetic() && replacement.isSynthetic()) {
                //cut the full length
                this.adjustIndexes(1, -(oldFirst.getTACToken().length() + 1));
            } else if (!oldFirst.isSynthetic() && !replacement.isSynthetic()) {
                //adjust by the length difference
                this.adjustIndexes(1, replacement.getTACToken().length() - oldFirst.getTACToken().length());
            }
            return oldFirst;
        }

        Lexeme replaceLastWith(final LexemeImpl replacement) {
            if (replacement == null) {
                throw new NullPointerException();
            }
            if (this.tail == null) {
                throw new IllegalStateException("No last lexeme to replace");
            }
            final LexemeImpl oldLast = this.tail;
            final LexemeImpl prev = this.tail.getPreviousImpl(true, true);
            this.tail = replacement;
            if (oldLast == this.head) {
                //Replacing the only token
                this.head = replacement;
                this.tail.setFirst(this.head);
            } else {
                this.tail.setFirst(oldLast.first);
            }
            if (prev != null) {
                this.tail.setPrevious(prev);
                prev.setNext(this.tail);
            }
            return oldLast;
        }

        void addAsFirst(final LexemeImpl toAdd) {
            if (toAdd != null) {
                final LexemeImpl oldFirst = this.head;
                if (oldFirst != null) {
                    oldFirst.setPrevious(toAdd);
                    toAdd.setNext(oldFirst);
                } else {
                    this.tail = toAdd;
                }
                toAdd.setPrevious(null);
                this.head = toAdd;
                this.updateLinksToFirst();
                if (!toAdd.isSynthetic()) {
                    //Assume a single white space token separator:
                    this.adjustIndexes(1, toAdd.getTACToken().length() + 1);
                }
            }
        }

        void addAsLast(final LexemeImpl toAdd) {
            if (toAdd != null) {
                if (this.head != null) {
                    final LexemeImpl oldLast = this.tail;
                    oldLast.setNext(toAdd);
                    toAdd.setPrevious(oldLast);
                    toAdd.setFirst(this.head);
                } else {
                    this.head = toAdd;
                    toAdd.setFirst(toAdd);
                    toAdd.setPrevious(null);
                }
                toAdd.setNext(null);
                this.tail = toAdd;
            }
        }

        Lexeme removeFirst() {
            final Lexeme removed = this.head;
            if (this.head.hasNext(true)) {
                this.head = this.head.getNextImpl(true, true);
                this.head.setPrevious(null);
            }
            this.updateLinksToFirst();
            if (!removed.isSynthetic()) {
                this.adjustIndexes(0, -(removed.getTACToken().length() + 1));
            }
            return removed;
        }

        Lexeme removeLast() {
            final Lexeme removed = this.tail;
            if (this.tail.hasPrevious(true)) {
                this.tail = this.tail.getPreviousImpl(true, true);
                this.tail.setNext(null);
            }
            return removed;
        }

        private void updateLinksToFirst() {
            final LexemeImpl first = this.head;
            LexemeImpl l = this.head;
            while (l != null) {
                l.setFirst(first);
                l = l.getNextImpl(true, true);
            }
        }

        private void adjustIndexes(final int fromIndex, final int by) {
            LexemeImpl l = this.head;
            int index = -1;
            while (l != null) {
                if (index >= fromIndex) {
                    l.setStartIndex(l.getStartIndex() + by);
                    l.setEndIndex(l.getEndIndex() + by);
                }
                l = l.getNextImpl(true, true);
                index++;
            }
        }

        private void constructFromTAC(final String tac) {
            if (tac != null && tac.length() > 0) {
                final Pattern horVisFractionNumberPart1Pattern = Pattern.compile("^[0-9]*$");
                final Pattern horVisFractionNumberPart2Pattern = Pattern.compile("^[0-9]*/[0-9]*[A-Z]{2}$");
                // Windshear token for a particular runway has changed between 16th and 19th edition of Annex 3
                //  16th = "WS RWYnn[LRC]"
                //  19th = "WS Rnn[LRC]"
                final Pattern windShearRunwayPattern = Pattern.compile("^R(?:WY)?([0-9]{2})?[LRC]?$");
                final StringTokenizer st = new StringTokenizer(tac, " \n\t\r\f", true);
                String lastToken = null;
                String lastLastToken = null;
                boolean inWhitespace = false;
                int start = 0;
                LexemeImpl l;
                while (st.hasMoreTokens()) {
                    String s = st.nextToken();
                    start = tac.indexOf(s, start);
                    //Whitespace only:
                    if (s.matches("\\s")) {
                        if (inWhitespace) {
                            //combine with the preceding whitespace:
                            //Note: identify already here because Lexeme.getPrevious() and getNext need identified whitespace lexemes:
                            final int startIndex = this.getLastLexeme().getStartIndex();
                            final String token = this.getLastLexeme().getTACToken() + s;
                            final int endIndex = startIndex + token.length() - 1;
                            l = new LexemeImpl(token, Lexeme.Identity.WHITE_SPACE);
                            l.setStartIndex(startIndex);
                            l.setEndIndex(endIndex);
                            this.replaceLastWith(l);
                        } else {
                            //Note: identify already here because Lexeme.getPrevious() and getNext need identified whitespace lexemes:
                            l = new LexemeImpl(s, Lexeme.Identity.WHITE_SPACE);
                            l.setStartIndex(start);
                            l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                            this.addAsLast(l);
                            inWhitespace = true;
                        }
                    } else {
                        inWhitespace = false;
                        if (s.endsWith("=")) {
                            //first create the last token before the end:
                            l = new LexemeImpl(s.substring(0, s.length() - 1));
                            l.setStartIndex(start);
                            l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                            this.addAsLast(l);

                            //..and then the end token:
                            l = new LexemeImpl("=", Lexeme.Identity.END_TOKEN);
                            l.setStartIndex(start + s.length() - 1);
                            l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                            this.addAsLast(l);

                        } else if (lastToken != null && horVisFractionNumberPart2Pattern.matcher(s).matches() && horVisFractionNumberPart1Pattern.matcher(lastToken)
                                .matches()) {
                            // cases like "1 1/8SM", combine the two tokens:
                            l = combineThisAndPrevToken(lastToken, s);

                        } else if ("WS".equals(lastLastToken) && "ALL".equals(lastToken) && windShearRunwayPattern.matcher(s).matches()) {
                            // "WS ALL RWY" case: concat all three parts as the last token:
                            //last is a whitespace now, so need to remove it first:
                            this.removeLast(); // space
                            this.removeLast(); // ALL
                            this.removeLast(); // space
                            l = new LexemeImpl("WS ALL RWY");
                            l.setStartIndex(this.getLastLexeme().getStartIndex());
                            l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                            this.replaceLastWith(l);
                        } else if ("WS".equals(lastToken) && windShearRunwayPattern.matcher(s).matches()) {
                            // "WS RWY22L" case, concat the two parts as the last token:
                            l = new LexemeImpl("WS " + s);
                            //last is a whitespace now, so need to remove it first:
                            this.removeLast();
                            l.setStartIndex(this.getLastLexeme().getStartIndex());
                            l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                            this.replaceLastWith(l);
                        } else if (("PROB30".equals(lastToken) || "PROB40".equals(lastToken)) && ("TEMPO".equals(s))) {
                            l = combineThisAndPrevToken(lastToken, s);
                        } else if ("LOW".equals(lastToken) && "WIND".equals(s)) {
                            l = combineThisAndPrevToken(lastToken, s);
                        } else if ("WX".equals(lastToken) && "WRNG".equals(s)) {
                            l = combineThisAndPrevToken(lastToken, s);
                        } else {
                            l = new LexemeImpl(s);
                            l.setStartIndex(start);
                            l.setEndIndex(start + l.getTACToken().length() - 1);
                            this.addAsLast(l);
                        }
                        lastToken = l.getTACToken();
                        if (l.getPrevious() != null) {
                            lastLastToken = l.getPrevious().getTACToken();
                        } else {
                            lastLastToken = null;
                        }
                    }
                    start += s.length();
                }
                this.originalTac = tac;
            }
        }

        private LexemeImpl combineThisAndPrevToken(final String lastToken, final String currentToken) {
            LexemeImpl l = new LexemeImpl(lastToken + " " + currentToken);
            this.removeLast();
            l.setStartIndex(this.getLastLexeme().getStartIndex());
            l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
            this.replaceLastWith(l);
            return l;
        }

        private String getAsTAC() {
            StringBuilder sb = new StringBuilder();
            LexemeImpl l = this.head;
            while (l != null) {
                sb.append(l.getTACToken());
                l = l.getNextImpl(true, true);
            }
            return sb.toString();
        }

    }

    static class LexemeSequenceBuilderImpl implements LexemeSequenceBuilder {
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
            if (this.seq.getLexemes(true).size() > 0) {
                this.seq.removeLast();
            }
            return this;
        }

        @Override
        public Optional<Lexeme> getLast() {
            return Optional.ofNullable(this.seq.getLastLexeme());
        }

        @Override
        public boolean isEmpty() {
            return this.seq.getLexemes().isEmpty();
        }
    }

    static class LexemeImpl implements Lexeme {
        private Identity id;
        private final String tacToken;
        private Status status;
        private String lexerMessage;
        private boolean isSynthetic;
        private boolean explicitlyIgnored;
        private final Map<ParsedValueName, Object> parsedValues;
        private int startIndex = -1;
        private int endIndex = -1;
        private double certainty = 0.0d;
        //Lexing navigation:
        private LexemeImpl first;
        private LexemeImpl next;
        private LexemeImpl prev;

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
            //this.lexingFactory = lexingFactory;
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
        @Override
        public <T> T getParsedValue(final ParsedValueName name, final Class<T> clz) {
            if (this.id == null) {
                return null;
            } else {
                if (!this.id.canStore(name)) {
                    throw new IllegalArgumentException("Lexeme of identity " + this.id + " can never contain parsed value " + name + ", you should fix your code");
                }
            }
            final Object val = this.parsedValues.get(name);
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
            return Collections.unmodifiableMap(this.parsedValues);
        }

        @Override
        public String getTACToken() {
            return this.tacToken;
        }

        @Override
        public Lexeme getFirst(final boolean acceptIgnoredAndWhitespace) {
            Lexeme retval = this.first;
            if (!acceptIgnoredAndWhitespace) {
                if (retval != null && (Identity.WHITE_SPACE == retval.getIdentity() || retval.isIgnored())) {
                    retval = retval.getNext();
                }
            }
            return retval;
        }

        @Override
        public Lexeme getFirst() {
            return this.getFirst(false);
        }

        @Override
        public Lexeme getPrevious() {
            return this.getPrevious(false);
        }

        @Override
        public Lexeme getPrevious(final boolean acceptIgnoredAndWhitespace) {
            if (acceptIgnoredAndWhitespace) {
                return this.getPreviousImpl(true, true);
            } else {
                return this.getPreviousImpl(false, false);
            }
        }

        LexemeImpl getPreviousImpl(final boolean acceptIgnored, final boolean acceptWhitespace) {
            LexemeImpl retval = this.prev;
            if (!acceptIgnored || !acceptWhitespace) {
                boolean notAcceptable = true;
                while (retval != null && notAcceptable) {
                    notAcceptable = false;
                    if (!acceptIgnored && retval.isIgnored()) {
                        notAcceptable = true;
                    }
                    if (!acceptWhitespace && Identity.WHITE_SPACE == retval.getIdentity()) {
                        notAcceptable = true;
                    }
                    if (notAcceptable) {
                        retval = retval.prev;
                    }
                }
            }
            return retval;
        }

        @Override
        public Lexeme getNext() {
            return this.getNext(false);
        }

        @Override
        public Lexeme getNext(final boolean acceptIgnoredAndWhitespace) {
            if (acceptIgnoredAndWhitespace) {
                return this.getNextImpl(true, true);
            } else {
                return this.getNextImpl(false, false);
            }
        }

        LexemeImpl getNextImpl(final boolean acceptIgnored, final boolean acceptWhitespace) {
            LexemeImpl retval = this.next;
            if (!acceptIgnored || !acceptWhitespace) {
                boolean notAcceptable = true;
                while (retval != null && notAcceptable) {
                    notAcceptable = false;
                    if (!acceptIgnored && retval.isIgnored()) {
                        notAcceptable = true;
                    }
                    if (!acceptWhitespace && Identity.WHITE_SPACE == retval.getIdentity()) {
                        notAcceptable = true;
                    }
                    if (notAcceptable) {
                        retval = retval.next;
                    }
                }
            }
            return retval;
        }


        @Override
        public boolean hasPrevious() {
            return this.hasPrevious(false);
        }

        @Override
        public boolean hasPrevious(final boolean acceptIgnored) {
            return this.getPrevious(acceptIgnored) != null;
        }

        @Override
        public boolean hasNext(final boolean acceptIgnored) {
            return this.getNext(acceptIgnored) != null;
        }

        @Override
        public boolean hasNext() {
            return this.hasNext(false);
        }

        @Override
        public LexemeSequence getTailSequence() throws IllegalStateException {
            final LexemeSequenceBuilder lsb = new LexemeSequenceBuilderImpl();
            LexemeImpl l = this.next;
            while (l != null) {
                lsb.append(l);
                l = l.next;
            }
            return lsb.build();
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
        public boolean isIgnored() {
            return explicitlyIgnored;
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

        public void setStatus(final Status status) {
            this.status = status;
        }

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

        /**
         * Sets the confidence of the Lexeme identification.
         *
         * @param percentage
         *         between 0.0 and 1.0
         *
         * @see #getIdentificationCertainty()
         */

        public void setIdentificationCertainty(final double percentage) {
            if (percentage < 0.0 || percentage > 1.0) {
                throw new IllegalArgumentException("Certainty must be between 0.0 and 1.0");
            }
            this.certainty = percentage;
        }

        /**
         * Setting to set this Lexeme as ignored or not.
         *
         * @param explicitlyIgnored
         *         true if to be ignored
         */
        public void setIgnored(final boolean explicitlyIgnored) {
            this.explicitlyIgnored = explicitlyIgnored;
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

        @Override
        public Lexeme findNext(final Lexeme.Identity needle) {
            return findNext(needle, null, null);
        }

        @Override
        public Lexeme findNext(final Lexeme.Identity needle, final Consumer<Lexeme> found) {
            return findNext(needle, found, null);
        }

        @Override
        public Lexeme findNext(final Lexeme.Identity needle, final Consumer<Lexeme> found, final LexemeParsingNotifyer notFound) {
            Lexeme retval = null;
            Lexeme current = this.getNext();
            if (current != null) {
                boolean stop = false;
                Lexeme.Identity currentId;
                while (!stop) {
                    currentId = current.getIdentityIfAcceptable();
                    if (needle == null || currentId == needle) {
                        retval = current;
                    }
                    stop = !current.hasNext() || retval != null;
                    current = current.getNext();
                }
            }
            if (retval != null) {
                if (found != null) {
                    found.accept(retval);
                }
            } else {
                if (notFound != null) {
                    notFound.ping();
                }
            }
            return retval;
        }

        void setFirst(final LexemeImpl token) {
            this.first = token;
        }

        void setNext(final LexemeImpl token) {
            this.next = token;
        }

        void setPrevious(final LexemeImpl token) {
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

}
