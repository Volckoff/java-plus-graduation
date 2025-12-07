package ru.practicum.service;

import ru.practicum.dto.event.EventRequestStatusUpdateRequestDto;
import ru.practicum.dto.event.EventRequestStatusUpdateResultDto;
import ru.practicum.dto.request.ParticipationRequestDto;

import java.util.List;

public interface RequestService {

    List<ParticipationRequestDto> getUserRequests(Long userId);

    ParticipationRequestDto addParticipationRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId);

    EventRequestStatusUpdateResultDto changeRequestStatus(Long userId, Long eventId,
                                                          EventRequestStatusUpdateRequestDto updateRequestDto);

    Long getConfirmedRequestsCount(Long eventId, ru.practicum.model.RequestStatus status);
}

