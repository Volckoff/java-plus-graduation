package ru.practicum.client;

import java.util.List;
import java.util.Map;

public interface AnalyzerClient {

    Map<Long, Double> getEventRatings(List<Long> eventIds);

    Map<Long, Double> getRecommendationsForUser(Long userId, Integer maxResults);
}

