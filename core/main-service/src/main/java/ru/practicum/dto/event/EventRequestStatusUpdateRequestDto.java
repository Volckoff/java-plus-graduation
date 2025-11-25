package ru.practicum.dto.event;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.model.RequestStatus;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateRequestDto {

    @NotEmpty
    List<Long> requestIds;

    @NotNull
    RequestStatus status;

}
