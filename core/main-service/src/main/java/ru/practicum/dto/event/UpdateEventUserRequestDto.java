package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateEventUserRequestDto {

    @Size(min = 20, max = 2000, message = "Длина аннотации должна быть от 20 до 2000 символов")
    String annotation;

    Long category;

    @Size(min = 20, max = 7000, message = "Описание должно быть длиной от 20 до 7000 символов")
    String description;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Future(message = "Дата события должна быть в будущем")
    LocalDateTime eventDate;

    LocationDto location;

    Boolean paid;

    @PositiveOrZero(message = "Лимит участников должен быть положительным числом или нулём")
    Integer participantLimit;

    Boolean requestModeration;

    StateActionUser stateAction;

    @Size(min = 3, max = 120, message = "Название должно быть длиной от 3 до 120 символов")
    String title;

    public enum StateActionUser {
        SEND_TO_REVIEW,
        CANCEL_REVIEW
    }
}
