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
public class CategorySearchResponse {
    private Long id;
    private String name;
    private String categoryCode;
    private String description;
    private List<ImageResponse> images;
    private Date createdDate;
    private String createdBy;
}
