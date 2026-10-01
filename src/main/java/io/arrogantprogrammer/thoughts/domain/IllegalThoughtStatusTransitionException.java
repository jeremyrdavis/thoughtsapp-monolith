package io.arrogantprogrammer.thoughts.domain;

public class IllegalThoughtStatusTransitionException extends RuntimeException {

    public IllegalThoughtStatusTransitionException(ThoughtStatus from, ThoughtStatus to) {
        super("cannot transition thought from " + from + " to " + to);
    }
}
