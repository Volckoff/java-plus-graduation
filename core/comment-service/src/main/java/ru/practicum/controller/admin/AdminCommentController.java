package ru.practicum.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.comment.CommentAdminDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.model.CommentStatus;
import ru.practicum.service.CommentService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
@Validated
public class AdminCommentController {

    private final CommentService commentService;

    @GetMapping
    public List<CommentDto> search(@RequestParam(required = false) CommentStatus status,
                                   @RequestParam(required = false) Long eventId,
                                   @RequestParam(required = false) Long authorId,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime start,
                                   @RequestParam(required = false)
                                   @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime end,
                                   @RequestParam(defaultValue = "0") @Min(0) int from,
                                   @RequestParam(defaultValue = "10") @Min(1) @Max(1000) int size) {
        return commentService.adminSearch(status, eventId, authorId, start, end, from, size);
    }

    @GetMapping("/events/{eventId}")
    @ResponseStatus(HttpStatus.OK)
    public List<CommentDto> getByStatusForEvent(@PathVariable @Positive Long eventId,
                                                @RequestParam(defaultValue = "CONFIRMED") CommentStatus status) {
        return commentService.getCommentByStatus(eventId, status);
    }

    @PatchMapping("/{commentId}")
    @ResponseStatus(HttpStatus.OK)
    public CommentDto patchComment(@PathVariable Long commentId,
                                   @Valid @RequestBody CommentAdminDto dto) {
        return commentService.patchCommentByAdmin(commentId, dto);
    }

    @DeleteMapping("/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(@PathVariable Long commentId) {
        commentService.deleteCommentByAdmin(commentId);
    }
}




