package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.category.CategoryDto;
import ru.practicum.dto.category.NewCategoryDto;
import ru.practicum.exception.AlreadyExistsException;
import ru.practicum.exception.DeletedException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.CategoryMapper;
import ru.practicum.model.Category;
import ru.practicum.repository.CategoryRepository;
import ru.practicum.repository.EventRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final EventRepository eventRepository;

    @Override
    @Transactional
    public CategoryDto addCategory(NewCategoryDto newCategoryDto) {
        log.info("Попытка добавления категории: {}", newCategoryDto);
        if (!categoryRepository.existsByNameIgnoreCaseAndTrim(newCategoryDto.getName())) {
            return categoryMapper.toDto(categoryRepository
                    .save(categoryMapper.toCategory(newCategoryDto)));
        } else {
            throw new AlreadyExistsException("Category", "name", newCategoryDto.getName());
        }
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        log.info("Попытка удаления категории с ID {}", categoryId);
        if (categoryRepository.findById(categoryId).isEmpty()) {
            throw new NotFoundException("Category", "Id", categoryId);
        }
        if (eventRepository.existsByCategoryId(categoryId)) {
            throw new DeletedException("Category", "name", categoryId);
        } else {
            categoryRepository.deleteById(categoryId);
        }
    }

    @Override
    @Transactional
    public CategoryDto updateCategory(Long categoryId, NewCategoryDto newCategoryDto) {
        log.info("Попытка обновления категории с ID {}: новые данные {}", categoryId, newCategoryDto);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", "Id", categoryId));
        if (categoryRepository.existsByNameIgnoreCaseAndTrim(newCategoryDto.getName()) &&
                !category.getName().equalsIgnoreCase(newCategoryDto.getName())) {
            throw new AlreadyExistsException("Category", "name", newCategoryDto.getName());
        }
        if (newCategoryDto.getName() != null && !newCategoryDto.getName().isBlank()) {
            category.setName(newCategoryDto.getName());
        }
        return categoryMapper.toDto(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long categoryId) {
        log.info("Получение категории с ID: {}", categoryId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Category", "Id", categoryId));
        return categoryMapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories(int from, int size) {
        log.info("Получение всех категорий с параметрами: from={}, size={}", from, size);
        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).stream()
                .map(categoryMapper::toDto)
                .sorted((c1, c2) -> c1.getName().compareToIgnoreCase(c2.getName()))
                .collect(Collectors.toList());
    }
}
