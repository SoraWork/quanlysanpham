package com.hoaiphong.quanlysanpham.service.impl;

import com.hoaiphong.quanlysanpham.base.CreateResponse;
import com.hoaiphong.quanlysanpham.configuration.Translator;
import com.hoaiphong.quanlysanpham.dto.request.ImageRequestId;
import com.hoaiphong.quanlysanpham.dto.request.ProductCreateRequest;
import com.hoaiphong.quanlysanpham.dto.request.ProductRequest;
import com.hoaiphong.quanlysanpham.dto.response.ImageResponse;
import com.hoaiphong.quanlysanpham.dto.response.ProductCreateResponse;
import com.hoaiphong.quanlysanpham.dto.response.ProductSearchResponse;
import com.hoaiphong.quanlysanpham.dto.response.ProductUpdateResponse;
import com.hoaiphong.quanlysanpham.entity.*;
import com.hoaiphong.quanlysanpham.exception.SomeThingWrongException;
import com.hoaiphong.quanlysanpham.mapper.ImageMapper;
import com.hoaiphong.quanlysanpham.mapper.ProductMapper;
import com.hoaiphong.quanlysanpham.repository.CategoryRepository;
import com.hoaiphong.quanlysanpham.repository.ImageRepository;
import com.hoaiphong.quanlysanpham.repository.ProductCategoryRepository;
import com.hoaiphong.quanlysanpham.repository.ProductRepository;
import com.hoaiphong.quanlysanpham.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.hoaiphong.quanlysanpham.service.impl.CategoryServiceImpl.escapeLike;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ImageRepository imageRepository;
    private final ProductCategoryRepository  productCategoryRepository;

    private final FileStorageServiceImpl fileStorageServiceImpl;
    private final ProductMapper productMapper;
    private final ImageMapper imageMapper;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreateResponse<ProductCreateResponse> createProduct(ProductCreateRequest request, List<MultipartFile> images) {
        if(productRepository.existsByProductCode(request.getProductCode())){
            throw new SomeThingWrongException(Translator.toLocale("product_code.exists"));
        }
        Product product = productMapper.toEntity(request, images, fileStorageServiceImpl);

        productRepository.save(product);

        List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());

        Set<ProductCategory> productCategories = new HashSet<>();

        for (Category category : categories) {
            ProductCategory pc = new ProductCategory();
            pc.setId(new ProductCategoryId(product.getId(), category.getId()));
            pc.setProduct(product);
            pc.setCategory(category);
            pc.setCreatedDate(new Date());
            pc.setModifiedDate(new Date());
            pc.setCreatedBy(null);
            pc.setModifiedBy(null);
            pc.setStatus("1");

            productCategories.add(pc);
        }

        product.setProductCategories(productCategories);
        productRepository.save(product);

        ProductCreateResponse data = new ProductCreateResponse(product.getId());
        return new CreateResponse<>(200, Translator.toLocale("product.create.success"), data);
    }


    public Page<ProductSearchResponse> searchProducts(
            String name,
            String productCode,
            Date createdFrom,
            Date createdTo,
            Long categoryId,
            Pageable pageable
    ) {
        name = escapeLike(name);
        productCode = escapeLike(productCode);

        //  Lấy page sản phẩm (repository đã lọc categoryId nếu truyền)
        Page<Product> productsPage = productRepository.searchProducts(
                name, productCode, createdFrom, createdTo, categoryId, pageable
        );
        if (productsPage.isEmpty()) {
            return Page.empty(pageable);
        }
        // Lấy danh sách productId trong trang hiện tại
        List<Long> productIds = productsPage.getContent().stream()
                .map(Product::getId)
                .collect(Collectors.toList());
        //  Lấy tất cả ảnh cho các product trong trang (dùng ImageRepository)
        List<Image> images = imageRepository.findByProductIdIn(productIds);
        Map<Long, List<ImageResponse>> imageMap = images.stream()
                .collect(Collectors.groupingBy(
                        img -> img.getProduct().getId(),
                        Collectors.mapping(imageMapper::toResponse, Collectors.toList())
                ));
        //  Lấy tất cả ProductCategory cho productIds (dùng ProductCategoryRepository)
        List<ProductCategory> pcs = productCategoryRepository.findByProductIdIn(productIds);

        //  Gom tên category theo productId
        Map<Long, List<String>> categoryNamesMap = pcs.stream()
                .filter(pc -> pc.getCategory() != null)
                .collect(Collectors.groupingBy(
                        pc -> pc.getProduct().getId(),
                        Collectors.mapping(pc -> pc.getCategory().getName(), Collectors.toList())
                ));

        //  Map từng Product -> ProductSearchResponse (dùng dữ liệu đã lấy)
        List<ProductSearchResponse> dtoList = productsPage.getContent().stream().map(p -> {
            ProductSearchResponse dto = new ProductSearchResponse();
            dto.setId(p.getId());
            dto.setName(p.getName());
            dto.setProductCode(p.getProductCode());
            dto.setPrice(p.getPrice());
            dto.setQuantity(p.getQuantity());
            dto.setCreatedDate(p.getCreatedDate());
            dto.setModifiedDate(p.getModifiedDate());

            // images: từ imageMap
            dto.setImages(imageMap.getOrDefault(p.getId(), Collections.emptyList()));

            // categories: join bằng ", "
            List<String> cnames = categoryNamesMap.getOrDefault(p.getId(), Collections.emptyList());
            String categoriesJoined = cnames.isEmpty() ? "" : String.join(", ", cnames);
            dto.setCategories(categoriesJoined);

            return dto;
        }).collect(Collectors.toList());

        //  Trả Page<ProductSearchResponse> giữ nguyên thông tin phân trang
        return new PageImpl<>(dtoList, pageable, productsPage.getTotalElements());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ProductUpdateResponse updateProduct(Long id, ProductRequest request, List<MultipartFile> images) {
        // 1. Lấy product + fetch ảnh + category
        Product product = productRepository.findByIdWithActiveStatus(id)
                .orElseThrow(() -> new SomeThingWrongException(Translator.toLocale("product_code.notexists")));

        // 2. Kiểm tra trùng categoryCode
        if (productRepository.existsByProductCodeAndIdNot(request.getProductCode(), id)) {
            throw new SomeThingWrongException(Translator.toLocale("product_code.duplicated"));
        }

        updateProductInfo(product, request);

        updateProductImages(product, request.getImagesid(), images);

        updateProductCategories(product, request.getCategoryIds());

        productRepository.save(product);

        // Lọc ảnh active (status = "1")
        Set<Image> activeImages = new HashSet<>();
        for (Image img : product.getImages()) {
            if ("1".equals(img.getStatus())) {
                activeImages.add(img);
            }
        }
        product.setImages(activeImages);

        // Lọc danh mục active (status = "1")
        Set<ProductCategory> activeProductCategories = new HashSet<>();
        for (ProductCategory pc : product.getProductCategories()) {
            if ("1".equals(pc.getStatus())) {
                activeProductCategories.add(pc);
            }
        }
        product.setProductCategories(activeProductCategories);

        //  Mapping trả về
        return productMapper.toUpdateResponse(product);
    }

    @Override
    public boolean deleteProduct(Long id) {
        Product product = productRepository.findByIdWithActiveStatus(id)
                .orElseThrow(() -> new SomeThingWrongException(Translator.toLocale("product_code.notexists")));
        product.setStatus("0");
        product.setModifiedDate(new Date());
        productRepository.save(product);
        return true;
    }

    @Override
    public ByteArrayInputStream exportProducts(String name, String productCode, Date createdFrom, Date createdTo, Long categoryId, int page, int size) throws IOException {
        name = escapeLike(name);
        productCode = escapeLike(productCode);

        // Lấy danh sách sản phẩm
        List<Product> products = productRepository.searchAllForExport(
                name, productCode, createdFrom, createdTo, categoryId, PageRequest.of(page, size)
        );

        if (products.isEmpty()) {
            return new ByteArrayInputStream(new byte[0]);
        }

        // Lấy danh sách productIds
        List<Long> productIds = products.stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        // Lấy tất cả ProductCategory tương ứng
        List<ProductCategory> pcs = productCategoryRepository.findByProductIdIn(productIds);

        // Gom tên danh mục theo productId
        Map<Long, List<String>> categoryNamesMap = pcs.stream()
                .filter(pc -> pc.getCategory() != null)
                .collect(Collectors.groupingBy(
                        pc -> pc.getProduct().getId(),
                        Collectors.mapping(pc -> pc.getCategory().getName(), Collectors.toList())
                ));

        //  Tạo workbook Excel
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Products");

            // Header
            String[] columns = {"ID", "Tên", "Mã", "Giá", "Số lượng", "Ngày tạo", "Ngày sửa", "Danh mục"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            // Dữ liệu
            int rowIdx = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getId());
                row.createCell(1).setCellValue(p.getName());
                row.createCell(2).setCellValue(p.getProductCode());
                row.createCell(3).setCellValue(p.getPrice() != null ? p.getPrice().toString() : "");
                row.createCell(4).setCellValue(p.getQuantity() != null ? p.getQuantity() : 0);
                row.createCell(5).setCellValue(p.getCreatedDate() != null ? p.getCreatedDate().toString() : "");
                row.createCell(6).setCellValue(p.getModifiedDate() != null ? p.getModifiedDate().toString() : "");

                // Danh mục: join theo productId
                String categories = String.join(", ",
                        categoryNamesMap.getOrDefault(p.getId(), Collections.emptyList()));
                row.createCell(7).setCellValue(categories);
            }

            // Auto size
            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());

        }
    }

    private void updateProductInfo(Product product,ProductRequest request){
        if (request.getName() != null){
            product.setName(request.getName());
        }
        if (request.getProductCode() != null){
            product.setProductCode(request.getProductCode());
        }
        if (request.getPrice() != null){
            product.setPrice(request.getPrice());
        }
        if (request.getQuantity() != null){
            product.setQuantity(request.getQuantity());
        }
        if(request.getDescription() != null){
            product.setDescription(request.getDescription());
        }
    }

    private void updateProductImages(Product product, List<ImageRequestId> imagesToReplace, List<MultipartFile> newImages){
        // 1. Xử lý danh sách ảnh cần ẩn (status = "0")
        if(imagesToReplace != null && !imagesToReplace.isEmpty()) {
            List<String> idsToReplace = new ArrayList<>();
            for (ImageRequestId imgReq : imagesToReplace) {
                // Nếu imgReq có id hợp lệ thì mới thêm
                if (imgReq.getId() != null && !imgReq.getId().isBlank()) {
                    idsToReplace.add(imgReq.getId());
                }
            }
            // Nếu có id thì mới xử lý
            if (!idsToReplace.isEmpty()) {
                for (Image img : product.getImages()) {
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
                newImg.setProduct(product);
                product.getImages().add(newImg);
            }
        }
    }


    private void updateProductCategories(Product product, List<Long> newCategoryIds) {
        if (newCategoryIds == null) {
            // nếu client không gửi categoryIds thì không thay đổi liên kết
            return;
        }

        Date now = new Date();

        // Lấy current relations (kể cả status = 0)
        Set<ProductCategory> existingRelations = productCategoryRepository.findByProduct(product);

        // 1) Tạo danh sách id hiện có để kiểm tra nhanh (không dùng stream)
        List<Long> existingIds = new ArrayList<>();
        if (existingRelations != null) {
            for (ProductCategory pc : existingRelations) {
                if (pc != null && pc.getCategory() != null && pc.getCategory().getId() != null) {
                    existingIds.add(pc.getCategory().getId());
                }
            }
        }

        // 2) Những liên kết cũ không còn tồn tại trong danh sách mới => set status = "0"
        if (existingRelations != null) {
            for (ProductCategory pc : existingRelations) {
                Long catId = pc.getCategory() != null ? pc.getCategory().getId() : null;
                if (catId != null) {
                    boolean inNew = false;
                    for (Long id : newCategoryIds) {
                        if (id != null && id.equals(catId)) {
                            inNew = true;
                            break;
                        }
                    }
                    if (!inNew && "1".equals(pc.getStatus())) {
                        pc.setStatus("0");
                        pc.setModifiedDate(now);
                        productCategoryRepository.save(pc);
                    }
                }
            }
        }

        // 3) Những liên kết đã bị xóa mềm trước đó và giờ được chọn lại => set status = "1"
        if (existingRelations != null) {
            for (ProductCategory pc : existingRelations) {
                Long catId = pc.getCategory() != null ? pc.getCategory().getId() : null;
                if (catId != null) {
                    boolean inNew = false;
                    for (Long id : newCategoryIds) {
                        if (id != null && id.equals(catId)) {
                            inNew = true;
                            break;
                        }
                    }
                    if (inNew && "0".equals(pc.getStatus())) {
                        pc.setStatus("1");
                        pc.setModifiedDate(now);
                        productCategoryRepository.save(pc);
                    }
                }
            }
        }

        // 4) Những liên kết mới hoàn toàn => thêm mới
        for (Long id : newCategoryIds) {
            if (id == null) continue;
            boolean exists = false;
            for (Long exId : existingIds) {
                if (exId != null && exId.equals(id)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                // kiểm tra category tồn tại và active (nếu muốn)
                Category category = categoryRepository.findById(id)
                        .orElseThrow(() -> new SomeThingWrongException(Translator.toLocale("category_code.notexists" + id)));

                ProductCategory newPc = new ProductCategory();
                // set embedded id (nếu bạn dùng ProductCategoryId)
                ProductCategoryId pcId = new ProductCategoryId(product.getId(), category.getId());
                newPc.setId(pcId);

                newPc.setProduct(product);
                newPc.setCategory(category);
                newPc.setStatus("1");
                newPc.setCreatedDate(now);
                newPc.setModifiedDate(now);

                productCategoryRepository.save(newPc);
            }
        }

        // 5) Cập nhật lại product.productCategories trong persistence context (nếu cần)
        Set<ProductCategory> refreshed = productCategoryRepository.findByProduct(product);
        product.setProductCategories(refreshed);
    }


}
