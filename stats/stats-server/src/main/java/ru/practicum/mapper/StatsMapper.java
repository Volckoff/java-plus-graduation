package ru.practicum.mapper;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.practicum.model.EndpointHit;
import ru.practicum.EndpointHitDto;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StatsMapper {
    public static EndpointHit toEntity(EndpointHitDto dto) {
        return EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
    }
}
