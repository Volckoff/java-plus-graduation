package ru.practicum.controller.pub;

import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.service.CommentService;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class PublicCommentController {

    private final CommentService commentService;

    @GetMapping("/{eventId}/comments")
    public List<CommentDto> getComments(@PathVariable Long eventId,
                                        @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                        @RequestParam(defaultValue = "10") @Positive int size) {
        return commentService.getCommentsByEvent(eventId, from, size);
    }
}
