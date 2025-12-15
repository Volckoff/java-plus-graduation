package ru.practicum.client.mapper;

import com.google.protobuf.Timestamp;
import ru.practicum.grpc.stats.action.ActionTypeProto;
import ru.practicum.grpc.stats.action.UserActionProto;

import java.time.Instant;

public class UserActionMapper {

    public static UserActionProto toProto(Long userId, Long eventId, ActionTypeProto actionType, Instant timestamp) {
        return UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(actionType)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(timestamp.getEpochSecond())
                        .setNanos(timestamp.getNano())
                        .build())
                .build();
    }
}


