package ru.practicum.client.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.event.EventFullDto;

@Slf4j
@Component
public class EventClientFallback implements EventOperation {

    @Override
    public EventFullDto getEventById(Long eventId) {
        log.warn("Event service is unavailable. Fallback: returning null for event ID: {}", eventId);
        return null;
    }

    @Override
    public EventFullDto getPublicEventById(Long eventId) {
        log.warn("Event service is unavailable. Fallback: returning null for event ID: {}", eventId);
        return null;
    }
}




