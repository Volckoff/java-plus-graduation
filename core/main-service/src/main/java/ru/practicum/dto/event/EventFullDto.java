package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.EventState;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventFullDto {

    Long id;
    String annotation;
    CategoryDto category;
    Long confirmedRequests;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime createdOn;

    String description;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime eventDate;

    UserShortDto initiator;
    LocationDto location;
    boolean paid;
    Integer participantLimit;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    LocalDateTime publishedOn;

    boolean requestModeration;
    EventState state;
    String title;
    Long views;

}
