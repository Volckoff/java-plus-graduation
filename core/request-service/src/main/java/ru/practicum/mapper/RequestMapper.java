package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.Request;

@Component
public class RequestMapper {

    public ParticipationRequestDto toDto(Request request) {
        if (request == null) {
            return null;
        }

        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreated())
                .requesterId(request.getRequesterId())
                .eventId(request.getEventId())
                .status(request.getStatus())
                .build();
    }
}
