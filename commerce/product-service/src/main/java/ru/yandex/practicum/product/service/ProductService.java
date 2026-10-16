package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    @Transactional(readOnly = true)
    public ProductDto getById(Long id) {
        return productMapper.toDto(findProduct(id));
    }

    @Transactional
    public ProductDto create(CreateProductRequest request) {
        Category category = categoryService.getEntityById(request.categoryId());
        Product saved = productRepository.save(productMapper.toEntity(request, category));
        return productMapper.toDto(saved);
    }

    @Transactional
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = findProduct(id);
        productMapper.applyUpdate(product, request);
        if (request.categoryId() != null) {
            product.setCategory(categoryService.getEntityById(request.categoryId()));
        }
        return productMapper.toDto(product);
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAllActive() {
        return productRepository.findByActiveTrue().stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getByCategory(Long categoryId) {
        categoryService.getEntityById(categoryId); // бросит NotFoundException, если категории нет
        return productRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDto> search(String query) {
        return productRepository.findByNameContainingIgnoreCaseAndActiveTrue(query).stream()
                .map(productMapper::toDto)
                .toList();
    }


}
