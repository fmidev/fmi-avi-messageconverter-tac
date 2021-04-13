package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.function.Consumer;
import java.util.function.Predicate;

import fi.fmi.avi.converter.ConversionHints;
import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeIdentity;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;
import fi.fmi.avi.converter.tac.lexer.LexemeSequenceBuilder;
import fi.fmi.avi.converter.tac.lexer.LexemeVisitor;
import fi.fmi.avi.converter.tac.lexer.LexingFactory;
import fi.fmi.avi.model.MessageType;

/**
 * Default LexingFactory implementation.
 */

public class LexingFactoryImpl implements LexingFactory {

    private static final String TAC_DELIMS = Arrays.stream(Lexeme.MeteorologicalBulletinSpecialCharacter.values())
            .map(Lexeme.MeteorologicalBulletinSpecialCharacter::getContent)
            .collect(StringBuilder::new, StringBuilder::append, StringBuilder::append)
            .append("=")
            .toString();

    private final List<List<Predicate<String>>> tokenCombiningRules = new ArrayList<>();

    private final Map<MessageType, Lexeme> startTokens = new HashMap<>();

    public void addTokenCombiningRule(final List<Predicate<String>> rule) {
        this.tokenCombiningRules.add(rule);
    }

    public void setMessageStartToken(final MessageType type, final Lexeme token) {
        this.startTokens.put(type, token);
    }

    @Override
    public List<List<Predicate<String>>> getTokenCombiningRules() {
        return this.tokenCombiningRules;
    }

    @Override
    public LexemeSequence createLexemeSequence(final String input, final ConversionHints hints) {
        final LexemeSequenceImpl result = new LexemeSequenceImpl(this, input);
        appendArtifialStartTokenIfNecessary(input, result, hints);
        return result;
    }

    @Override
    public LexemeSequenceBuilder createLexemeSequenceBuilder() {
        return new LexemeSequenceBuilderImpl(this);
    }

    @Override
    public Lexeme createLexeme(final String token) {
        return new LexemeImpl(this, token);
    }

    @Override
    public Lexeme createLexeme(final String token, final LexemeIdentity identity) {
        return new LexemeImpl(this, token, identity);
    }

    @Override
    public Lexeme createLexeme(final String token, final LexemeIdentity identity, final Lexeme.Status status) {
        return new LexemeImpl(this, token, identity, status);
    }

    public Lexeme createLexeme(final String token, final LexemeIdentity identity, final Lexeme.Status status, final boolean synthetic) {
        final LexemeImpl l = new LexemeImpl(this, token, identity, status);
        l.setSynthetic(synthetic);
        return l;
    }

    private void appendArtifialStartTokenIfNecessary(final String input, final LexemeSequenceImpl result, final ConversionHints hints) {
        if (hints != null && hints.containsKey(ConversionHints.KEY_MESSAGE_TYPE)) {
            final Lexeme artificialStartToken = this.startTokens.get(hints.get(ConversionHints.KEY_MESSAGE_TYPE));
            if (artificialStartToken != null) {
                if (!input.startsWith(artificialStartToken.getTACToken() + " ") && !input.startsWith(artificialStartToken.getTACToken() + "\n")) {
                    result.addAsFirst(new LexemeImpl(this, Lexeme.MeteorologicalBulletinSpecialCharacter.SPACE));
                    result.addAsFirst(artificialStartToken);
                }
            }
        }
    }

    static class LexemeSequenceImpl implements LexemeSequence {

        private final LexingFactory factory;
        private String originalTac;
        private LexemeImpl head;
        private LexemeImpl tail;

        LexemeSequenceImpl(final LexingFactory factory, final String originalTac) {
            this.factory = factory;
            if (originalTac != null) {
                this.constructFromTAC(originalTac);
            }
        }

        LexemeSequenceImpl(final LexingFactory factory) {
            this(factory, null);
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
        public List<LexemeSequence> splitBy(final LexemeIdentity... ids) {
            return this.splitBy(true, ids);
        }

        @Override
        public List<LexemeSequence> splitBy(final boolean separatorStartsSequence, final LexemeIdentity... ids) {
            final List<LexemeSequence> retval = new ArrayList<>();
            LexemeSequenceBuilder builder = new LexemeSequenceBuilderImpl(this.factory);
            LexemeImpl l = this.head;
            boolean matchFound = false;
            while (l != null) {
                matchFound = false;
                for (final LexemeIdentity toMatch : ids) {
                    if (toMatch.equals(l.getIdentity())) {
                        matchFound = true;
                        if (!separatorStartsSequence) {
                            builder.append(l);
                        }
                        //Do not produce empty sequences
                        if (!builder.isEmpty()) {
                            retval.add(builder.build());
                            builder = new LexemeSequenceBuilderImpl(this.factory);
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
            final LexemeSequenceBuilder builder = new LexemeSequenceBuilderImpl(this.factory);
            Lexeme l = this.head;
            while (l != null && LexemeIdentity.WHITE_SPACE.equals(l.getIdentity())) {
                l = l.getNext();
            }
            if (l != null) {
                builder.append(l).appendAll(l.getTailSequence().getLexemes());
            }
            Optional<Lexeme> last = builder.getLast();
            while (last.isPresent() && LexemeIdentity.WHITE_SPACE.equals(last.get().getIdentity())) {
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

        void addAsFirst(final Lexeme toAdd) {
            this.addAsFirst(new LexemeImpl(this.factory, toAdd));
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

        LexemeImpl removeLast() {
            final LexemeImpl removed = this.tail;
            if (this.tail != null) {
                if (this.tail.hasPrevious(true)) {
                    this.tail = this.tail.getPreviousImpl(true, true);
                    this.tail.setNext(null);
                } else {
                    this.head = null;
                    this.tail = null;
                }
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
                final StringTokenizer st = new StringTokenizer(tac, TAC_DELIMS, true);
                int start = 0;
                while (st.hasMoreTokens()) {
                    final String s = st.nextToken();
                    start = tac.indexOf(s, start);
                    //Special chars or space:
                    final Lexeme.MeteorologicalBulletinSpecialCharacter specialCharacter = Lexeme.MeteorologicalBulletinSpecialCharacter.fromChar(s.charAt(0));
                    if (s.length() == 1 && specialCharacter != null) {
                        final LexemeImpl l = new LexemeImpl(this.factory, s, LexemeIdentity.WHITE_SPACE);
                        l.setStartIndex(start);
                        l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                        l.setParsedValue(Lexeme.ParsedValueName.TYPE, specialCharacter);
                        this.addAsLast(l);
                    } else {
                        if ("=".equals(s)) {
                            final LexemeImpl l = new LexemeImpl(this.factory, "=", LexemeIdentity.END_TOKEN);
                            l.setStartIndex(start);
                            l.setEndIndex(start);
                            this.addAsLast(l);
                        } else {
                            final LexemeImpl l = new LexemeImpl(this.factory, s);
                            l.setStartIndex(start);
                            l.setEndIndex(start + l.getTACToken().length() - 1);
                            this.addAsLast(l);
                        }
                        if (this.tail.hasPrevious()) {
                            for (final List<Predicate<String>> combiningRule : this.factory.getTokenCombiningRules()) {
                                this.combinePrevMatchingTokens(combiningRule);
                            }
                        }
                    }
                    start += s.length();
                }
            }
            this.originalTac = tac;
        }

        private void combinePrevMatchingTokens(final List<Predicate<String>> toMatch) {
            LexemeImpl l = this.tail;
            int index = toMatch.size() - 1;
            boolean match = false;
            while (index >= 0 && l != null) {
                if (!toMatch.get(index).test(l.getTACToken())) {
                    break;
                }
                if (index == 0) {
                    match = true;
                }
                l = l.getPreviousImpl(false, false);
                index--;
            }
            if (match) {
                final StringBuilder sb = new StringBuilder();
                Lexeme firstCombined = null;
                LexemeImpl preceedingToken = null;
                for (int i = 0; i < toMatch.size(); i++) {
                    firstCombined = this.removeLast();
                    sb.insert(0, firstCombined.getTACToken()); //the last token
                    preceedingToken = this.removeLast();
                    if (i < toMatch.size() - 1) {
                        sb.insert(0, preceedingToken.getTACToken()); //white-space before the last
                    }
                }
                if (firstCombined != null) {
                    if (preceedingToken != null) {
                        this.addAsLast(preceedingToken);
                    }
                    final String content = sb.toString();
                    final LexemeImpl token = new LexemeImpl(this.factory, content);
                    token.setStartIndex(firstCombined.getStartIndex());
                    token.setEndIndex(firstCombined.getStartIndex() + content.length() - 1);
                    this.addAsLast(token);
                }
            }
        }

        private String getAsTAC() {
            final StringBuilder sb = new StringBuilder();
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
        private final LexingFactory factory;

        LexemeSequenceBuilderImpl(final LexingFactory factory) {
            this.factory = factory;
            seq = new LexemeSequenceImpl(factory);
        }

        @Override
        public LexemeSequenceBuilder append(final Lexeme lexeme) {
            this.seq.addAsLast(new LexemeImpl(this.factory, lexeme));
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
                    this.seq.addAsLast(new LexemeImpl(this.factory, l));
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
        private final LexingFactory factory;
        private final String tacToken;
        private final Map<ParsedValueName, Object> parsedValues;
        private LexemeIdentity id;
        private Status status;
        private String lexerMessage;
        private boolean isSynthetic;
        private boolean explicitlyIgnored;
        private int startIndex = -1;
        private int endIndex = -1;
        private double certainty = 0.0d;
        //Lexing navigation:
        private LexemeImpl first;
        private LexemeImpl next;
        private LexemeImpl prev;

        LexemeImpl(final LexingFactory factory, final Lexeme lexeme) {
            this.factory = factory;
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

        LexemeImpl(final LexingFactory factory, final String token) {
            this(factory, token, null, Status.UNRECOGNIZED);
        }

        LexemeImpl(final LexingFactory factory, final String token, final LexemeIdentity identity) {
            this(factory, token, identity, Status.OK);
        }

        LexemeImpl(final LexingFactory factory, final MeteorologicalBulletinSpecialCharacter value) {
            this(factory, value.getContent(), LexemeIdentity.WHITE_SPACE, Status.OK);
        }

        LexemeImpl(final LexingFactory factory, final String token, final LexemeIdentity identity, final Status status) {
            this.factory = factory;
            this.tacToken = token;
            this.id = identity;
            this.status = status;
            this.isSynthetic = false;
            this.parsedValues = new HashMap<>();
        }

        @Override
        public LexemeIdentity getIdentity() {
            return this.id;
        }

        @Override
        public LexemeIdentity getIdentityIfAcceptable() throws IllegalStateException {
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

        public void setStatus(final Status status) {
            this.status = status;
        }

        @Override
        public String getLexerMessage() {
            return this.lexerMessage;
        }

        @Override
        public void setLexerMessage(final String msg) {
            this.lexerMessage = msg;
        }

        @Override
        public int getStartIndex() {
            return this.startIndex;
        }

        void setStartIndex(final int index) {
            this.startIndex = index;
        }

        @Override
        public int getEndIndex() {
            return this.endIndex;
        }

        void setEndIndex(final int index) {
            this.endIndex = index;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T getParsedValue(final ParsedValueName name, final Class<T> clz) {
            if (this.id == null) {
                return null;
            } else {
                if (!this.id.canStore(name)) {
                    throw new IllegalArgumentException(
                            "Lexeme of identity " + this.id + " can never contain parsed value " + name + ", you should fix your code");
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
                while (retval != null && (LexemeIdentity.WHITE_SPACE.equals(retval.getIdentity()) || retval.isIgnored())) {
                    retval = retval.getNext();
                }
            }
            return retval;
        }

        @Override
        public Lexeme getFirst() {
            return this.getFirst(false);
        }

        void setFirst(final LexemeImpl token) {
            this.first = token;
        }

        @Override
        public Lexeme getPrevious() {
            return this.getPrevious(false);
        }

        void setPrevious(final LexemeImpl token) {
            this.prev = token;
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
                    notAcceptable = !acceptIgnored && retval.isIgnored();
                    if (!acceptWhitespace && LexemeIdentity.WHITE_SPACE.equals(retval.getIdentity())) {
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

        void setNext(final LexemeImpl token) {
            this.next = token;
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
                    notAcceptable = !acceptIgnored && retval.isIgnored();
                    if (!acceptWhitespace && LexemeIdentity.WHITE_SPACE.equals(retval.getIdentity())) {
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
            final LexemeSequenceBuilder lsb = new LexemeSequenceBuilderImpl(this.factory);
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

        public void setSynthetic(final boolean synthetic) {
            isSynthetic = synthetic;
        }

        @Override
        public boolean isRecognized() {
            return !Status.UNRECOGNIZED.equals(this.status);
        }

        @Override
        public double getIdentificationCertainty() {
            return this.certainty;
        }

        /**
         * Sets the confidence of the Lexeme identification.
         *
         * @param percentage
         *         between 0.0 and 1.0
         *
         * @see #getIdentificationCertainty()
         */

        @Override
        public void setIdentificationCertainty(final double percentage) {
            if (percentage < 0.0 || percentage > 1.0) {
                throw new IllegalArgumentException("Certainty must be between 0.0 and 1.0");
            }
            this.certainty = percentage;
        }

        @Override
        public boolean isIgnored() {
            return explicitlyIgnored;
        }

        /**
         * Setting to set this Lexeme as ignored or not.
         *
         * @param explicitlyIgnored
         *         true if to be ignored
         */
        @Override
        public void setIgnored(final boolean explicitlyIgnored) {
            this.explicitlyIgnored = explicitlyIgnored;
        }

        @Override
        public void identify(final LexemeIdentity id, final double certainty) {
            identify(id, Status.OK, null, certainty);
        }

        @Override
        public void identify(final LexemeIdentity id, final Status status, final double certainty) {
            identify(id, status, null, certainty);
        }

        @Override
        public void identify(final LexemeIdentity id, final Status status, final String note, final double certainty) {
            this.id = id;
            this.status = status;
            this.lexerMessage = note;
            this.setIdentificationCertainty(certainty);
        }

        @Override
        public void identify(final LexemeIdentity id) {
            identify(id, Status.OK, null, 1.0);
        }

        @Override
        public void identify(final LexemeIdentity id, final Status status) {
            identify(id, status, null, 1.0);
        }

        @Override
        public void identify(final LexemeIdentity id, final Status status, final String note) {
            identify(id, status, note, 1.0);
        }

        @Override
        public void setParsedValue(final ParsedValueName name, final Object value) {
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
        public void accept(final LexemeVisitor visitor, final ConversionHints hints) {
            //Always acccept:
            if (visitor != null) {
                visitor.visit(this, hints);
            }
        }

        @Override
        public Lexeme findNext(final LexemeIdentity needle) {
            return findNext(needle, null, null);
        }

        @Override
        public Lexeme findNext(final LexemeIdentity needle, final Consumer<Lexeme> found) {
            return findNext(needle, found, null);
        }

        @Override
        public Lexeme findNext(final LexemeIdentity needle, final Consumer<Lexeme> found, final LexemeParsingNotifyer notFound) {
            Lexeme retval = null;
            Lexeme current = this.getNext();
            if (current != null) {
                boolean stop = false;
                LexemeIdentity currentId;
                while (!stop) {
                    currentId = current.getIdentityIfAcceptable();
                    if (needle == null || (currentId != null && currentId.equals(needle))) {
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

        public String toString() {
            return "'" + this.tacToken + "'(" + this.id + "," + this.status + ")";
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
            if (parsedValues.equals(lexeme.parsedValues)) {
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
            result = 31 * result + parsedValues.hashCode();
            return result;
        }
    }

}
