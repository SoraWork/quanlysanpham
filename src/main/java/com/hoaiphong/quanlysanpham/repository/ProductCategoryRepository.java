package com.hoaiphong.quanlysanpham.repository;

import com.hoaiphong.quanlysanpham.entity.Product;
import com.hoaiphong.quanlysanpham.entity.ProductCategory;
import com.hoaiphong.quanlysanpham.entity.ProductCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {

    // Lấy danh sách productId theo categoryId (nếu muốn dùng)
    @Query("""
        SELECT pc.product.id 
        FROM ProductCategory pc 
        WHERE (:categoryId IS NULL OR pc.category.id = :categoryId)
        AND pc.product.status = '1'
    """)
    List<Long> findProductIdsByCategory(@Param("categoryId") Long categoryId);

    // Lấy tất cả ProductCategory theo danh sách productIds (dùng để gom tên category)
    @Query("""
        SELECT pc FROM ProductCategory pc
        WHERE pc.product.id IN :productIds
        AND pc.status = '1'
    """)
    List<ProductCategory> findByProductIdIn(@Param("productIds") List<Long> productIds);


    @Query("""
        select pc from ProductCategory pc
        join fetch pc.category c
        where pc.product = :product
    """)
    Set<ProductCategory> findByProduct(Product product);
}