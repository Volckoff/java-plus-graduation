package ru.practicum.client.request;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.model.RequestStatus;

public interface RequestOperation {

    @GetMapping("/admin/requests/count/{eventId}")
    Long getConfirmedRequestsCount(@PathVariable @NotNull Long eventId, 
                                   @RequestParam(name = "status") RequestStatus status);
}

