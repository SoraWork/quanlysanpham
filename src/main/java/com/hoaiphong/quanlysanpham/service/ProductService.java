package com.hoaiphong.quanlysanpham.service;

import com.hoaiphong.quanlysanpham.base.CreateResponse;
import com.hoaiphong.quanlysanpham.dto.request.ProductCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.ProductRequest;
import com.hoaiphong.quanlysanpham.dto.response.ProductCreateResponse;
import com.hoaiphong.quanlysanpham.dto.response.ProductSearchResponse;
import com.hoaiphong.quanlysanpham.dto.response.ProductUpdateResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public interface ProductService {
    CreateResponse<ProductCreateResponse> createProduct(ProductCreateRequest request, List<MultipartFile> images);
    Page<ProductSearchResponse> searchProducts(
            String name,
            String productCode,
            Date createdFrom,
            Date createdTo,
            Long categoryId,
            Pageable pageable
    );

    ProductUpdateResponse updateProduct(Long id, ProductRequest request, List<MultipartFile> images);

    boolean deleteProduct(Long id);

    ByteArrayInputStream exportProducts(
            String name,
            String productCode,
            Date createdFrom,
            Date createdTo,
            Long categoryId,
            int page,
            int size
    ) throws IOException;
}
