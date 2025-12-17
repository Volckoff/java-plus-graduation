package ru.practicum.mapper;

import ru.practicum.ewm.stat.avro.ActionTypeAvro;
import ru.practicum.ewm.stat.avro.EventSimilarityAvro;
import ru.practicum.ewm.stat.avro.UserActionAvro;
import ru.practicum.model.ActionType;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;

public class Mapper {

    public static UserAction mapToUserAction(UserActionAvro userActionAvro) {
        return UserAction.builder()
                .userId(userActionAvro.getUserId())
                .eventId(userActionAvro.getEventId())
                .actionType(toActionType(userActionAvro.getActionType()))
                .created(userActionAvro.getTimestamp())
                .weight(0.0)
                .build();
    }

    public static ActionType toActionType(ActionTypeAvro actionTypeAvro) {
        return ActionType.valueOf(actionTypeAvro.name());
    }

    public static EventSimilarity mapToEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        return EventSimilarity.builder()
                .aeventId(eventSimilarityAvro.getEventA())
                .beventId(eventSimilarityAvro.getEventB())
                .score(eventSimilarityAvro.getScore())
                .build();
    }
}
