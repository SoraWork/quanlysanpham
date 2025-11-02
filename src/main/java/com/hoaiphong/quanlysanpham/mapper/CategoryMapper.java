package com.hoaiphong.quanlysanpham.mapper;

import com.hoaiphong.quanlysanpham.dto.request.CategoryCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.CategoryRequest;
import com.hoaiphong.quanlysanpham.dto.response.CategorySearchResponse;
import com.hoaiphong.quanlysanpham.dto.response.CategoryUpdateResponse;
import com.hoaiphong.quanlysanpham.entity.Category;
import com.hoaiphong.quanlysanpham.entity.Image;
import com.hoaiphong.quanlysanpham.service.impl.FileStorageServiceImpl;
import org.mapstruct.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Mapper(componentModel = "spring", uses = {ImageMapper.class})
public interface CategoryMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "createdDate", expression = "java(new java.util.Date())")
    @Mapping(target = "modifiedDate", expression = "java(new java.util.Date())")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
    @Mapping(target = "productCategories", ignore = true)
    @Mapping(target = "images", ignore = true)
    Category toEntity(CategoryCreateRequest request, @Context List<MultipartFile> images, @Context FileStorageServiceImpl fileStorageServiceImpl);

    @AfterMapping
    default void mapImages(@MappingTarget Category category,
                           @Context List<MultipartFile> images,
                           @Context FileStorageServiceImpl fileStorageServiceImpl) {
        if (images == null || images.isEmpty()) {
            return;
        }

        Set<Image> imageEntities = new HashSet<>();
        for (MultipartFile file : images) {
            String url = fileStorageServiceImpl.save(file);

            Image img = new Image();
            img.setName(file.getOriginalFilename());
            img.setUrl(url);
            img.setStatus("1");
            img.setCreatedDate(new Date());
            img.setModifiedDate(new Date());
            img.setCategory(category);
            imageEntities.add(img);
        }

        category.setImages(imageEntities);
    }

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "productCategories", ignore = true)
    Category toEntity(CategoryRequest request);

    // Entity → Response
    @Mapping(target = "images", ignore = true)
    CategorySearchResponse toSearchResponse(Category entity);

    CategoryUpdateResponse toUpdateResponse(Category entity);

    List<CategorySearchResponse> toSearchResponseList(List<Category> entities);
}
