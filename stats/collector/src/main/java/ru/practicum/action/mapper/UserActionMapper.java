package ru.practicum.action.mapper;

import ru.practicum.action.model.ActionType;
import ru.practicum.action.model.UserAction;
import ru.practicum.ewm.stat.avro.ActionTypeAvro;
import ru.practicum.ewm.stat.avro.UserActionAvro;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;

import java.time.Instant;

public class UserActionMapper {
    public static UserActionAvro toUserActionAvro(UserAction userAction) {
        return UserActionAvro.newBuilder()
                .setUserId(userAction.getUserId())
                .setEventId(userAction.getEventId())
                .setTimestamp(userAction.getTimestamp())
                .setActionType(toActionTypeAvro(userAction.getActionType()))
                .build();

    }

    public static ActionTypeAvro toActionTypeAvro(ActionType actionType) {
        return ActionTypeAvro.valueOf(actionType.name());
    }


    public static UserAction map(UserActionProto userActionProto) {
        return UserAction.builder()
                .userId(userActionProto.getUserId())
                .eventId(userActionProto.getEventId())
                .actionType(toActionType(userActionProto.getActionType()))
                .timestamp(Instant.ofEpochSecond(userActionProto.getTimestamp().getSeconds(),
                        userActionProto.getTimestamp().getNanos()))
                .build();
    }

    public static ActionType toActionType(ActionTypeProto actionTypeProto) {
        return switch (actionTypeProto) {
            case ACTION_VIEW -> ActionType.VIEW;
            case ACTION_REGISTER -> ActionType.REGISTER;
            case ACTION_LIKE -> ActionType.LIKE;
            default -> null;
        };
    }

}
