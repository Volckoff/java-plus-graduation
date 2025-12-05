package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NewEventDto {


    @NotBlank(message = "Поле annotation не может быть пустым")
    @Size(min = 20, max = 2000, message = "Поле annotation должно быть от 20 до 2000 символов")
    String annotation;

    @NotNull(message = "Поле category не может быть пустым")
    Long category;

    @NotBlank(message = "Поле description не может быть пустым")
    @Size(min = 20, max = 7000, message = "Поле description должно быть от 20 до 7000 символов")
    String description;

    @NotNull(message = "Поле eventDate не может быть пустым")
    @Future(message = "Поле eventDate должно быть в будущем")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;

    @NotNull(message = "Поле location не может быть пустым")
    LocationDto location;

    @Builder.Default
    Boolean paid = false;

    @Builder.Default
    @PositiveOrZero(message = "Лимит участников должен быть положительным числом или нулём")
    Long participantLimit = 0L;

    @Builder.Default
    Boolean requestModeration = true;

    @NotBlank(message = "Поле title не может быть пустым")
    @Size(min = 3, max = 120, message = "Поле title должно быть от 3 до 120 символов")
    String title;
}




