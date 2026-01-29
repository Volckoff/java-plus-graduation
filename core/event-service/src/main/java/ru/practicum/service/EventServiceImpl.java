package ru.practicum.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.AnalyzerClient;
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
    private final AnalyzerClient analyzerClient;

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

        // Получаем рейтинги для всех событий
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Double> ratingsMap = getEventRatings(eventIds);

        return events.stream()
                .map(event -> buildShortDto(event, usersMap, ratingsMap.getOrDefault(event.getId(), 0.0)))
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

        // Получаем рейтинги для всех событий
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Double> ratingsMap = getEventRatings(eventIds);

        return events.stream()
                .map(event -> buildFullDto(event, usersMap, ratingsMap.getOrDefault(event.getId(), 0.0)))
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

        if (events.isEmpty()) return List.of();

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(initiatorIds);

        // Получаем рейтинги для всех событий
        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Double> ratingsMap = getEventRatings(eventIds);

        return events.stream()
                .map(event -> buildShortDto(event, usersMap, ratingsMap.getOrDefault(event.getId(), 0.0)))
                .collect(Collectors.toList());
    }

    @Override
    public EventFullDto getPublicEvent(Long eventId, Long userId, HttpServletRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", "id", eventId));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new NotFoundException("Event", "id", eventId);
        }

        // Отправляем информацию о просмотре через Collector
        if (userId != null) {
            try {
                statsClient.recordView(userId, eventId);
            } catch (Exception e) {
                log.warn("Не удалось отправить информацию о просмотре: {}", e.getMessage());
            }
        }

        // Получаем рейтинг через Analyzer
        Double rating = getEventRatings(List.of(eventId)).getOrDefault(eventId, 0.0);

        return buildFullDto(event, getUserShortDtoMap(Set.of(event.getInitiatorId())), rating);
    }

    private EventFullDto buildFullDto(Event event) {
        Map<Long, Double> ratingsMap = getEventRatings(List.of(event.getId()));
        Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
        return buildFullDto(event, getUserShortDtoMap(Set.of(event.getInitiatorId())), rating);
    }

    private EventFullDto buildFullDto(Event event, Map<Long, UserShortDto> usersMap, Double rating) {
        EventFullDto dto = eventMapper.toFullDto(event);
        dto.setRating(rating);
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
        Map<Long, Double> ratingsMap = getEventRatings(List.of(event.getId()));
        Double rating = ratingsMap.getOrDefault(event.getId(), 0.0);
        return buildShortDto(event, getUserShortDtoMap(Set.of(event.getInitiatorId())), rating);
    }

    private EventShortDto buildShortDto(Event event, Map<Long, UserShortDto> usersMap, Double rating) {
        EventShortDto dto = eventMapper.toShortDto(event);
        dto.setRating(rating);
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

    private Map<Long, Double> getEventRatings(List<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Map.of();
        }
        try {
            return analyzerClient.getEventRatings(eventIds);
        } catch (Exception e) {
            log.warn("Не удалось получить рейтинги мероприятий: {}", e.getMessage());
            return eventIds.stream().collect(Collectors.toMap(id -> id, id -> 0.0));
        }
    }

    @Override
    @Transactional
    public void likeEvent(Long userId, Long eventId) {
        getUserOrThrow(userId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event", "id", eventId));

        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Нельзя лайкнуть неопубликованное событие");
        }

        // Проверяем, что пользователь посещал мероприятие (есть подтвержденный запрос)
        try {
            Boolean hasConfirmedRequest = requestClient.hasConfirmedRequest(userId, eventId);
            if (hasConfirmedRequest == null || !hasConfirmedRequest) {
                throw new ConflictException("Пользователь может лайкать только посещённые им мероприятия");
            }
        } catch (Exception e) {
            if (e instanceof ConflictException) {
                throw e;
            }
            log.warn("Ошибка при проверке участия пользователя {} в событии {}: {}", userId, eventId, e.getMessage());
            throw new ConflictException("Не удалось проверить участие пользователя в мероприятии");
        }

        try {
            statsClient.recordLike(userId, eventId);
        } catch (Exception e) {
            log.warn("Не удалось отправить действие лайка: {}", e.getMessage());
        }
    }

    @Override
    public List<EventShortDto> getRecommendations(Long userId, Integer maxResults) {
        getUserOrThrow(userId);

        // Получаем рекомендации через Analyzer
        Map<Long, Double> recommendationsMap;
        try {
            recommendationsMap = analyzerClient.getRecommendationsForUser(userId, maxResults);
        } catch (Exception e) {
            log.warn("Не удалось получить рекомендации для пользователя {}: {}", userId, e.getMessage());
            return List.of();
        }

        if (recommendationsMap == null || recommendationsMap.isEmpty()) {
            return List.of();
        }

        // Получаем события по ID
        List<Event> events = eventRepository.findAllById(recommendationsMap.keySet());
        
        // Фильтруем только опубликованные события
        events = events.stream()
                .filter(event -> event.getState().equals(EventState.PUBLISHED))
                .collect(Collectors.toList());

        if (events.isEmpty()) {
            return List.of();
        }

        Set<Long> initiatorIds = events.stream()
                .map(Event::getInitiatorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(initiatorIds);

        // Сортируем по рейтингу из рекомендаций
        return events.stream()
                .sorted((e1, e2) -> {
                    Double score1 = recommendationsMap.getOrDefault(e1.getId(), 0.0);
                    Double score2 = recommendationsMap.getOrDefault(e2.getId(), 0.0);
                    return score2.compareTo(score1);
                })
                .map(event -> buildShortDto(event, usersMap, recommendationsMap.getOrDefault(event.getId(), 0.0)))
                .collect(Collectors.toList());
    }

}