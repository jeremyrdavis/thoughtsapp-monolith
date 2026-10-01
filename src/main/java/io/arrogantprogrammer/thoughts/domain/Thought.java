package io.arrogantprogrammer.thoughts.domain;

import java.time.Instant;
import java.util.List;

public class Thought {

    private final ThoughtId id;
    private Content content;
    private Author author;
    private Rating rating;
    private ThoughtStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    private Thought(ThoughtId id, Content content, Author author, Rating rating,
                     ThoughtStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.rating = rating;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Thought create(Content content, Author author) {
        Instant now = Instant.now();
        return new Thought(ThoughtId.generate(), content, author, Rating.zero(), ThoughtStatus.IN_REVIEW, now, now);
    }

    public static Thought rehydrate(ThoughtId id, Content content, Author author, Rating rating,
                                     ThoughtStatus status, Instant createdAt, Instant updatedAt) {
        return new Thought(id, content, author, rating, status, createdAt, updatedAt);
    }

    public void edit(Content content, Author author) {
        this.content = content;
        this.author = author;
        this.updatedAt = Instant.now();
    }

    public void thumbsUp() {
        this.rating = rating.incrementThumbsUp();
        this.updatedAt = Instant.now();
    }

    public void thumbsDown() {
        this.rating = rating.incrementThumbsDown();
        this.updatedAt = Instant.now();
    }

    public void approve() {
        transitionTo(ThoughtStatus.APPROVED, ThoughtStatus.IN_REVIEW);
    }

    public void remove() {
        transitionTo(ThoughtStatus.REMOVED, ThoughtStatus.IN_REVIEW, ThoughtStatus.APPROVED);
    }

    public void restore() {
        transitionTo(ThoughtStatus.IN_REVIEW, ThoughtStatus.REMOVED);
    }

    public void sendBackToReview() {
        transitionTo(ThoughtStatus.IN_REVIEW, ThoughtStatus.APPROVED);
    }

    private void transitionTo(ThoughtStatus target, ThoughtStatus... allowedFrom) {
        if (!List.of(allowedFrom).contains(status)) {
            throw new IllegalThoughtStatusTransitionException(status, target);
        }
        this.status = target;
        this.updatedAt = Instant.now();
    }

    public ThoughtId id() {
        return id;
    }

    public Content content() {
        return content;
    }

    public Author author() {
        return author;
    }

    public Rating rating() {
        return rating;
    }

    public ThoughtStatus status() {
        return status;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
    }
}
