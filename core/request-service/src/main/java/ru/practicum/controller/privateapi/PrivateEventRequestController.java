package ru.practicum.controller.privateapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventRequestStatusUpdateRequestDto;
import ru.practicum.dto.event.EventRequestStatusUpdateResultDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.service.RequestService;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events/{eventId}/requests")
@RequiredArgsConstructor
@Validated
public class PrivateEventRequestController {
    private final RequestService requestService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<ParticipationRequestDto> getEventRequests(@PathVariable @Positive Long userId,
                                                          @PathVariable @Positive Long eventId) {
        return requestService.getEventRequests(userId, eventId);
    }

    @PatchMapping
    @ResponseStatus(HttpStatus.OK)
    public EventRequestStatusUpdateResultDto updateRequestsStatus(@PathVariable @Positive Long userId,
                                                                  @PathVariable @Positive Long eventId,
                                                                  @RequestBody @Valid EventRequestStatusUpdateRequestDto
                                                                          updateRequest) {
        return requestService.changeRequestStatus(userId, eventId, updateRequest);
    }
}




