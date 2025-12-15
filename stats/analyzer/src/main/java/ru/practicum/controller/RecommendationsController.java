package ru.practicum.controller;

import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.grpc.stat.analyzer.RecommendationsControllerGrpc;
import ru.practicum.grpc.stat.request.InteractionsCountRequestProto;
import ru.practicum.grpc.stat.request.RecommendedEventProto;
import ru.practicum.grpc.stat.request.SimilarEventsRequestProto;
import ru.practicum.grpc.stat.request.UserPredictionsRequestProto;
import ru.practicum.service.RecommendationService;

import java.util.List;

@GrpcService
@Slf4j
@RequiredArgsConstructor
public class RecommendationsController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationService recommendationService;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Вызов getRecommendationsForUser для request = {}", request);
        List<RecommendedEventProto> recommendedEvents = recommendationService.generateRecommendationsForUser(request);
        for (RecommendedEventProto event : recommendedEvents) {
            responseObserver.onNext(event);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Вызов getSimilarEvents для request = {}", request);
        List<RecommendedEventProto> recommendedEvents = recommendationService.getSimilarEvents(request);
        for (RecommendedEventProto event : recommendedEvents) {
            responseObserver.onNext(event);
        }
        responseObserver.onCompleted();
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        log.info("Вызов getInteractionsCount для request = {}", request);
        List<RecommendedEventProto> recommendedEvents = recommendationService.getInteractionsCount(request);
        for (RecommendedEventProto event : recommendedEvents) {
            responseObserver.onNext(event);
        }
        responseObserver.onCompleted();
    }
}
