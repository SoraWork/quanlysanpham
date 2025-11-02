package com.hoaiphong.quanlysanpham.mapper;

import com.hoaiphong.quanlysanpham.dto.request.ImageRequest;
import com.hoaiphong.quanlysanpham.dto.response.ImageResponse;
import com.hoaiphong.quanlysanpham.entity.Image;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring")
public interface ImageMapper {


    Image toEntity(ImageRequest request);
    List<Image> toEntityList(List<ImageRequest> requests);

    @Mapping(source = "id", target = "uuid")
    ImageResponse toResponse(Image entity);
    List<ImageResponse> toResponseList(List<Image> entities);
}