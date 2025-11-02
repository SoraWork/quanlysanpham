package com.hoaiphong.quanlysanpham.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductCreateRequest {
    @NotBlank(message = "{product.name.not_blank}")
    @Size(max = 200, message = "{product.name.size}")
    private String name;

    @NotBlank(message = "{product.code.not_blank}")
    @Size(max = 50, message = "{product.code.size}")
    private String productCode;

    @Size(max = 500, message = "{product.description.size}")
    private String description;

    @NotNull(message = "{product.price.not_null}")
    @PositiveOrZero(message = "{product.price.positive_or_zero}")
    private Double price;

    @NotNull(message = "{product.quantity.not_null}")
    @PositiveOrZero(message = "{product.quantity.positive_or_zero}")
    private Long quantity;

    @NotEmpty(message = "{product.category_ids.not_empty}")
    private List<Long> categoryIds;
}
