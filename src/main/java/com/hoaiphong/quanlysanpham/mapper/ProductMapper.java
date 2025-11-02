package com.hoaiphong.quanlysanpham.mapper;

import com.hoaiphong.quanlysanpham.dto.request.ProductCreateRequest;
import com.hoaiphong.quanlysanpham.dto.response.ProductCreateResponse;
import com.hoaiphong.quanlysanpham.dto.response.ProductSearchResponse;
import com.hoaiphong.quanlysanpham.dto.response.ProductUpdateResponse;
import com.hoaiphong.quanlysanpham.entity.Image;
import com.hoaiphong.quanlysanpham.entity.Product;
import com.hoaiphong.quanlysanpham.service.impl.FileStorageServiceImpl;
import org.mapstruct.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {ImageMapper.class})
public interface ProductMapper {
    // DTO → Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "1")
    @Mapping(target = "productCategories", ignore = true)
    @Mapping(target = "createdDate", expression = "java(new java.util.Date())")
    @Mapping(target = "modifiedDate", expression = "java(new java.util.Date())")
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "modifiedBy", ignore = true)
//    @Mapping(target = "productCategories", ignore = true)
    @Mapping(target = "images", ignore = true)
    Product toEntity(ProductCreateRequest request, @Context List<MultipartFile> images, @Context FileStorageServiceImpl fileStorageServiceImpl);

    @AfterMapping
    default void mapImages(@MappingTarget Product product,
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
            img.setProduct(product);
            imageEntities.add(img);
        }

        product.setImages(imageEntities);
    }
    // Entity → Response
    ProductCreateResponse toCreateResponse(Product entity);


    @Mapping(target = "categories", ignore = true)
    ProductSearchResponse toSearchResponse(Product product);
    @AfterMapping
    default void mapCategories(Product product, @MappingTarget ProductSearchResponse response) {
        if (product.getProductCategories() != null && !product.getProductCategories().isEmpty()) {
            String categoryNames = product.getProductCategories().stream()
                    .filter(pc -> pc.getCategory() != null)
                    .map(pc -> pc.getCategory().getName())
                    .collect(Collectors.joining(", "));
            response.setCategories(categoryNames);
        }
    }
    List<ProductSearchResponse> toSearchResponseList(List<Product> entities);

    @Mapping(target = "categories", ignore = true)
    ProductUpdateResponse toUpdateResponse(Product product);
    @AfterMapping
    default void mapCategories(Product product, @MappingTarget ProductUpdateResponse response) {
        if (product.getProductCategories() != null && !product.getProductCategories().isEmpty()) {
            String categoryNames = product.getProductCategories().stream()
                    .filter(pc -> pc.getCategory() != null)
                    .map(pc -> pc.getCategory().getName())
                    .collect(Collectors.joining(", "));
            response.setCategories(categoryNames);
        }
    }
    List<ProductUpdateResponse> toUpdateResponseList(List<Product> entities);
}
