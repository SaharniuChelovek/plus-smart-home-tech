package ru.yandex.practicum.product.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;

@Component
@RequiredArgsConstructor
public class ProductMapper {

    private final CategoryMapper categoryMapper;

    public Product toEntity(CreateProductRequest r, Category category) {
        Product p = new Product();
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setImageUrl(r.imageUrl());
        p.setCategory(category);
        return p;
    }

    public ProductDto toDto(Product p) {
        return new ProductDto(p.getId(), p.getName(), p.getDescription(),
                p.getPrice(), categoryMapper.toDto(p.getCategory()), p.getImageUrl(), p.isActive());
    }


    public void applyUpdate(Product p, UpdateProductRequest r) {
        if (r.name() != null) p.setName(r.name());
        if (r.description() != null) p.setDescription(r.description());
        if (r.price() != null) p.setPrice(r.price());
        if (r.imageUrl() != null) p.setImageUrl(r.imageUrl());
        if (r.active() != null) p.setActive(r.active());
    }
}
