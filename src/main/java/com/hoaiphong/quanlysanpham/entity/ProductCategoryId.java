package com.hoaiphong.quanlysanpham.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductCategoryId implements Serializable {

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "category_id")
    private Long categoryId;
}