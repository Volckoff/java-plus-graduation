package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.event.*;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.RequestRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;

    private final StatsClient statsClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    //PRIVATE
    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        checkTwoHoursForEvent(dto.getEventDate());
        User initiator = getUserOrThrow(userId);
        Category category = getCategoryOrThrow(dto.getCategory());
        Location location = locationMapper.toLocation(dto.getLocation());

        Event event = eventMapper.toEvent(dto, initiator, category, location);

        return buildFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        getUserOrThrow(userId);

        return eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
                .stream()
                .map(this::buildShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getUserEvent(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event", "id", eventId));

        return buildFullDto(event);
    }

    @Override
    @Transactional
    public EventFullDto updateUserEvent(Long userId, Long eventId, UpdateEventUserRequestDto dto) {
        getUserOrThrow(userId);
        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Event", "id", eventId));

        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("опубликованное событие нельзя редактировать");
        }

        if (dto.getEventDate() != null) {
            checkTwoHoursForEvent(dto.getEventDate());
        }

        eventMapper.patchFromUser(dto, event);

        if (dto.getCategory() != null) {
            Category category = getCategoryOrThrow(dto.getCategory());
            event.setCategory(category);
        }

        if (dto.getLocation() != null) {
            event.setLocation(locationMapper.toLocation(dto.getLocation()));
        }

        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);
        return buildFullDto(saved);
    }

    //ADMIN
    @Override
    public List<EventFullDto> searchAdmin(List<Long> users,
                                          List<EventState> states,
                                          List<Long> categories,
                                          LocalDateTime rangeStart,
                                          LocalDateTime rangeEnd,
                                          int from,
                                          int size) {
        checkRangeTime(rangeStart, rangeEnd);

        return eventRepository.findEventsByAdminFilters(users, states, categories, rangeStart, rangeEnd, from, size)
                .stream()
                .map(this::buildFullDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventFullDto updateAdminEvent(Long eventId, UpdateEventAdminRequestDto dto) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", "id", eventId));

        if (dto.getEventDate() != null) {
            checkTwoHoursForEvent(dto.getEventDate());
        }

        eventMapper.patchFromAdmin(dto, event);

        if (dto.getCategory() != null) {
            Category category = getCategoryOrThrow(dto.getCategory());
            event.setCategory(category);
        }

        if (dto.getLocation() != null) {
            event.setLocation(locationMapper.toLocation(dto.getLocation()));
        }

        if (dto.getStateAction() != null) {
            if (event.getState() != EventState.PENDING) {
                throw new ConflictException("Только ожидающие события могут быть опубликованы или отклонены.");
            }
            switch (dto.getStateAction()) {
                case PUBLISH_EVENT -> {
                    event.setState(EventState.PUBLISHED);
                    event.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> event.setState(EventState.CANCELED);
            }
        }

        Event saved = eventRepository.save(event);
        return buildFullDto(saved);
    }

    //PUBLIC
    @Override
    public List<EventShortDto> searchPublic(String text,
                                            List<Long> categories,
                                            Boolean paid,
                                            LocalDateTime rangeStart,
                                            LocalDateTime rangeEnd,
                                            Boolean onlyAvailable,
                                            String sort,
                                            int from,
                                            int size,
                                            HttpServletRequest request) {
        checkRangeTime(rangeStart, rangeEnd);

        List<Event> events = eventRepository.findPublishedEvents(
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        saveHit(request);

        if (events.isEmpty()) return List.of();

        return events.stream()
                .peek(event -> event.setViews(getViewsForEvent(event.getId())))
                .map(this::buildShortDto)
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", "id", eventId));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Event", "id", eventId);
        }

        saveHit(request);

        event.setViews(event.getViews() + 1);
        eventRepository.save(event);

        return eventMapper.toFullDto(event);
    }

    //helper
    private EventFullDto buildFullDto(Event event) {
        EventFullDto dto = eventMapper.toFullDto(event);
        Long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        dto.setConfirmedRequests(confirmed);
        return dto;
    }

    private EventShortDto buildShortDto(Event event) {
        EventShortDto dto = eventMapper.toShortDto(event);
        Long confirmed = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
        dto.setConfirmedRequests(confirmed);
        return dto;
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", "id", userId));
    }

    private Category getCategoryOrThrow(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", "id", categoryId));
    }

    private void checkTwoHoursForEvent(LocalDateTime time) {
        if (time.isBefore(LocalDateTime.now().plusHours(2))) {
            throw new ConflictException("дата и время на которые намечено событие не может быть раньше," +
                    " чем через два часа от текущего момента");
        }
    }

    private void checkRangeTime(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            throw new IllegalArgumentException("Начало должно быть до окончания");
        }
    }

    private void saveHit(HttpServletRequest request) {
        try {
            EndpointHitDto hit = EndpointHitDto.builder()
                    .app("ewm-main-service")
                    .uri(request.getRequestURI())
                    .ip(request.getRemoteAddr())
                    .timestamp(LocalDateTime.now())
                    .build();
            statsClient.saveHit(hit);
        } catch (Exception e) {
            log.warn("Не удалось записать хит статистики: {}", e.getMessage());
        }
    }

    private Long getViewsForEvent(Long eventId) {
        String start = LocalDateTime.now().minusYears(10).format(FORMATTER);
        String end = LocalDateTime.now().format(FORMATTER);
        String uri = "/events/" + eventId;

        try {
            var listStats = statsClient.getStats(start, end, List.of(uri), true);
            return listStats.isEmpty() ? 0L : listStats.get(0).getHits();
        } catch (Exception e) {
            log.warn("Не удалось получить хит статистики: {}", e.getMessage());
            return 0L;
        }
    }
}