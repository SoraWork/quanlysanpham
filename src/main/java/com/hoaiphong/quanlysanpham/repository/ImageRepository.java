package com.hoaiphong.quanlysanpham.repository;

import com.hoaiphong.quanlysanpham.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, String> {

    @Query("SELECT i FROM Image i WHERE i.category.id IN :categoryIds AND i.status = '1'")
    List<Image> findByCategoryIdIn(List<Long> categoryIds);

    @Query("SELECT i FROM Image i WHERE i.product.id IN :productIds AND i.status = '1'")
    List<Image> findByProductIdIn(List<Long> productIds);

}
