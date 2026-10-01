package io.arrogantprogrammer.thoughts.application;

import io.arrogantprogrammer.thoughts.domain.Author;
import io.arrogantprogrammer.thoughts.domain.Content;
import io.arrogantprogrammer.thoughts.domain.Thought;
import io.arrogantprogrammer.thoughts.domain.ThoughtId;
import io.arrogantprogrammer.thoughts.domain.ThoughtRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
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

    public Optional<ThoughtDTO> findById(UUID id) {
        return thoughtRepository.findById(new ThoughtId(id)).map(ThoughtDTO::from);
    }

    public List<ThoughtDTO> list(int page, int size) {
        return thoughtRepository.page(page, size).stream().map(ThoughtDTO::from).toList();
    }

    @Transactional
    public ThoughtDTO create(String content, String authorName, String authorBio) {
        Thought thought = Thought.create(new Content(content), new Author(authorName, authorBio));
        thoughtRepository.save(thought);
        return ThoughtDTO.from(thought);
    }

    @Transactional
    public Optional<ThoughtDTO> update(UUID id, String content, String authorName, String authorBio) {
        return thoughtRepository.findById(new ThoughtId(id)).map(thought -> {
            thought.edit(new Content(content), new Author(authorName, authorBio));
            thoughtRepository.save(thought);
            return ThoughtDTO.from(thought);
        });
    }

    @Transactional
    public boolean delete(UUID id) {
        return thoughtRepository.delete(new ThoughtId(id));
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
