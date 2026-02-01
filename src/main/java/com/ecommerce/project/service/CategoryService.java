package com.ecommerce.project.service;

import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;


/**
 * To  promote loose coupling and modularity it is interface
 * */
public interface CategoryService {
        CategoryResponse getAllCategories(Integer pageNumber,Integer pageSize,String sortBy,String sortOrder);
        CategoryDTO createCategory(CategoryDTO categoryDTO);

        CategoryDTO deleteCategory(Long categoryId);

        CategoryDTO updateCategory(CategoryDTO category, Long categoryId);
}
