package ru.practicum.service;

import ru.practicum.ewm.stat.avro.EventSimilarityAvro;
import ru.practicum.ewm.stat.avro.UserActionAvro;

import java.util.List;

public interface SimilarityService {

    List<EventSimilarityAvro> updateSimilarity(UserActionAvro userAction);

}
