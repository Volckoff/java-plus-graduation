package ru.practicum.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.model.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    CategoryDto toDto(Category category);

    @Mapping(target = "id", ignore = true)
    Category toCategory(NewCategoryDto newCategoryDto);

}
