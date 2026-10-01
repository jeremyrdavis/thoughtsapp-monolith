package io.arrogantprogrammer.thoughts.application;

import io.arrogantprogrammer.thoughts.domain.ThoughtRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class ThoughtApplicationService {

    @Inject
    ThoughtRepository thoughtRepository;

    public Optional<ThoughtDTO> randomApprovedThought() {
        return thoughtRepository.findRandomApproved().map(ThoughtDTO::from);
    }
}
