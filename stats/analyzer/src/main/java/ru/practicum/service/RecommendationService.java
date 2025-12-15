package ru.practicum.service;

import ru.practicum.ewm.stat.avro.UserActionAvro;
import ru.practicum.grpc.stat.request.InteractionsCountRequestProto;
import ru.practicum.grpc.stat.request.RecommendedEventProto;
import ru.practicum.grpc.stat.request.SimilarEventsRequestProto;
import ru.practicum.grpc.stat.request.UserPredictionsRequestProto;

import java.util.List;

public interface RecommendationService {

    List<RecommendedEventProto> generateRecommendationsForUser(UserPredictionsRequestProto request);

    List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request);

    List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request);

    void saveUserAction(UserActionAvro userActionAvro);
}
