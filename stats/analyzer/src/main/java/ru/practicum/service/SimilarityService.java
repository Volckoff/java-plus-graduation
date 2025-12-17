package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.EventSimilarity;
import ru.practicum.repository.EventSimilarityRepository;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SimilarityService {
    private final EventSimilarityRepository repository;

    @Transactional(readOnly = true)
    public List<EventSimilarity> findAllContainsEventId(long eventId) {
        return repository.findAllByEvent(eventId);
    }

    @Transactional(readOnly = true)
    public List<EventSimilarity> findNPairContainsEventIdsSortedDescScore(Set<Long> eventIds, int maxResults) {
        return repository.findNPairContainsEventIdsSortedDescScore(eventIds, maxResults);
    }
}

