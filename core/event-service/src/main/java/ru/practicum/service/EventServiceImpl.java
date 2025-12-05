package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.client.StatsClient;
import ru.practicum.client.request.RequestClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.event.*;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.EventMapper;
import ru.practicum.mapper.LocationMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final UserClient userClient;
    private final CategoryRepository categoryRepository;
    private final RequestClient requestClient;

    private final EventMapper eventMapper;
    private final LocationMapper locationMapper;

    private final StatsClient statsClient;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {
        checkTwoHoursForEvent(dto.getEventDate());
        getUserOrThrow(userId);
        Category category = getCategoryOrThrow(dto.getCategory());
        Location location = locationMapper.toLocation(dto.getLocation());

        Event event = eventMapper.toEvent(dto, userId, category, location);

        return buildFullDto(eventRepository.save(event));
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {
        getUserOrThrow(userId);

        List<Event> events = eventRepository.findAllByInitiatorId(userId,
                PageRequest.of(from / size, size));
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(Set.of(userId));

        return events.stream()
                .map(event -> buildShortDto(event, usersMap))
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

    @Override
    public List<EventFullDto> searchAdmin(List<Long> users,
                                          List<EventState> states,
                                          List<Long> categories,
                                          LocalDateTime rangeStart,
                                          LocalDateTime rangeEnd,
                                          int from,
                                          int size) {
        checkRangeTime(rangeStart, rangeEnd);

        List<Event> events = eventRepository.findEventsByAdminFilters(users, states, categories, rangeStart,
                rangeEnd, from, size);
        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(initiatorIds);

        return events.stream()
                .map(event -> buildFullDto(event, usersMap))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getEventById(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", "id", eventId));
        return buildFullDto(event);
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

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(initiatorIds);

        return events.stream()
                .peek(event -> event.setViews(getViewsForEvent(event.getId())))
                .map(event -> buildShortDto(event, usersMap))
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

        return buildFullDto(event);
    }

    private EventFullDto buildFullDto(Event event) {
        return buildFullDto(event, getUserShortDtoMap(Set.of(event.getInitiatorId())));
    }

    private EventFullDto buildFullDto(Event event, Map<Long, UserShortDto> usersMap) {
        EventFullDto dto = eventMapper.toFullDto(event);
        try {
            Long confirmed = requestClient.getConfirmedRequestsCount(event.getId(), RequestStatus.CONFIRMED);
            dto.setConfirmedRequests(confirmed != null ? confirmed : 0L);
        } catch (Exception e) {
            log.warn("Ошибка при получении количества подтвержденных запросов для события {}: {}",
                    event.getId(), e.getMessage());
            dto.setConfirmedRequests(0L);
        }
        UserShortDto initiator = usersMap.get(event.getInitiatorId());
        if (initiator == null) {
            log.warn("Пользователь с ID {} не найден, создаем минимальный UserShortDto", event.getInitiatorId());
            initiator = UserShortDto.builder()
                    .id(event.getInitiatorId())
                    .name("Unknown User")
                    .build();
        }
        dto.setInitiator(initiator);
        return dto;
    }

    private EventShortDto buildShortDto(Event event) {
        return buildShortDto(event, getUserShortDtoMap(Set.of(event.getInitiatorId())));
    }

    private EventShortDto buildShortDto(Event event, Map<Long, UserShortDto> usersMap) {
        EventShortDto dto = eventMapper.toShortDto(event);
        try {
            Long confirmed = requestClient.getConfirmedRequestsCount(event.getId(), RequestStatus.CONFIRMED);
            dto.setConfirmedRequests(confirmed != null ? confirmed : 0L);
        } catch (Exception e) {
            log.warn("Ошибка при получении количества подтвержденных запросов для события {}: {}",
                    event.getId(), e.getMessage());
            dto.setConfirmedRequests(0L);
        }
        UserShortDto initiator = usersMap.get(event.getInitiatorId());
        if (initiator == null) {
            log.warn("Пользователь с ID {} не найден, создаем минимальный UserShortDto", event.getInitiatorId());
            initiator = UserShortDto.builder()
                    .id(event.getInitiatorId())
                    .name("Unknown User")
                    .build();
        }
        dto.setInitiator(initiator);
        return dto;
    }

    private Map<Long, UserShortDto> getUserShortDtoMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        try {
            List<UserDto> users = userClient.getUsersByIds(List.copyOf(userIds));
            if (users == null || users.isEmpty()) {
                return Map.of();
            }
            return users.stream()
                    .filter(user -> user != null && user.getId() != null)
                    .collect(Collectors.toMap(
                            UserDto::getId,
                            user -> UserShortDto.builder()
                                    .id(user.getId())
                                    .name(user.getName())
                                    .build()
                    ));
        } catch (Exception e) {
            log.error("Ошибка при получении пользователей: {}", e.getMessage());
            return Map.of();
        }
    }

    private void getUserOrThrow(Long userId) {
        try {
            UserDto user = userClient.getUserById(userId);
            if (user == null) {
                throw new NotFoundException("User", "id", userId);
            }
        } catch (feign.FeignException e) {
            if (e.status() == 404) {
                throw new NotFoundException("User", "id", userId);
            }
            log.error("Ошибка при получении пользователя {}: {}", userId, e.getMessage());
            throw new NotFoundException("User", "id", userId);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при получении пользователя {}: {}", userId, e.getMessage());
            throw new NotFoundException("User", "id", userId);
        }
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
                    .app("ewm-event-service")
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