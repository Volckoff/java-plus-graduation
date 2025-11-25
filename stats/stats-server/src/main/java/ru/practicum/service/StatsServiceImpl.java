package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.EndpointHitDto;
import ru.practicum.ViewStatsDto;
import ru.practicum.mapper.StatsMapper;
import ru.practicum.model.EndpointHit;
import ru.practicum.repository.StatsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsServiceImpl implements StatsService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StatsRepository statsRepository;

    @Transactional
    @Override
    public void save(EndpointHitDto endpointHitDto) {
        log.debug("Попытка сохранить просмотр: {}", endpointHitDto);
        if (endpointHitDto == null) {
            log.warn("Невозможно сохранить просмотр — параметр EndpointHitDto равен null.");
            throw new IllegalArgumentException("Параметр EndpointHitDto не может быть null.");
        }
        EndpointHit endpointHit = StatsMapper.toEntity(endpointHitDto);
        statsRepository.save(endpointHit);
        log.info("Просмотр успешно сохранен");
    }

    @Override
    public List<ViewStatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start, FORMATTER);
        LocalDateTime endTime = LocalDateTime.parse(end, FORMATTER);

        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("Начало должно быть до окончания");
        }

        return unique ?
                statsRepository.findUniqueStats(startTime, endTime, uris)
                : statsRepository.findAllStats(startTime, endTime, uris);
    }
}
