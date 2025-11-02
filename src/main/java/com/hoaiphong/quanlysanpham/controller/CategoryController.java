package com.hoaiphong.quanlysanpham.controller;

import com.hoaiphong.quanlysanpham.base.CreateResponse;
import com.hoaiphong.quanlysanpham.base.PageResponse;
import com.hoaiphong.quanlysanpham.configuration.Translator;
import com.hoaiphong.quanlysanpham.dto.request.CategoryCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.CategoryRequest;
import com.hoaiphong.quanlysanpham.dto.response.CategoryCreateResponse;
import com.hoaiphong.quanlysanpham.dto.response.CategorySearchResponse;
import com.hoaiphong.quanlysanpham.dto.response.CategoryUpdateResponse;
import com.hoaiphong.quanlysanpham.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CreateResponse<CategoryCreateResponse>> createCategory(
            @Valid @ModelAttribute CategoryCreateRequest request,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        CreateResponse<CategoryCreateResponse> response = categoryService.createCategory(request, images);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<PageResponse<CategorySearchResponse>> searchCategories(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String categoryCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        PageResponse<CategorySearchResponse> response = categoryService.searchCategories(
                name, categoryCode, createdFrom, createdTo, page, size
        );
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryUpdateResponse> updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute CategoryRequest request,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        CategoryUpdateResponse response = categoryService.updateCategory(id, request, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("message", Translator.toLocale("category.delete.success")));
    }
    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportCategories(
            @RequestParam(required = false) String name,
            @RequestParam(required = false, name = "category_code") String categoryCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {
        ByteArrayInputStream in = categoryService.exportCategories(name, categoryCode, createdFrom, createdTo,page,size);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=categories.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
