package ru.practicum.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.CommentAdminDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.*;
import ru.practicum.repository.CommentRepository;
import ru.practicum.repository.EventRepository;
import ru.practicum.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        User author = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toComment(newCommentDto, author, event);

        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentDto dto) {
        Comment comment = getCommentOrThrow(commentId, userId);

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно редактировать только комментарии в статусе PENDING");
        }

        commentMapper.patchFromDto(dto, comment);

        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteCommentByUser(Long userId, Long commentId) {
        Comment comment = getCommentOrThrow(commentId, userId);
        commentRepository.delete(comment);
    }

    @Override
    public List<CommentDto> getCommentsByEvent(Long eventId, int from, int size) {
        getEventOrThrow(eventId);
        return commentRepository.findPublishedByEvent(eventId, PageRequest.of(from / size, size))
                .stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> adminSearch(CommentStatus status,
                                        Long eventId,
                                        Long authorId,
                                        LocalDateTime start,
                                        LocalDateTime end,
                                        int from, int size) {

        Pageable pageable = PageRequest.of(from / size, size);

        Page<Comment> result;

        if (start == null && end == null) {
            result = commentRepository.searchWithoutDates(status, eventId, authorId, pageable);
        } else if (start == null) {
            result = commentRepository.searchWithDates(status, eventId, authorId, LocalDateTime.MIN, end, pageable);
        } else if (end == null) {
            result = commentRepository.searchWithDates(status, eventId, authorId, start, LocalDateTime.MAX, pageable);
        } else {
            result = commentRepository.searchWithDates(status, eventId, authorId, start, end, pageable);
        }

        return result.getContent().stream()
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentByStatus(Long eventId, CommentStatus status) {
        getEventOrThrow(eventId);

        return commentRepository.findByEventIdAndStatus(eventId, status).stream()
                .filter(comment -> comment.getStatus().equals(status))
                .map(commentMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentDto patchCommentByAdmin(Long commentId, CommentAdminDto dto) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Comment", "id", commentId));

        if (dto.getStatus() == CommentStatus.PENDING) {
            throw new ConflictException("Целевой статус модерации не может быть PENDING");
        }

        commentMapper.patchFromAdminDto(dto, comment);

        return commentMapper.toDto(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Comment", "id", commentId);
        }

        commentRepository.deleteById(commentId);
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User", "id", userId));
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new NotFoundException("Event", "id", eventId));
    }

    private Comment getCommentOrThrow(Long commentId, Long userId) {
        return commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment", "id", commentId));
    }
}
