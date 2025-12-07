package ru.practicum.dto.event;

import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventRequestStatusUpdateResultDto {

    @Builder.Default
    List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();

    @Builder.Default
    List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

}




