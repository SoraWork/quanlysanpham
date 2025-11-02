package com.hoaiphong.quanlysanpham.repository;


import com.hoaiphong.quanlysanpham.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    boolean existsByProductCode(String productCode);
    boolean existsByProductCodeAndIdNot(String productCode, Long id);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.productCategories pc
    WHERE p.status = '1'
    AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\')
    AND (:productCode IS NULL OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :productCode, '%')) ESCAPE '\\')
    AND (:createdFrom IS NULL OR p.createdDate >= :createdFrom)
    AND (:createdTo IS NULL OR p.createdDate <= :createdTo)
    AND (:categoryId IS NULL OR pc.category.id = :categoryId)
    """)
    Page<Product> searchProducts(
            @Param("name") String name,
            @Param("productCode") String productCode,
            @Param("createdFrom") Date createdFrom,
            @Param("createdTo") Date createdTo,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN FETCH p.productCategories pc
    LEFT JOIN FETCH pc.category
    LEFT JOIN FETCH p.images
    WHERE p.id = :id
      AND p.status = '1'
""")
    Optional<Product> findByIdWithActiveStatus(@Param("id") Long id);

    @Query("""
    SELECT DISTINCT p FROM Product p
    LEFT JOIN p.productCategories pc
    WHERE p.status = '1'
    AND (:name IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%')) ESCAPE '\\')
    AND (:productCode IS NULL OR LOWER(p.productCode) LIKE LOWER(CONCAT('%', :productCode, '%')) ESCAPE '\\')
    AND (:createdFrom IS NULL OR p.createdDate >= :createdFrom)
    AND (:createdTo IS NULL OR p.createdDate <= :createdTo)
    AND (:categoryId IS NULL OR pc.category.id = :categoryId)
    """)
    List<Product> searchAllForExport(
            @Param("name") String name,
            @Param("productCode") String productCode,
            @Param("createdFrom") Date createdFrom,
            @Param("createdTo") Date createdTo,
            @Param("categoryId") Long categoryId,
            Pageable pageable
    );
}
