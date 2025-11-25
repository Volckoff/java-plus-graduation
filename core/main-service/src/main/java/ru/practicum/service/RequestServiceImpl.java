package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.event.EventRequestStatusUpdateRequestDto;
import ru.practicum.dto.event.EventRequestStatusUpdateResultDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.RequestMapper;
import ru.practicum.model.*;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestMapper requestMapper;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Request", "UserId", userId));
        return requestRepository.findAllByRequesterId(userId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ParticipationRequestDto addParticipationRequest(Long userId, Long eventId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Request", "UserId", userId));
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Request", "EventId", eventId));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя запросить участие в неопубликованном событии");
        }

        if (event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Создатель не может запросить участие в своём событии.");
        }

        boolean alreadyExists = requestRepository.existsByRequesterIdAndEventId(userId, eventId);
        if (alreadyExists) {
            throw new ConflictException("Запрос уже существует");
        }

        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                        >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        RequestStatus status = RequestStatus.PENDING;
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        }

        Request request = new Request();
        request.setEvent(event);
        request.setRequester(user);
        request.setCreated(LocalDateTime.now());
        request.setStatus(status);

        Request saved = requestRepository.save(request);
        return requestMapper.toDto(saved);
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Request", "RequestId", requestId));

        if (!request.getRequester().getId().equals(userId)) {
            throw new ConflictException("Пользователь может отменять только свои запросы");
        }

        request.setStatus(RequestStatus.CANCELED);
        return requestMapper.toDto(requestRepository.save(request));
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Request", "EventId", eventId));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только создатель может смотреть запросы к событию");
        }

        return requestRepository.findAllByEventId(eventId).stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResultDto changeRequestStatus(Long userId, Long eventId,
                                                                 EventRequestStatusUpdateRequestDto updateRequestDto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Request", "EventId", eventId));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Только создатель может менять статус запроса");
        }

        if (event.getParticipantLimit() != 0 &&
                requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                        >= event.getParticipantLimit()) {
            throw new ConflictException("Достигнут лимит участников");
        }

        List<Request> requests = requestRepository.findAllById(updateRequestDto.getRequestIds());
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        for (Request req : requests) {
            if (!req.getEvent().getId().equals(eventId)) {
                throw new ConflictException("Запрос не относится к этому событию");
            }

            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Можно менять только статус запросов, находящихся в ожидании");
            }

            if (updateRequestDto.getStatus() == RequestStatus.CONFIRMED) {
                if (event.getParticipantLimit() != 0 &&
                        requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED)
                                >= event.getParticipantLimit()) {
                    req.setStatus(RequestStatus.REJECTED);
                    rejectedRequests.add(requestMapper.toDto(req));
                } else {
                    req.setStatus(RequestStatus.CONFIRMED);
                    confirmedRequests.add(requestMapper.toDto(req));
                }
            } else if (updateRequestDto.getStatus() == RequestStatus.REJECTED) {
                req.setStatus(RequestStatus.REJECTED);
                rejectedRequests.add(requestMapper.toDto(req));
            }
        }

        requestRepository.saveAll(requests);

        return new EventRequestStatusUpdateResultDto(confirmedRequests, rejectedRequests);
    }
}