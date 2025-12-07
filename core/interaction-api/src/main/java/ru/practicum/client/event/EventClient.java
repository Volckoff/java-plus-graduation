package ru.practicum.client.event;

import org.springframework.cloud.openfeign.FeignClient;

@FeignClient(name = "event-service", fallback = EventClientFallback.class)
public interface EventClient extends EventOperation {
}

