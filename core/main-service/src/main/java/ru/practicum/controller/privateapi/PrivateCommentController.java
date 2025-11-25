package ru.practicum.controller.privateapi;

import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.service.CommentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/users/{userId}")
@RequiredArgsConstructor
@Validated
public class PrivateCommentController {

    private final CommentService commentService;

    @PostMapping("/events/{eventId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentDto createComment(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long eventId,
                                    @Valid @RequestBody NewCommentDto dto) {
        return commentService.createComment(userId, eventId, dto);
    }

    @PatchMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto updateComment(@PathVariable @Positive Long userId,
                                    @PathVariable @Positive Long commentId,
                                    @Valid @RequestBody UpdateCommentDto dto) {
        return commentService.updateCommentByUser(userId, commentId, dto);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable @Positive Long userId,
                              @PathVariable @Positive Long commentId) {
        commentService.deleteCommentByUser(userId, commentId);
    }
}