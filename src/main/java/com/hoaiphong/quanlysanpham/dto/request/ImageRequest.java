package com.hoaiphong.quanlysanpham.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequest {
    private String name;
    private String url;
    private String status;
}