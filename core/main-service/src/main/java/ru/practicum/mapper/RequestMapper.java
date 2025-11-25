package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.model.Request;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface RequestMapper {

    @Mapping(target = "requesterId", source = "request.requester.id")
    @Mapping(target = "eventId", source = "request.event.id")
    ParticipationRequestDto toDto(Request request);

    @Mapping(target = "requester.id", source = "requesterId")
    @Mapping(target = "event.id", source = "eventId")
    Request toEntity(ParticipationRequestDto dto);
}