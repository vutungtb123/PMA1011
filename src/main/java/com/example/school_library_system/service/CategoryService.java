package com.example.school_library_system.service;

import com.example.school_library_system.entity.Category;
import com.example.school_library_system.repository.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CategoryService {
    @Autowired
    private CategoryRepository categoryRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Integer id) {
        if (id == null) return null;
        return categoryRepository.findById(id).orElse(null);
    }

    public Category saveCategory(Category category) {
        return categoryRepository.save(Objects.requireNonNull(category, "Category must not be null"));
    }

    public void deleteCategory(Integer id) {
        if (id == null) return;
        categoryRepository.deleteById(id);
    }
}
