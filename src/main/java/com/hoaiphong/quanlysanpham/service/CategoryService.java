package com.hoaiphong.quanlysanpham.service;

import com.hoaiphong.quanlysanpham.base.CreateResponse;
import com.hoaiphong.quanlysanpham.base.PageResponse;
import com.hoaiphong.quanlysanpham.dto.request.CategoryCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.CategoryRequest;
import com.hoaiphong.quanlysanpham.dto.response.CategoryCreateResponse;
import com.hoaiphong.quanlysanpham.dto.response.CategorySearchResponse;
import com.hoaiphong.quanlysanpham.dto.response.CategoryUpdateResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface CategoryService {
    CreateResponse<CategoryCreateResponse> createCategory(CategoryCreateRequest request, List<MultipartFile> images);

    PageResponse<CategorySearchResponse> searchCategories(
            String name,
            String categoryCode,
            Date createdFrom,
            Date createdTo,
            int page,
            int size);

    CategoryUpdateResponse updateCategory(Long id, CategoryRequest request,List<MultipartFile> images);

    boolean deleteCategory(Long id);

    ByteArrayInputStream exportCategories(
            String name,
            String categoryCode,
            Date createdFrom,
            Date createdTo,
            int page,
            int size
    ) throws IOException;
}