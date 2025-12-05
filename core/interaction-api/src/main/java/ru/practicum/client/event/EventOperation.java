package ru.practicum.client.event;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.event.EventFullDto;

public interface EventOperation {

    @GetMapping("/admin/events/{eventId}")
    EventFullDto getEventById(@PathVariable @NotNull Long eventId);

    @GetMapping("/events/{eventId}")
    EventFullDto getPublicEventById(@PathVariable @NotNull Long eventId);
}




