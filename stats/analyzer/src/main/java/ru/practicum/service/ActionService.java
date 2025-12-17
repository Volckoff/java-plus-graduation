package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.configuration.UserActionWeightConfig;
import ru.practicum.model.ActionType;
import ru.practicum.model.UserAction;
import ru.practicum.repository.UserActionRepository;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ActionService {
    private final UserActionRepository repository;
    private final UserActionWeightConfig userActionWeightConfig;

    @Transactional
    public void addAction(long userId, long eventId, ActionType actionType, Instant timestamp) {
        Optional<UserAction> oldActionOpt = repository.findByUserIdAndEventId(userId, eventId);
        if (oldActionOpt.isEmpty()) {
            UserAction action = UserAction.builder()
                    .userId(userId)
                    .eventId(eventId)
                    .actionType(actionType)
                    .created(timestamp)
                    .weight(getUserActionWeight(actionType))
                    .build();
            repository.save(action);
        } else {
            UserAction oldAction = oldActionOpt.get();
            double oldWeight = getUserActionWeight(oldAction.getActionType());
            double newWeight = getUserActionWeight(actionType);
            if (newWeight >= oldWeight) {
                oldAction.setActionType(actionType);
                oldAction.setWeight(newWeight);
                Instant oldTimestamp = oldAction.getCreated();
                if (oldTimestamp == null || oldTimestamp.isBefore(timestamp)) {
                    oldAction.setCreated(timestamp);
                }
                repository.save(oldAction);
            }
        }
    }

    @Transactional(readOnly = true)
    public Set<Long> findAllByUserIdAndEventIdIn(long userId, Set<Long> eventIds) {
        return new HashSet<>(repository.findEventIdsByUserIdAndEventIdIn(userId, eventIds));
    }

    @Transactional(readOnly = true)
    public Set<Long> findByUserIdOrderByTimestampDesc(long userId, int maxResult) {
        return new HashSet<>(repository.findByUserIdOrderByCreatedDescLimitedTo(userId, maxResult));
    }

    @Transactional(readOnly = true)
    public List<UserAction> findAllByEventIds(Set<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return repository.findAllByEventIdIn(eventIds).stream().toList();
    }

    private double getUserActionWeight(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> userActionWeightConfig.getVIEW();
            case REGISTER -> userActionWeightConfig.getREGISTER();
            case LIKE -> userActionWeightConfig.getLIKE();
        };
    }
}

