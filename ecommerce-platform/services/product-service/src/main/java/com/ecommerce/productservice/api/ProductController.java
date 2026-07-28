package com.ecommerce.productservice.api;

import com.ecommerce.platform.common.ApiResponse;
import com.ecommerce.platform.common.PageResponse;
import com.ecommerce.productservice.api.dto.CreateProductRequest;
import com.ecommerce.productservice.api.dto.ProductResponse;
import com.ecommerce.productservice.api.dto.UpdateProductRequest;
import com.ecommerce.productservice.application.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProductResponse> create(@Valid @RequestBody CreateProductRequest request) {
        var product = productService.create(request.name(), request.description(), request.category(),
                request.price(), request.stock());
        return ApiResponse.success(ProductResponse.from(product), "Produto cadastrado com sucesso");
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> findById(@PathVariable UUID id) {
        return ApiResponse.success(ProductResponse.from(productService.findById(id)));
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> findAll(Pageable pageable) {
        var page = productService.findAll(pageable).map(ProductResponse::from);
        return ApiResponse.success(PageResponse.from(page));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductResponse> update(@PathVariable UUID id, @Valid @RequestBody UpdateProductRequest request) {
        var product = productService.update(id, request.name(), request.description(), request.category(),
                request.price(), request.stock());
        return ApiResponse.success(ProductResponse.from(product), "Produto atualizado com sucesso");
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}
