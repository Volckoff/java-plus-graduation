package ru.practicum.mapper;

import org.springframework.stereotype.Component;
import ru.practicum.dto.comment.CommentAdminDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.model.CommentStatus;

@Component
public class CommentMapper {

    public Comment toComment(NewCommentDto newCommentDto, Long authorId, Long eventId) {
        if (newCommentDto == null && authorId == null && eventId == null) {
            return null;
        }
        return Comment.builder()
                .text(newCommentDto != null ? newCommentDto.getText() : null)
                .authorId(authorId)
                .eventId(eventId)
                .status(CommentStatus.PENDING)
                .build();
    }

    public void patchFromDto(UpdateCommentDto updateCommentDto, Comment comment) {
        if (updateCommentDto == null || comment == null) {
            return;
        }
        if (updateCommentDto.getText() != null) {
            comment.setText(updateCommentDto.getText());
        }
    }

    public void patchFromAdminDto(CommentAdminDto commentAdminDto, Comment comment) {
        if (commentAdminDto == null || comment == null) {
            return;
        }
        if (commentAdminDto.getText() != null) {
            comment.setText(commentAdminDto.getText());
        }
        if (commentAdminDto.getStatus() != null) {
            comment.setStatus(commentAdminDto.getStatus());
        }
    }

    public CommentDto toDto(Comment comment) {
        if (comment == null) {
            return null;
        }
        return CommentDto.builder()
                .id(comment.getId())
                .eventId(comment.getEventId())
                .text(comment.getText())
                .createdOn(comment.getCreatedOn())
                .updatedOn(comment.getUpdatedOn())
                .status(comment.getStatus())
                .build();
    }
}
