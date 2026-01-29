package ru.practicum.action.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Builder
@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserAction {

    @NotNull
    Long userId;

    @NotNull
    Long eventId;

    @NotNull
    ActionType actionType;

    Instant timestamp = Instant.now();

}
