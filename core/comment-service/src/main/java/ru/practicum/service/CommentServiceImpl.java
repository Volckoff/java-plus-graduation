package ru.practicum.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.client.event.EventClient;
import ru.practicum.client.user.UserClient;
import ru.practicum.dto.comment.CommentAdminDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.Comment;
import ru.practicum.model.CommentStatus;
import ru.practicum.model.EventState;
import ru.practicum.repository.CommentRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final EventClient eventClient;
    private final UserClient userClient;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public CommentDto createComment(Long userId, Long eventId, NewCommentDto newCommentDto) {
        getUserOrThrow(userId);
        EventFullDto event = getEventOrThrow(eventId);

        if (event.getState() != EventState.PUBLISHED) {
            throw new ConflictException("Нельзя комментировать неопубликованное событие");
        }

        Comment comment = commentMapper.toComment(newCommentDto, userId, eventId);

        Comment saved = commentRepository.save(comment);
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(Set.of(userId));
        return buildCommentDto(saved, usersMap);
    }

    @Override
    @Transactional
    public CommentDto updateCommentByUser(Long userId, Long commentId, UpdateCommentDto dto) {
        Comment comment = getCommentOrThrow(commentId, userId);

        if (comment.getStatus() != CommentStatus.PENDING) {
            throw new ConflictException("Можно редактировать только комментарии в статусе PENDING");
        }

        commentMapper.patchFromDto(dto, comment);

        Comment saved = commentRepository.save(comment);
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(Set.of(userId));
        return buildCommentDto(saved, usersMap);
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
        List<Comment> comments = commentRepository.findPublishedByEvent(eventId,
                PageRequest.of(from / size, size));
        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(authorIds);
        
        return comments.stream()
                .map(comment -> buildCommentDto(comment, usersMap))
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

        List<Comment> comments = result.getContent();
        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(authorIds);
        
        return comments.stream()
                .map(comment -> buildCommentDto(comment, usersMap))
                .collect(Collectors.toList());
    }

    @Override
    public List<CommentDto> getCommentByStatus(Long eventId, CommentStatus status) {
        getEventOrThrow(eventId);

        List<Comment> comments = commentRepository.findByEventIdAndStatus(eventId, status).stream()
                .filter(comment -> comment.getStatus().equals(status))
                .collect(Collectors.toList());
        Set<Long> authorIds = comments.stream()
                .map(Comment::getAuthorId)
                .collect(Collectors.toSet());
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(authorIds);
        
        return comments.stream()
                .map(comment -> buildCommentDto(comment, usersMap))
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

        Comment saved = commentRepository.save(comment);
        Map<Long, UserShortDto> usersMap = getUserShortDtoMap(Set.of(comment.getAuthorId()));
        return buildCommentDto(saved, usersMap);
    }

    @Override
    @Transactional
    public void deleteCommentByAdmin(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new NotFoundException("Comment", "id", commentId);
        }

        commentRepository.deleteById(commentId);
    }

    private void getUserOrThrow(Long userId) {
        ru.practicum.dto.user.UserDto user = userClient.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("User", "id", userId);
        }
    }

    private EventFullDto getEventOrThrow(Long eventId) {
        EventFullDto event = eventClient.getEventById(eventId);
        if (event == null) {
            event = eventClient.getPublicEventById(eventId);
            if (event == null) {
                throw new NotFoundException("Event", "id", eventId);
            }
        }
        return event;
    }

    private Comment getCommentOrThrow(Long commentId, Long userId) {
        return commentRepository.findByIdAndAuthorId(commentId, userId)
                .orElseThrow(() -> new NotFoundException("Comment", "id", commentId));
    }

    private CommentDto buildCommentDto(Comment comment, Map<Long, UserShortDto> usersMap) {
        CommentDto dto = commentMapper.toDto(comment);
        dto.setAuthor(usersMap.get(comment.getAuthorId()));
        return dto;
    }

    private Map<Long, UserShortDto> getUserShortDtoMap(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<ru.practicum.dto.user.UserDto> users = userClient.getUsersByIds(List.copyOf(userIds));
        return users.stream()
                .collect(Collectors.toMap(
                        ru.practicum.dto.user.UserDto::getId,
                        user -> UserShortDto.builder()
                                .id(user.getId())
                                .name(user.getName())
                                .build()
                ));
    }
}




