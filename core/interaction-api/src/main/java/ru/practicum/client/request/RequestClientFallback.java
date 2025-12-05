package ru.practicum.client.request;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.model.RequestStatus;

@Slf4j
@Component
public class RequestClientFallback implements RequestOperation {

    @Override
    public Long getConfirmedRequestsCount(Long eventId, RequestStatus status) {
        log.warn("Request service is unavailable. Fallback: returning 0 for event ID: {}", eventId);
        return 0L;
    }
}




