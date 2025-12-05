package ru.practicum.mapper;

import org.mapstruct.*;
import ru.practicum.dto.comment.CommentAdminDto;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.comment.UpdateCommentDto;
import ru.practicum.model.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "authorId", source = "authorId")
    @Mapping(target = "eventId", source = "eventId")
    @Mapping(target = "status", expression = "java(ru.practicum.model.CommentStatus.PENDING)")
    Comment toComment(NewCommentDto newCommentDto, Long authorId, Long eventId);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "status", ignore = true)
    void patchFromDto(UpdateCommentDto updateCommentDto, @MappingTarget Comment comment);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdOn", ignore = true)
    @Mapping(target = "updatedOn", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "eventId", ignore = true)
    @Mapping(target = "text", source = "text")
    @Mapping(target = "status", source = "status")
    void patchFromAdminDto(CommentAdminDto commentAdminDto, @MappingTarget Comment comment);

    @Mapping(target = "author", ignore = true)
    @Mapping(target = "eventId", source = "eventId")
    CommentDto toDto(Comment comment);

}

