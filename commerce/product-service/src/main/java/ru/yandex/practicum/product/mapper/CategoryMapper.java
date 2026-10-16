package ru.yandex.practicum.product.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest r) {
        Category c = new Category();
        c.setName(r.name());
        c.setDescription(r.description());
        return c;
    }

    public CategoryDto toDto(Category c) {
        return new CategoryDto(c.getId(), c.getName(), c.getDescription());
    }
}
