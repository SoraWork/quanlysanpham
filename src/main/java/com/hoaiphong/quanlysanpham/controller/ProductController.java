package com.hoaiphong.quanlysanpham.controller;

import com.hoaiphong.quanlysanpham.base.CreateResponse;
import com.hoaiphong.quanlysanpham.base.PageResponse;
import com.hoaiphong.quanlysanpham.configuration.Translator;
import com.hoaiphong.quanlysanpham.dto.request.CategoryCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.CategoryRequest;
import com.hoaiphong.quanlysanpham.dto.request.ProductCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.ProductRequest;
import com.hoaiphong.quanlysanpham.dto.response.*;
import com.hoaiphong.quanlysanpham.exception.SomeThingWrongException;
import com.hoaiphong.quanlysanpham.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @PostMapping
    public ResponseEntity<CreateResponse<ProductCreateResponse>> createProduct(
            @Valid @ModelAttribute ProductCreateRequest request,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        CreateResponse<ProductCreateResponse> response = productService.createProduct(request, images);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/search")
    public PageResponse<ProductSearchResponse> searchProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String product_code,
            @RequestParam(required = false) String createdFrom,
            @RequestParam(required = false) String createdTo,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size);

        //  Parse chuỗi ngày sang Date (nếu có)
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date fromDate = null;
        Date toDate = null;
        try {
            if (createdFrom != null && !createdFrom.isEmpty()) {
                fromDate = sdf.parse(createdFrom);
            }
            if (createdTo != null && !createdTo.isEmpty()) {
                toDate = sdf.parse(createdTo);
            }
        } catch (ParseException e) {
            throw new SomeThingWrongException(Translator.toLocale("product.date.invalid_format"));
        }

        //  Gọi service
        Page<ProductSearchResponse> result = productService.searchProducts(
                name, product_code, fromDate, toDate, categoryId, pageable
        );

        // Tạo đối tượng phân trang
        PageResponse.Pagination pagination = new PageResponse.Pagination(
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        );

        //  Trả về response chuẩn
        PageResponse<ProductSearchResponse> response = new PageResponse<>();
        response.setData(result.getContent());
        response.setPagination(pagination);
        System.out.println("2");
        return response;
    }


    @PutMapping("/{id}")
    public ResponseEntity<ProductUpdateResponse> updateCategory(
            @PathVariable Long id,
            @Valid @ModelAttribute ProductRequest request,
            @RequestParam(required = false) List<MultipartFile> images
    ) {
        ProductUpdateResponse response = productService.updateProduct(id, request, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(Map.of("message", Translator.toLocale("product.delete.success")));
    }

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportProducts(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String productCode,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdFrom,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date createdTo,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) throws IOException {
        ByteArrayInputStream in = productService.exportProducts(name, productCode, createdFrom, createdTo,categoryId,page,size);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=categories.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}
