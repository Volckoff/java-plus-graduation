package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.user.NewUserRequestDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserShortDto;
import ru.practicum.model.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(UserDto userDto);

    UserDto toUserDto(User user);

    @Mapping(target = "id", ignore = true)
    User toUser(NewUserRequestDto newUserDto);

    UserShortDto toShortDto(User user);
}
