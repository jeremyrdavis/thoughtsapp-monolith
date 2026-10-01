package io.arrogantprogrammer.thoughts.adapters.out.persistence;

import io.arrogantprogrammer.thoughts.domain.ThoughtStatus;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "thoughts")
public class ThoughtEntity extends PanacheEntityBase {

    @Id
    public UUID id;

    @Column(nullable = false, length = 500)
    public String content;

    @Column(nullable = false, length = 200)
    public String author;

    @Column(name = "author_bio", length = 200)
    public String authorBio;

    @Column(name = "thumbs_up", nullable = false)
    public int thumbsUp;

    @Column(name = "thumbs_down", nullable = false)
    public int thumbsDown;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public ThoughtStatus status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    public Instant updatedAt;
}
