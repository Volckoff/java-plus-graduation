package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.configuration.UserActionWeightConfig;
import ru.practicum.grpc.stats.recommendation.InteractionsCountRequestProto;
import ru.practicum.grpc.stats.recommendation.RecommendedEventProto;
import ru.practicum.grpc.stats.recommendation.SimilarEventsRequestProto;
import ru.practicum.grpc.stats.recommendation.UserPredictionsRequestProto;
import ru.practicum.model.ActionType;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class AnalyzerService {
    private final ActionService actionService;
    private final SimilarityService similarityService;
    private final UserActionWeightConfig userActionWeightConfig;

    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        List<EventSimilarity> similarPair = similarityService.findAllContainsEventId(request.getEventId());

        Set<Long> ids = similarPair.stream().map(EventSimilarity::getAeventId).collect(Collectors.toSet());
        Set<Long> otherIds = similarPair.stream().map(EventSimilarity::getBeventId).collect(Collectors.toSet());
        ids.addAll(otherIds);

        Set<Long> userEventIds = actionService.findAllByUserIdAndEventIdIn(request.getUserId(), ids);

        similarPair.removeIf(o -> userEventIds.contains(o.getAeventId()) && userEventIds.contains(o.getBeventId()));

        return similarPair.stream()
                .sorted(Comparator.comparing(EventSimilarity::getScore, Comparator.reverseOrder()))
                .limit(request.getMaxResults())
                .map(o -> {
                    long eventId = Objects.equals(o.getAeventId(), request.getEventId()) 
                            ? o.getBeventId() 
                            : o.getAeventId();
                    return RecommendedEventProto.newBuilder()
                            .setEventId(eventId)
                            .setScore(o.getScore())
                            .build();
                }).toList();
    }

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Set<Long> actionIds = actionService.findByUserIdOrderByTimestampDesc(request.getUserId(),
                (int) request.getMaxResults());

        List<EventSimilarity> similarities = similarityService.findNPairContainsEventIdsSortedDescScore(actionIds,
                (int) request.getMaxResults());

        Map<Long, Double> eventIds = similarities.stream()
                .collect(Collectors.toMap(o -> actionIds.contains(o.getAeventId()) ? o.getBeventId() : o.getAeventId(),
                        EventSimilarity::getScore, Double::max));

        return eventIds.entrySet().stream().map(o -> RecommendedEventProto.newBuilder()
                .setEventId(o.getKey())
                .setScore(o.getValue())
                .build()).toList();
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        Set<Long> eventIds = new HashSet<>(request.getEventIdList());

        Map<Long, Double> actionMap = actionService.findAllByEventIds(eventIds).stream()
                .collect(Collectors.groupingBy(UserAction::getEventId,
                        Collectors.summingDouble(o -> getUserActionWeight(o.getActionType()))));

        return actionMap.entrySet().stream().map(o -> RecommendedEventProto.newBuilder()
                .setEventId(o.getKey())
                .setScore(o.getValue())
                .build()).toList();
    }

    private double getUserActionWeight(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> userActionWeightConfig.getVIEW();
            case REGISTER -> userActionWeightConfig.getREGISTER();
            case LIKE -> userActionWeightConfig.getLIKE();
        };
    }
}
