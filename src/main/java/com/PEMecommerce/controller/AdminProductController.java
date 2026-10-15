package com.PEMecommerce.controller;

import com.PEMecommerce.dao.ProductCategoryRepository;
import com.PEMecommerce.dao.ProductRepository;
import com.PEMecommerce.dto.ProductAdminResponse;
import com.PEMecommerce.dto.ProductRequest;
import com.PEMecommerce.entity.Product;
import com.PEMecommerce.entity.ProductCategory;
import com.PEMecommerce.service.ImageUploadException;
import com.PEMecommerce.service.ProductImageStorage;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/products")
@CrossOrigin("http://localhost:4200")
public class AdminProductController {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductImageStorage productImageStorage;

    public AdminProductController(ProductRepository productRepository,
                                  ProductCategoryRepository productCategoryRepository,
                                  ProductImageStorage productImageStorage) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productImageStorage = productImageStorage;
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImage(@RequestParam(value = "file", required = false) MultipartFile file) {
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("imageUrl", productImageStorage.store(file)));
        } catch (ImageUploadException exception) {
            return badRequest(exception.getMessage());
        } catch (IOException exception) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Could not store the image"));
        }
    }

    @GetMapping
    public List<ProductAdminResponse> listProducts() {
        return productRepository.findAll().stream()
                .sorted(Comparator.comparing(Product::getId))
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable Long id) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return notFound();
        }
        return ResponseEntity.ok(toResponse(product));
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest request) {
        String validationError = validate(request);
        if (validationError != null) {
            return badRequest(validationError);
        }
        String sku = request.getSku().trim();
        if (productRepository.existsBySku(sku)) {
            return conflict("A product with this SKU already exists");
        }
        ProductCategory category = productCategoryRepository.findById(request.getCategoryId()).orElse(null);
        if (category == null) {
            return badRequest("Category was not found");
        }

        Product product = new Product();
        apply(product, request, sku, category);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(productRepository.save(product)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(@PathVariable Long id, @RequestBody ProductRequest request) {
        Product product = productRepository.findById(id).orElse(null);
        if (product == null) {
            return notFound();
        }
        String validationError = validate(request);
        if (validationError != null) {
            return badRequest(validationError);
        }
        String sku = request.getSku().trim();
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            return conflict("A product with this SKU already exists");
        }
        ProductCategory category = productCategoryRepository.findById(request.getCategoryId()).orElse(null);
        if (category == null) {
            return badRequest("Category was not found");
        }

        apply(product, request, sku, category);
        return ResponseEntity.ok(toResponse(productRepository.save(product)));
    }

    private void apply(Product product, ProductRequest request, String sku, ProductCategory category) {
        product.setSku(sku);
        product.setName(request.getName().trim());
        product.setDescription(trimToEmpty(request.getDescription()));
        product.setUnitPrice(request.getUnitPrice());
        product.setImageUrl(imageUrlOrPlaceholder(request.getImageUrl()));
        product.setActive(request.getActive() == null || request.getActive());
        product.setUnitsInStock(request.getUnitsInStock());
        product.setCategory(category);
    }

    private String validate(ProductRequest request) {
        if (request == null) {
            return "Product data is required";
        }
        if (trimToNull(request.getSku()) == null) {
            return "SKU is required";
        }
        if (trimToNull(request.getName()) == null) {
            return "Name is required";
        }
        if (request.getUnitPrice() == null || request.getUnitPrice().signum() <= 0) {
            return "Unit price must be greater than zero";
        }
        if (request.getUnitsInStock() == null || request.getUnitsInStock() < 0) {
            return "Units in stock must be zero or greater";
        }
        if (request.getCategoryId() == null) {
            return "Category is required";
        }
        return null;
    }

    private ProductAdminResponse toResponse(Product product) {
        return new ProductAdminResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getUnitPrice(),
                product.getUnitsInStock(),
                product.getCategory().getId(),
                product.getImageUrl(),
                product.isActive()
        );
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }

    private ResponseEntity<Map<String, String>> notFound() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Product was not found"));
    }

    private ResponseEntity<Map<String, String>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", message));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimToEmpty(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? "" : trimmed;
    }

    private String imageUrlOrPlaceholder(String imageUrl) {
        String trimmed = trimToNull(imageUrl);
        if (trimmed == null) {
            return "assets/images/products/placeholder.png";
        }
        String slashed = trimmed.replace('\\', '/');
        String lower = slashed.toLowerCase();
        boolean remote = lower.startsWith("http://") || lower.startsWith("https://");
        if (!remote) {
            int assetsAt = lower.indexOf("assets/");
            if (assetsAt >= 0) {
                return slashed.substring(assetsAt);
            }
            if (lower.startsWith("file:") || lower.matches("^[a-z]:/.*")) {
                return "assets/images/products/placeholder.png";
            }
        }
        return slashed;
    }
}
