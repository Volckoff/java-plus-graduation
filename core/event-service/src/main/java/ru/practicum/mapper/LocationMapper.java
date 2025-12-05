package ru.practicum.mapper;

import org.mapstruct.Mapper;
import ru.practicum.dto.event.LocationDto;
import ru.practicum.model.Location;

@Mapper(componentModel = "spring")
public interface LocationMapper {

    Location toLocation(LocationDto dto);

    LocationDto toDto(Location location);
}
