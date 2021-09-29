package fi.fmi.avi.converter.tac.lexer.impl;

import fi.fmi.avi.converter.tac.lexer.LexemeVisitor;

/**
 * Created by rinne on 18/01/17.
 */
public abstract class PrioritizedLexemeVisitor implements LexemeVisitor, Comparable<LexemeVisitor> {

    private OccurrenceFrequency expectedOccurrence;

    protected PrioritizedLexemeVisitor(final OccurrenceFrequency expectedOccurrence) {
        this.expectedOccurrence = expectedOccurrence;
    }

    protected PrioritizedLexemeVisitor() {
        this(OccurrenceFrequency.AVERAGE);
    }

    public OccurrenceFrequency getExpectedOccurrence() {
        return this.expectedOccurrence;
    }

    public void setExpectedOccurrence(final OccurrenceFrequency prio) {
        this.expectedOccurrence = prio;
    }

    public PrioritizedLexemeVisitor withExpectedOccurrence(final OccurrenceFrequency prio) {
        this.setExpectedOccurrence(prio);
        return this;
    }

    @Override
    public int compareTo(final LexemeVisitor o) {
        if (o instanceof PrioritizedLexemeVisitor) {
            return this.expectedOccurrence.asNumber() - ((PrioritizedLexemeVisitor) o).expectedOccurrence.asNumber();
        } else {
            return 0;
        }
    }

    public String toString() {
        return "expected occurrence frequency:" + this.expectedOccurrence;
    }

    public enum OccurrenceFrequency {
        FREQUENT(1), AVERAGE(2), RARE(3);

        private final int occurrence;

        OccurrenceFrequency(final int occurrence) {
            this.occurrence = occurrence;
        }

        public int asNumber() {
            return this.occurrence;
        }
    }
}
