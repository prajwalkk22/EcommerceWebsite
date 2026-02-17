package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repository.CategoryRepository;
import com.ecommerce.project.repository.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;

    // ✅ Add Product
    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));

        boolean productExists = category.getProducts().stream()
                .anyMatch(p -> p.getProductName().equalsIgnoreCase(productDTO.getProductName()));

        if (productExists) {
            throw new APIException("Product already exists in this category");
        }

        Product product = modelMapper.map(productDTO, Product.class);
        product.setCategory(category);
        product.setImage("default.png");

        double specialPrice = product.getPrice()
                - ((product.getDiscount() * 0.01) * product.getPrice());
        product.setSpecialPrice(specialPrice);

        Product savedProduct = productRepository.save(product);
        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    // ✅ Get All Products (Pagination + Sorting)
    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize,
                                          String sortBy, String sortOrder) {

        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize)
                .withSort(sort);   // ✅ VERSION SAFE FIX

        Page<Product> pageProducts = productRepository.findAll(pageable);

        return buildProductResponse(pageProducts);
    }

    // ✅ Get Products By Category
    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber,
                                            Integer pageSize, String sortBy, String sortOrder) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize)
                .withSort(sort);

        Page<Product> pageProducts =
                productRepository.findByCategory(category, pageable); // ✅ FIXED

        if (pageProducts.isEmpty()) {
            throw new APIException(category.getCategoryName() + " category has no products");
        }

        return buildProductResponse(pageProducts);
    }

    // ✅ Search Products By Keyword
    @Override
    public ProductResponse searchProductByKeyword(String keyword, Integer pageNumber,
                                                  Integer pageSize, String sortBy, String sortOrder) {

        Sort sort = sortOrder.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(pageNumber, pageSize)
                .withSort(sort);

        Page<Product> pageProducts =
                productRepository.findByProductNameLikeIgnoreCase(
                        "%" + keyword + "%", pageable);

        if (pageProducts.isEmpty()) {
            throw new APIException("No products found with keyword: " + keyword);
        }

        return buildProductResponse(pageProducts);
    }

    // ✅ Update Product
    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {

        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "productId", productId));

        productFromDb.setProductName(productDTO.getProductName());
        productFromDb.setDescription(productDTO.getDescription());
        productFromDb.setQuantity(productDTO.getQuantity());
        productFromDb.setDiscount(productDTO.getDiscount());
        productFromDb.setPrice(productDTO.getPrice());

        // ✅ ALWAYS recalculate special price
        double specialPrice = productFromDb.getPrice()
                - ((productFromDb.getDiscount() * 0.01) * productFromDb.getPrice());
        productFromDb.setSpecialPrice(specialPrice);

        Product updatedProduct = productRepository.save(productFromDb);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    // ✅ Delete Product
    @Override
    public ProductDTO deleteProduct(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "productId", productId));

        productRepository.delete(product);
        return modelMapper.map(product, ProductDTO.class);
    }

    // ✅ Update Product Image
    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image)
            throws IOException {

        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Product", "productId", productId));

        String fileName = fileService.uploadImage(path, image);
        productFromDb.setImage(fileName);

        Product updatedProduct = productRepository.save(productFromDb);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    // 🔁 Common Response Builder
    private ProductResponse buildProductResponse(Page<Product> pageProducts) {

        List<ProductDTO> productDTOS = pageProducts.getContent()
                .stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        ProductResponse response = new ProductResponse();
        response.setContent(productDTOS);
        response.setPageNumber(pageProducts.getNumber());
        response.setPageSize(pageProducts.getSize());
        response.setTotalElements(pageProducts.getTotalElements());
        response.setTotalPages(pageProducts.getTotalPages());
        response.setLastPage(pageProducts.isLast());

        return response;
    }
}
