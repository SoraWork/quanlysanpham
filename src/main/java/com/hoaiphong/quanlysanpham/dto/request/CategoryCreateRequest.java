package com.hoaiphong.quanlysanpham.dto.request;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategoryCreateRequest {
    @NotBlank(message = "{category.name.notBlank}")
    @Size(max = 100, message = "{category.name.maxLength}")
    private String name;

    @NotBlank(message = "{category.code.notBlank}")
    @Size(max = 50, message = "{category.code.maxLength}")
    private String categoryCode;

    @Size(max = 200, message = "{category.description.maxLength}")
    private String description;

}