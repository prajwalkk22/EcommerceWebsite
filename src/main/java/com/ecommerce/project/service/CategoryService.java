package com.ecommerce.project.service;

import com.ecommerce.project.model.Category;

import java.util.List;

/**
 * To  promote loose coupling and modularity it is interface
 * */
public interface CategoryService {
        List<Category> getAllCategories();
        void createCategory(Category category);

         String deleteCategory(Long categoryId);

    Category updateCategory(Category category, Long categoryId);
}
