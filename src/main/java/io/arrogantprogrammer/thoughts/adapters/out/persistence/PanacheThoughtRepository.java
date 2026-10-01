package io.arrogantprogrammer.thoughts.adapters.out.persistence;

import io.arrogantprogrammer.thoughts.domain.Thought;
import io.arrogantprogrammer.thoughts.domain.ThoughtId;
import io.arrogantprogrammer.thoughts.domain.ThoughtRepository;
import io.arrogantprogrammer.thoughts.domain.ThoughtStatus;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class PanacheThoughtRepository implements ThoughtRepository {

    @Override
    public void save(Thought thought) {
        ThoughtEntity entity = ThoughtEntity.findById(thought.id().value());
        if (entity == null) {
            ThoughtMapper.toEntity(thought).persist();
        } else {
            ThoughtMapper.copyMutableFields(entity, thought);
        }
    }

    @Override
    public boolean incrementThumbsUpIfApproved(ThoughtId id) {
        boolean updated = ThoughtEntity.update("thumbsUp = thumbsUp + 1 where id = ?1 and status = ?2",
                id.value(), ThoughtStatus.APPROVED) > 0;
        ThoughtEntity.getEntityManager().clear();
        return updated;
    }

    @Override
    public boolean incrementThumbsDownIfApproved(ThoughtId id) {
        boolean updated = ThoughtEntity.update("thumbsDown = thumbsDown + 1 where id = ?1 and status = ?2",
                id.value(), ThoughtStatus.APPROVED) > 0;
        ThoughtEntity.getEntityManager().clear();
        return updated;
    }

    @Override
    public Optional<Thought> findById(ThoughtId id) {
        ThoughtEntity entity = ThoughtEntity.findById(id.value());
        return Optional.ofNullable(entity).map(ThoughtMapper::toDomain);
    }

    @Override
    public Optional<Thought> findRandomApproved() {
        List<ThoughtEntity> approved = ThoughtEntity.list("status", ThoughtStatus.APPROVED);
        if (approved.isEmpty()) {
            return Optional.empty();
        }
        ThoughtEntity entity = approved.get(ThreadLocalRandom.current().nextInt(approved.size()));
        return Optional.of(ThoughtMapper.toDomain(entity));
    }

    @Override
    public List<Thought> page(int pageIndex, int pageSize) {
        List<ThoughtEntity> entities = ThoughtEntity.findAll(Sort.by("createdAt"))
                .page(pageIndex, pageSize)
                .list();
        return entities.stream().map(ThoughtMapper::toDomain).toList();
    }

    @Override
    public long count() {
        return ThoughtEntity.count();
    }
}
