package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ViewStatsDto;
import ru.practicum.client.StatsClient;
import ru.practicum.dto.compilation.CompilationDto;
import ru.practicum.dto.compilation.NewCompilationDto;
import ru.practicum.dto.compilation.UpdateCompilationRequestDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CompilationMapper;
import ru.practicum.model.Compilation;
import ru.practicum.model.Event;
import ru.practicum.model.RequestStatus;
import ru.practicum.repository.CompilationRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.client.request.RequestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CompilationServiceImpl implements CompilationService {

    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final StatsClient statsClient;
    private final RequestClient requestClient;

    @Override
    @Transactional(readOnly = true)
    public List<CompilationDto> getCompilations(Boolean pinned, Integer from, Integer size) {
        log.info("Получение подборок с параметрами: pinned={}, from={}, size={}", pinned, from, size);
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations = (pinned != null)
                ? compilationRepository.findByPinned(pinned, pageable).getContent()
                : compilationRepository.findAll(pageable).getContent();
        List<CompilationDto> result = compilations.stream()
                .map(compilationMapper::toDto)
                .map(this::addConfirmedRequestsAndViews)
                .collect(Collectors.toList());
        log.info("Подборки найдена");
        return result;
    }

    private CompilationDto addConfirmedRequestsAndViews(CompilationDto compilationDto) {
        if (compilationDto.getEvents() != null && !compilationDto.getEvents().isEmpty()) {
            List<String> uris = compilationDto.getEvents().stream()
                    .map(event -> "/events/" + event.getId())
                    .collect(Collectors.toList());
            Map<String, Long> viewsMap = getViewsFromStats(uris);
            for (EventShortDto eventDto : compilationDto.getEvents()) {
                String eventUri = "/events/" + eventDto.getId();
                eventDto.setViews(viewsMap.getOrDefault(eventUri, 0L));
                try {
                    Long confirmedRequests = requestClient.getConfirmedRequestsCount(eventDto.getId(),
                            RequestStatus.CONFIRMED);
                    eventDto.setConfirmedRequests(confirmedRequests != null ? confirmedRequests : 0L);
                } catch (Exception e) {
                    log.warn("Ошибка при получении количества подтвержденных запросов для события {}: {}", 
                            eventDto.getId(), e.getMessage());
                    eventDto.setConfirmedRequests(0L);
                }
            }
        }
        return compilationDto;
    }

    private Map<String, Long> getViewsFromStats(List<String> uris) {
        try {
            LocalDateTime end = LocalDateTime.now();
            LocalDateTime start = end.minusYears(1);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            List<ViewStatsDto> stats = statsClient.getStats(
                    start.format(formatter),
                    end.format(formatter),
                    uris,
                    false
            );
            Map<String, Long> viewsMap = new HashMap<>();
            for (ViewStatsDto stat : stats) {
                viewsMap.put(stat.getUri(), stat.getHits());
            }
            return viewsMap;
        } catch (Exception e) {
            log.warn("Ошибка при получении данных из сервиса статистики: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CompilationDto getCompilationById(Long compId) {
        log.info("Поиск подборки с id={}", compId);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", "Id", compId));
        CompilationDto result = compilationMapper.toDto(compilation);
        log.info("Подборка найдена");
        return addConfirmedRequestsAndViews(result);
    }

    private Set<Event> loadEvents(List<Long> eventIds) {
        List<Event> events = eventRepository.findAllById(eventIds);
        if (events.size() != eventIds.size()) {
            throw new NotFoundException("События не найдены по ID: " + eventIds);
        }
        return new HashSet<>(events);
    }

    @Override
    public CompilationDto saveCompilation(NewCompilationDto newComDto) {
        log.info("Попытка создания новой подборки");
        if (compilationRepository.existsByTitleIgnoreCaseAndTrim(newComDto.getTitle())) {
            throw new AlreadyExistsException("Compilation", "title", newComDto.getTitle());
        }
        Compilation compilation = compilationMapper.toCompilation(newComDto);
        Set<Event> events = (newComDto.getEvents() != null && !newComDto.getEvents().isEmpty())
                ? loadEvents(newComDto.getEvents())
                : new HashSet<>();
        compilation.setEvents(events);
        Compilation savedCompilation = compilationRepository.save(compilation);
        CompilationDto result = compilationMapper.toDto(savedCompilation);
        log.info("Подборка успешно создана");
        return addConfirmedRequestsAndViews(result);
    }

    @Override
    public CompilationDto updateCompilation(Long compId, UpdateCompilationRequestDto updateComReqDto) {
        log.info("Обновление подборки с id={} с данными: {}", compId, updateComReqDto);
        Compilation compilation = compilationRepository.findById(compId)
                .orElseThrow(() -> new NotFoundException("Compilation", "Id", compId));
        if (updateComReqDto.getTitle() != null && !updateComReqDto.getTitle().isBlank() &&
                compilationRepository.existsByTitleIgnoreCaseAndTrim(updateComReqDto.getTitle()) &&
                !compilation.getTitle().equalsIgnoreCase(updateComReqDto.getTitle())) {
            throw new AlreadyExistsException("Compilation", "title", updateComReqDto.getTitle());
        }
        if (updateComReqDto.getTitle() != null && !updateComReqDto.getTitle().isBlank()) {
            compilation.setTitle(updateComReqDto.getTitle());
        }
        if (updateComReqDto.getPinned() != null) {
            compilation.setPinned(updateComReqDto.getPinned());
        }
        if (updateComReqDto.getEvents() != null) {
            compilation.getEvents().clear();
            Set<Event> events = (updateComReqDto.getEvents().isEmpty())
                    ? new HashSet<>()
                    : loadEvents(updateComReqDto.getEvents());
            compilation.setEvents(events);
        }
        Compilation updatedCompilation = compilationRepository.save(compilation);
        CompilationDto result = compilationMapper.toDto(updatedCompilation);
        log.info("Подборка успешно обновлена");
        return addConfirmedRequestsAndViews(result);
    }

    @Override
    public void deleteCompilation(Long compId) {
        log.info("Удаление подборки с id={}", compId);
        if (!compilationRepository.existsById(compId)) {
            throw new NotFoundException("Compilation", "Id", compId);
        }
        compilationRepository.deleteById(compId);
        log.info("Подборка с id={} успешно удалена", compId);
    }
}
