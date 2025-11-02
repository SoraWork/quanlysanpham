package com.hoaiphong.quanlysanpham.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSearchResponse {
    private Long id;
    private String name;
    private String productCode;
    private Double price;
    private Long quantity;
    private Date createdDate;
    private Date modifiedDate;
    private String categories; // "Điện máy, Đồ gia dụng"
    private List<ImageResponse> images;
}
