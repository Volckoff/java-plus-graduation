package ru.practicum.controller.admin;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.model.RequestStatus;
import ru.practicum.service.RequestService;

@RestController
@RequestMapping("/admin/requests")
@RequiredArgsConstructor
@Validated
public class AdminRequestController {

    private final RequestService requestService;

    @GetMapping("/count/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public Long getConfirmedRequestsCount(@PathVariable @NotNull Long eventId,
                                          @RequestParam(name = "status") RequestStatus status) {
        return requestService.getConfirmedRequestsCount(eventId, status);
    }
}

