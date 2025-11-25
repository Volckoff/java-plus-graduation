package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.comment.CommentAdminDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.model.Comment;
import ru.practicum.model.Event;
import ru.practicum.model.User;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "author", source = "author")
    @Mapping(target = "event", source = "event")
    @Mapping(target = "status", expression = "java(ru.practicum.model.CommentStatus.PENDING)")
    Comment toComment(NewCommentDto newCommentDto, User author, Event event);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "status", ignore = true)
    void patchFromDto(UpdateCommentDto updateCommentDto, @MappingTarget Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "event", ignore = true)
    @Mapping(target = "text", source = "text")
    @Mapping(target = "status", source = "status")
    void patchFromAdminDto(CommentAdminDto commentAdminDto, @MappingTarget Comment comment);

    @Mapping(target = "author", source = "author")
    @Mapping(target = "eventId", source = "event.id")
    CommentDto toDto(Comment comment);

}
