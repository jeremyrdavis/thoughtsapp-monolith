package io.arrogantprogrammer.thoughts.application;

import io.arrogantprogrammer.thoughts.domain.ThoughtId;
import io.arrogantprogrammer.thoughts.domain.ThoughtRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

@ApplicationScoped
public class ThoughtApplicationService {

    @Inject
    ThoughtRepository thoughtRepository;

    public Optional<ThoughtDTO> randomApprovedThought() {
        return thoughtRepository.findRandomApproved().map(ThoughtDTO::from);
    }

    @Transactional
    public Optional<ThoughtDTO> thumbsUp(UUID id) {
        return vote(new ThoughtId(id), thoughtRepository::incrementThumbsUpIfApproved);
    }

    @Transactional
    public Optional<ThoughtDTO> thumbsDown(UUID id) {
        return vote(new ThoughtId(id), thoughtRepository::incrementThumbsDownIfApproved);
    }

    private Optional<ThoughtDTO> vote(ThoughtId id, Predicate<ThoughtId> increment) {
        if (!increment.test(id)) {
            return Optional.empty();
        }
        return thoughtRepository.findById(id).map(ThoughtDTO::from);
    }
}
