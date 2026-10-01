package io.arrogantprogrammer.thoughts.domain;

import java.util.List;
import java.util.Optional;

public interface ThoughtRepository {

    void save(Thought thought);

    Optional<Thought> findById(ThoughtId id);

    Optional<Thought> findRandomApproved();

    List<Thought> page(int pageIndex, int pageSize);

    long count();
}
