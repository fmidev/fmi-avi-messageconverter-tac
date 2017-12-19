package fi.fmi.avi.converter.tac.lexer.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import fi.fmi.avi.converter.tac.lexer.Lexeme;
import fi.fmi.avi.converter.tac.lexer.LexemeSequence;

class LexemeSequenceImpl implements LexemeSequence {

    private String originalTac;
    LinkedList<LexemeImpl> lexemes;

    public LexemeSequenceImpl(final String originalTac) {
        this();
        this.originalTac = originalTac;
        this.constructFromTAC();
    }

    public LexemeSequenceImpl() {
        this.lexemes = new LinkedList<LexemeImpl>();
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
        if (this.lexemes.size() > 0) {
            return this.lexemes.getFirst();
        } else {
            return null;
        }
    }

    @Override
    public Lexeme getLastLexeme() {
        if (this.lexemes.size() > 0) {
            return this.lexemes.getLast();
        } else {
            return null;
        }
    }

    @Override
    public List<Lexeme> getLexemes() {
        return Collections.unmodifiableList(this.lexemes);
    }

    @Override
    public List<LexemeSequence> splitBy(Lexeme.Identity...ids) {
        List<LexemeSequence> retval = new ArrayList<>();
        LexemeSequenceImpl seq = new LexemeSequenceImpl();
        for (LexemeImpl l:this.lexemes) {
            for (Lexeme.Identity toMatch:ids) {
                //Do not produce empty sequences
                if (toMatch == l.getIdentity()  && seq.lexemes.size() > 0) {
                    retval.add(seq);
                    seq = new LexemeSequenceImpl();
                    break;
                }
            }
            seq.addAsLast(l);
        }
        if (seq.lexemes.size() > 0) {
            retval.add(seq);
        }
        return retval;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.lexemes != null) {
            for (Lexeme l: this.lexemes.stream().filter(l -> Lexeme.Identity.WHITE_SPACE != l.getIdentity()).collect(Collectors.toList())) {
                sb.append('[');
                sb.append(l);
                sb.append(']');
            }
        }
        return sb.toString();
    }

    LexemeImpl replaceFirstWith(final LexemeImpl replacement) {
        if (replacement == null) {
            throw new NullPointerException();
        }
        if (this.lexemes.size() == 0) {
            throw new IllegalStateException("No first lexeme to replace");
        }
        LexemeImpl oldFirst = this.lexemes.removeFirst();
        this.addAsFirst(replacement);
        int indexAdjustment = 0;
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

    LexemeImpl replaceLastWith(final LexemeImpl replacement) {
        if (replacement == null) {
            throw new NullPointerException();
        }
        if (this.lexemes.size() == 0) {
            throw new IllegalStateException("No last lexeme to replace");
        }
        LexemeImpl oldLast = this.lexemes.removeLast();
        this.addAsLast(replacement);
        return oldLast;
    }

    void addAsFirst(final LexemeImpl toAdd) {
        if (toAdd != null) {
            LexemeImpl oldFirst = this.lexemes.getFirst();
            if (oldFirst != null) {
                oldFirst.setPrevious(toAdd);
                toAdd.setNext(oldFirst);
            }
            toAdd.setPrevious(null);
            this.lexemes.addFirst(toAdd);
            this.updateLinksToFirst();
            if (!toAdd.isSynthetic()) {
                //Assume a single white space token separator:
                this.adjustIndexes(1, toAdd.getTACToken().length() + 1);
            }
        }
    }

    void addAsLast(final LexemeImpl toAdd) {
        if (toAdd != null) {
            if (this.lexemes.size() > 0) {
                LexemeImpl oldLast = this.lexemes.getLast();
                oldLast.setNext(toAdd);
                toAdd.setPrevious(oldLast);
                toAdd.setFirst(this.lexemes.getFirst());
            } else {
                toAdd.setFirst(toAdd);
                toAdd.setPrevious(null);
            }
            toAdd.setNext(null);
            this.lexemes.addLast(toAdd);
        }
    }

    LexemeImpl removeFirst() {
        LexemeImpl removed = this.lexemes.removeFirst();
        if (removed != null) {
            this.lexemes.getFirst().setPrevious(null);
            this.updateLinksToFirst();
            if (!removed.isSynthetic()) {
                this.adjustIndexes(0, -(removed.getTACToken().length() + 1));
            }
        }

        return removed;
    }

    LexemeImpl removeLast() {
        LexemeImpl removed = this.lexemes.removeLast();
        if (removed != null) {
            this.lexemes.getLast().setNext(null);
        }
        return removed;
    }

    private void updateLinksToFirst() {
        LexemeImpl first = this.lexemes.getFirst();
        for (LexemeImpl l : this.lexemes) {
            l.setFirst(first);
        }
    }

    private void adjustIndexes(int fromIndex, int by) {
        ListIterator<LexemeImpl> it = this.lexemes.listIterator(fromIndex);
        LexemeImpl li;
        while (it.hasNext()) {
            li = it.next();
            li.setStartIndex(li.getStartIndex() + by);
            li.setEndIndex(li.getEndIndex() + by);
        }
    }


    private void constructFromTAC() {
        if (this.originalTac != null && this.originalTac.length() > 0) {
            Pattern horVisFractionNumberPart1Pattern = Pattern.compile("^[0-9]*$");
            Pattern horVisFractionNumberPart2Pattern = Pattern.compile("^[0-9]*/[0-9]*[A-Z]{2}$");
            // Windshear token for a particular runway has changed between 16th and 19th edition of Annex 3
            //  16th = "WS RWYnn[LRC]"
            //  19th = "WS Rnn[LRC]"
            Pattern windShearRunwayPattern = Pattern.compile("^R(?:WY)?([0-9]{2})?[LRC]?$");
            StringTokenizer st = new StringTokenizer(originalTac, " \n\t\r\f", true);
            String lastToken = null;
            String lastLastToken = null;
            boolean inWhitespace = false;
            int start = 0;
            LexemeImpl l;
            while (st.hasMoreTokens()) {
                String s = st.nextToken();
                start = originalTac.indexOf(s, start);
                //Whitespace only:
                if (s.matches("\\s")) {
                    if (inWhitespace) {
                        //combine with the preceding whitespace:
                        //Note: identify already here because Lexeme.getPrevious() and getNext need identified whitespace lexemes:
                        l = new LexemeImpl(this.getLastLexeme().getTACToken() + s, Lexeme.Identity.WHITE_SPACE);
                        l.setStartIndex(this.getLastLexeme().getStartIndex());
                        l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
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

                    } else if (lastToken != null && horVisFractionNumberPart2Pattern.matcher(s).matches() && horVisFractionNumberPart1Pattern.matcher(
                            lastToken).matches()) {
                        // cases like "1 1/8SM", combine the two tokens:
                        l = new LexemeImpl(lastToken + " " + s);
                        //last is a whitespace now, so need to remove it first:
                        this.removeLast();
                        l.setStartIndex(this.getLastLexeme().getStartIndex());
                        l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                        this.replaceLastWith(l);

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
                        l = new LexemeImpl(lastToken + " " + s);
                        //last is a whitespace now, so need to remove it first:
                        this.removeLast();
                        l.setStartIndex(this.getLastLexeme().getStartIndex());
                        l.setEndIndex(l.getStartIndex() + l.getTACToken().length() - 1);
                        this.replaceLastWith(l);
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
        }
    }

    private String getAsTAC() {
        if (this.lexemes != null) {
            StringBuilder retval = new StringBuilder();
            retval.append(this.lexemes.stream()
                    .map(LexemeImpl::getTACToken)
                    .collect(Collectors.joining()));
            return retval.toString();
        } else {
            return null;
        }
    }

}