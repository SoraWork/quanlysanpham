package com.hoaiphong.quanlysanpham.service.impl;

import com.hoaiphong.quanlysanpham.base.CreateResponse;
import com.hoaiphong.quanlysanpham.base.PageResponse;
import com.hoaiphong.quanlysanpham.configuration.Translator;
import com.hoaiphong.quanlysanpham.dto.request.CategoryCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.CategoryRequest;
import com.hoaiphong.quanlysanpham.dto.request.ImageRequestId;
import com.hoaiphong.quanlysanpham.dto.response.CategoryCreateResponse;
import com.hoaiphong.quanlysanpham.dto.response.CategorySearchResponse;
import com.hoaiphong.quanlysanpham.dto.response.CategoryUpdateResponse;
import com.hoaiphong.quanlysanpham.dto.response.ImageResponse;
import com.hoaiphong.quanlysanpham.entity.Category;
import com.hoaiphong.quanlysanpham.entity.Image;
import com.hoaiphong.quanlysanpham.exception.SomeThingWrongException;
import com.hoaiphong.quanlysanpham.mapper.CategoryMapper;
import com.hoaiphong.quanlysanpham.mapper.ImageMapper;
import com.hoaiphong.quanlysanpham.repository.CategoryRepository;
import com.hoaiphong.quanlysanpham.repository.ImageRepository;
import com.hoaiphong.quanlysanpham.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final FileStorageServiceImpl fileStorageServiceImpl;
    private final ImageRepository imageRepository;
    private final ImageMapper imageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateResponse<CategoryCreateResponse> createCategory(CategoryCreateRequest request, List<MultipartFile> images) {
        if (categoryRepository.existsByCategoryCode(request.getCategoryCode())) {
            throw new SomeThingWrongException(Translator.toLocale("category_code.exists"));
        }
        Category category = categoryMapper.toEntity(request, images,fileStorageServiceImpl);

        categoryRepository.save(category);

        CategoryCreateResponse data = new CategoryCreateResponse(category.getId());
        return new CreateResponse<>(200, Translator.toLocale("category.create.success"), data);
    }

    @Override
    public PageResponse<CategorySearchResponse> searchCategories(
            String name,
            String categoryCode,
            Date createdFrom,
            Date createdTo,
            int page,
            int size
    ) {
        name = escapeLike(name);
        categoryCode = escapeLike(categoryCode);

        // Query Category với phân trang
        Page<Category> categoriesPage = categoryRepository.searchCategories(
                name, categoryCode, createdFrom, createdTo, PageRequest.of(page, size)
        );

        List<Category> categories = categoriesPage.getContent();
        List<Long> categoryIds = categories.stream()
                .map(Category::getId)
                .toList();

        // Query Images cho các category này
        List<Image> images = imageRepository.findByCategoryIdIn(categoryIds);

        // Map Images → categoryId → List<ImageResponse> sử dụng imageMapper
        Map<Long, List<ImageResponse>> imagesMap = images.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getCategory().getId(),
                        Collectors.mapping(imageMapper::toResponse, Collectors.toList())
                ));

        // Map Category → CategorySearchResponse sử dụng categoryMapper + gắn images
        List<CategorySearchResponse> dtoList = categories.stream()
                .map(c -> {
                    CategorySearchResponse dto = categoryMapper.toSearchResponse(c);
                    dto.setImages(imagesMap.getOrDefault(c.getId(), new ArrayList<>()));
                    return dto;
                })
                .toList();

        // Build PageResponse
        PageResponse.Pagination pagination = new PageResponse.Pagination(
                categoriesPage.getNumber(),
                categoriesPage.getSize(),
                categoriesPage.getTotalElements(),
                categoriesPage.getTotalPages(),
                categoriesPage.hasNext(),
                categoriesPage.hasPrevious()
        );


        return new PageResponse<>(dtoList, pagination);
    }

    @Transactional(rollbackFor = Exception.class)
    public CategoryUpdateResponse updateCategory(Long id, CategoryRequest request, List<MultipartFile> images) {
        // 1. Lấy category + fetch ảnh
        Category category = categoryRepository.findByIdWithActiveStatus(id)
                .orElseThrow(() -> new SomeThingWrongException(Translator.toLocale("category_code.notexists")));

        // 2. Kiểm tra trùng categoryCode
        if (categoryRepository.existsByCategoryCodeAndIdNot(request.getCategoryCode(), id)) {
            throw new SomeThingWrongException(Translator.toLocale("category_code.duplicated"));
        }

        // 3. Update thông tin Category (cha)
        updateCategoryInfo(category, request);

        // 4. Update ảnh Category (con)
        updateCategoryImages(category, request.getImagesid(), images);

        // 5. Lưu category
        categoryRepository.save(category);

        // 6. Lọc ảnh status = 1 trước khi map
        Set<Image> activeImages = new HashSet<>();
        for (Image img : category.getImages()) {
            if ("1".equals(img.getStatus())) {
                activeImages.add(img);
            }
        }
        category.setImages(activeImages);

        // 7. Mapping trả về
        return categoryMapper.toUpdateResponse(category);
    }

    @Override
    public boolean deleteCategory(Long id) {
        Category category = categoryRepository.findByIdWithActiveStatus(id)
                .orElseThrow(() -> new SomeThingWrongException(Translator.toLocale("category_code.notexists")));
        category.setStatus("0");
        category.setModifiedDate(new Date());
        categoryRepository.save(category);
        return true;
    }

    @Override
    public ByteArrayInputStream exportCategories(String name, String categoryCode, Date createdFrom, Date createdTo,  int page,
                                                 int size) throws IOException {
        List<Category> categories = categoryRepository.searchAllForExport(
                name, categoryCode, createdFrom, createdTo, PageRequest.of(page, size)
        );

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Categories");

            // Tạo style header
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            //  Header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"ID", "Tên", "Mã", "Mô tả", "Ngày tạo", "Ngày sửa", "Người tạo", "Người sửa"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            //  Dữ liệu
            int rowIdx = 1;
            CreationHelper creationHelper = workbook.getCreationHelper();
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(creationHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));

            for (Category c : categories) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(c.getId());
                row.createCell(1).setCellValue(c.getName());
                row.createCell(2).setCellValue(c.getCategoryCode());
                row.createCell(3).setCellValue(c.getDescription());

                Cell createdDateCell = row.createCell(4);
                if (c.getCreatedDate() != null) {
                    createdDateCell.setCellValue(c.getCreatedDate());
                    createdDateCell.setCellStyle(dateStyle);
                }

                Cell modifiedDateCell = row.createCell(5);
                if (c.getModifiedDate() != null) {
                    modifiedDateCell.setCellValue(c.getModifiedDate());
                    modifiedDateCell.setCellStyle(dateStyle);
                }

                row.createCell(6).setCellValue(c.getCreatedBy() != null ? c.getCreatedBy() : "");
                row.createCell(7).setCellValue(c.getModifiedBy() != null ? c.getModifiedBy() : "");
            }

            //  Tự động căn cột
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            //  Ghi vào output stream và đóng kết nối
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }


    private void updateCategoryInfo(Category category, CategoryRequest request) {
        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getCategoryCode() != null) {
            category.setCategoryCode(request.getCategoryCode());
        }
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }
    }

    private void updateCategoryImages(Category category, List<ImageRequestId> imagesToReplace, List<MultipartFile> newImages) {
        // 1. Xử lý danh sách ảnh cần ẩn (status = "0")
        if (imagesToReplace != null && !imagesToReplace.isEmpty()) {
            List<String> idsToReplace = new ArrayList<>();
            for (ImageRequestId imgReq : imagesToReplace) {
                // Nếu imgReq có id hợp lệ thì mới thêm
                if (imgReq.getId() != null && !imgReq.getId().isBlank()) {
                    idsToReplace.add(imgReq.getId());
                }
            }

            // Nếu có id thì mới xử lý
            if (!idsToReplace.isEmpty()) {
                for (Image img : category.getImages()) {
                    if (idsToReplace.contains(img.getId())) {
                        img.setStatus("0");
                        img.setModifiedDate(new Date());
                    }
                }
            }
        }

        if (newImages != null) {
            for (MultipartFile file : newImages) {
                Image newImg = new Image();
                newImg.setName(file.getOriginalFilename());
                newImg.setUrl(fileStorageServiceImpl.save(file));
                newImg.setStatus("1");
                newImg.setCreatedDate(new Date());
                newImg.setModifiedDate(new Date());
                newImg.setCategory(category);
                category.getImages().add(newImg);
            }
        }
    }




    public static String escapeLike(String param) {
        if (param == null) return null;
        return param.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("%", "\\%");
    }


}
